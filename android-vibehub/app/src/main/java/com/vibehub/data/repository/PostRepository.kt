package com.vibehub.data.repository

import com.vibehub.data.local.dao.PostDao
import com.vibehub.data.local.dao.UserDao
import com.vibehub.data.remote.RemotePost
import com.vibehub.domain.model.Post
import com.vibehub.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles creating, editing, archiving, and deleting posts.
 * All write operations go through Supabase Postgres; reads are cached in Room.
 *
 * Video thumbnail generation is handled by the `thumbnail` Edge Function (called
 * after upload completes) — we only store the resulting URL here.
 */
@Singleton
class PostRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val postDao: PostDao,
    private val userDao: UserDao,
) {
    // ── Create ────────────────────────────────────────────────────────────────

    suspend fun createPost(
        caption: String,
        mediaUrls: List<String>,
        mediaType: String,
        thumbnailUrl: String? = null,
        hashtags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        location: String = "",
        musicTrackJson: String? = null,
        aspectRatio: Float = 1f,
        visibility: String = "public",
        commentPerm: String = "everyone",
    ): Result<Post> = runCatching {
        val inserted = supabase.postgrest["posts"].insert(
            buildJsonObject {
                put("caption",      caption.trim())
                put("media_type",   mediaType)
                put("aspect_ratio", aspectRatio.toDouble())
                put("location",     location)
                put("visibility",   visibility)
                put("comment_perm", commentPerm)
                thumbnailUrl?.let { put("thumbnail_url", it) }
                musicTrackJson?.let { put("music_track", it) }
                putJsonArray("media_urls")  { mediaUrls.forEach { add(it) } }
                putJsonArray("hashtags")    { hashtags.map { it.removePrefix("#").lowercase() }.forEach { add(it) } }
                putJsonArray("mentions")    { mentions.map { it.removePrefix("@").lowercase() }.forEach { add(it) } }
            }
        ).decodeSingle<RemotePost>()

        val author = userDao.getUser(inserted.authorId)?.toDomain() ?: User()
        val domain = inserted.toDomain(author)
        postDao.upsertPosts(listOf(inserted.toEntity()))

        // Trigger mention notifications via DB function (no edge function needed)
        if (mentions.isNotEmpty()) {
            triggerMentionNotifications(inserted.id, mentions)
        }

        domain
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun getPost(postId: String): Result<Post> = runCatching {
        val remote = supabase.postgrest["posts"]
            .select { filter { eq("id", postId) } }
            .decodeSingle<RemotePost>()
        val author = userDao.getUser(remote.authorId)?.toDomain() ?: User()
        remote.toDomain(author)
    }

    // ── Edit / Update ─────────────────────────────────────────────────────────

    suspend fun editPost(
        postId: String,
        caption: String,
        hashtags: List<String>,
    ): Result<Unit> = runCatching {
        supabase.postgrest["posts"].update(
            buildJsonObject {
                put("caption", caption.trim())
                putJsonArray("hashtags") { hashtags.map { it.removePrefix("#").lowercase() }.forEach { add(it) } }
            }
        ) { filter { eq("id", postId) } }

        postDao.getPost(postId)?.copy(caption = caption, hashtags = hashtags.joinToString(","))?.let {
            postDao.upsertPosts(listOf(it))
        }
    }

    suspend fun updateThumbnail(postId: String, thumbnailUrl: String): Result<Unit> = runCatching {
        supabase.postgrest["posts"].update(
            buildJsonObject { put("thumbnail_url", thumbnailUrl) }
        ) { filter { eq("id", postId) } }
    }

    // ── Archive / Unarchive ───────────────────────────────────────────────────

    suspend fun archivePost(postId: String): Result<Unit> = runCatching {
        supabase.postgrest["posts"].update(
            buildJsonObject { put("is_archived", true) }
        ) { filter { eq("id", postId) } }
        postDao.getPost(postId)?.let { postDao.upsertPosts(listOf(it)) }
    }

    suspend fun unarchivePost(postId: String): Result<Unit> = runCatching {
        supabase.postgrest["posts"].update(
            buildJsonObject { put("is_archived", false) }
        ) { filter { eq("id", postId) } }
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        supabase.postgrest["posts"].update(
            buildJsonObject { put("is_deleted", true) }
        ) { filter { eq("id", postId) } }
        postDao.deletePost(postId)
    }

    // ── Pin / Unpin ───────────────────────────────────────────────────────────

    suspend fun pinPost(postId: String, pin: Boolean): Result<Unit> = runCatching {
        supabase.postgrest["posts"].update(
            buildJsonObject { put("is_pinned", pin) }
        ) { filter { eq("id", postId) } }
    }

    // ── Like / Save ───────────────────────────────────────────────────────────

    suspend fun toggleLike(postId: String, isLiked: Boolean): Result<Unit> = runCatching {
        if (isLiked) {
            supabase.postgrest["post_likes"].delete { filter { eq("post_id", postId) } }
        } else {
            supabase.postgrest["post_likes"].insert(mapOf("post_id" to postId))
        }
    }

    suspend fun toggleSave(postId: String, isSaved: Boolean): Result<Unit> = runCatching {
        if (isSaved) {
            supabase.postgrest["post_saves"].delete { filter { eq("post_id", postId) } }
        } else {
            supabase.postgrest["post_saves"].insert(mapOf("post_id" to postId))
        }
    }

    // ── View tracking ─────────────────────────────────────────────────────────

    suspend fun trackView(postId: String): Result<Unit> = runCatching {
        supabase.postgrest["post_views"].insert(mapOf("post_id" to postId))
        supabase.postgrest["posts"].update(
            buildJsonObject { put("views_count", supabase.postgrest.rpc("increment_views", mapOf("p_post_id" to postId))) }
        ) { filter { eq("id", postId) } }
    }

    // ── Mention notifications ─────────────────────────────────────────────────

    private suspend fun triggerMentionNotifications(postId: String, mentions: List<String>) {
        runCatching {
            // Each mentioned username → look up user → insert notification
            mentions.forEach { username ->
                val rows = supabase.postgrest["profiles"]
                    .select { filter { eq("username", username) } }
                    .decodeList<Map<String, String>>()
                rows.firstOrNull()?.get("id")?.let { mentionedUserId ->
                    supabase.postgrest["notifications"].insert(
                        buildJsonObject {
                            put("user_id",  mentionedUserId)
                            put("post_id",  postId)
                            put("type",     "mention")
                            put("text",     "mentioned you in a post")
                        }
                    )
                }
            }
        }
    }
}
