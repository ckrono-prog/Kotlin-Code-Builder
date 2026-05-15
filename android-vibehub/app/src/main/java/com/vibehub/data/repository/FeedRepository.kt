package com.vibehub.data.repository

import com.vibehub.data.local.dao.PostDao
import com.vibehub.data.local.dao.StoryDao
import com.vibehub.data.local.dao.UserDao
import com.vibehub.data.remote.RemotePost
import com.vibehub.data.remote.RemoteStory
import com.vibehub.domain.model.Post
import com.vibehub.domain.model.Story
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val userDao: UserDao,
) {
    // ── Observable feed from local DB (offline-first) ─────────────────────────
    fun observeFeed(): Flow<List<Post>> =
        postDao.observeFeed().map { entities ->
            entities.map { entity ->
                val author = userDao.getUser(entity.authorId)?.toDomain()
                entity.toDomain(author ?: com.vibehub.domain.model.User())
            }
        }

    fun observeReels(): Flow<List<Post>> =
        postDao.observeReels().map { entities ->
            entities.map { entity ->
                val author = userDao.getUser(entity.authorId)?.toDomain()
                entity.toDomain(author ?: com.vibehub.domain.model.User())
            }
        }

    fun observeStories(): Flow<List<Story>> =
        storyDao.observeStories().map { entities ->
            entities.map { entity ->
                val author = userDao.getUser(entity.authorId)?.toDomain()
                entity.toDomain(author ?: com.vibehub.domain.model.User())
            }
        }

    // ── Remote refresh ─────────────────────────────────────────────────────────
    suspend fun refreshFeed(page: Int = 0, limit: Int = 20): Result<Unit> = runCatching {
        val remote = supabase.postgrest["posts"]
            .select {
                order("created_at", Order.DESCENDING)
                range(from = (page * limit).toLong(), to = ((page + 1) * limit - 1).toLong())
            }
            .decodeList<RemotePost>()

        // Cache authors
        remote.map { it.authorId }.distinct().forEach { authorId ->
            refreshUser(authorId)
        }

        // Cache posts
        postDao.upsertPosts(remote.map { it.toEntity() })
    }

    suspend fun refreshStories(): Result<Unit> = runCatching {
        val remote = supabase.postgrest["stories"]
            .select {
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<RemoteStory>()

        remote.map { it.authorId }.distinct().forEach { authorId ->
            refreshUser(authorId)
        }

        storyDao.upsertStories(remote.map { it.toEntity() })
    }

    // ── Likes / Saves ──────────────────────────────────────────────────────────
    suspend fun toggleLike(postId: String, isCurrentlyLiked: Boolean): Result<Unit> = runCatching {
        val delta = if (isCurrentlyLiked) -1 else 1
        postDao.updateLike(postId, !isCurrentlyLiked, delta)

        if (isCurrentlyLiked) {
            supabase.postgrest["likes"].delete { filter { eq("post_id", postId) } }
        } else {
            supabase.postgrest["likes"].insert(mapOf("post_id" to postId))
        }
    }

    suspend fun toggleSave(postId: String, isCurrentlySaved: Boolean): Result<Unit> = runCatching {
        postDao.updateSave(postId, !isCurrentlySaved)

        if (isCurrentlySaved) {
            supabase.postgrest["saves"].delete { filter { eq("post_id", postId) } }
        } else {
            supabase.postgrest["saves"].insert(mapOf("post_id" to postId))
        }
    }

    // ── Story views ────────────────────────────────────────────────────────────
    suspend fun markStoryViewed(storyId: String): Result<Unit> = runCatching {
        storyDao.markViewed(storyId)
        supabase.postgrest["story_views"].insert(mapOf("story_id" to storyId))
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private suspend fun refreshUser(userId: String) {
        runCatching {
            val remote = supabase.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<com.vibehub.data.remote.RemoteUser>()
            userDao.upsertUser(remote.toEntity())
        }
    }
}
