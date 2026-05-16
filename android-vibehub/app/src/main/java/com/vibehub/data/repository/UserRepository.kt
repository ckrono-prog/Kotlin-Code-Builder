package com.vibehub.data.repository

import com.vibehub.data.local.dao.HighlightDao
import com.vibehub.data.local.dao.PostDao
import com.vibehub.data.local.dao.UserDao
import com.vibehub.data.remote.RemotePost
import com.vibehub.data.remote.RemoteUser
import com.vibehub.domain.model.Highlight
import com.vibehub.domain.model.Post
import com.vibehub.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val highlightDao: HighlightDao,
) {
    // ── Observe (Room, offline-first) ─────────────────────────────────────────

    fun observeUser(userId: String): Flow<User?> =
        userDao.observeUser(userId).map { it?.toDomain() }

    fun observeUserPosts(userId: String): Flow<List<Post>> =
        postDao.observeUserPosts(userId).map { entities ->
            entities.map { e ->
                val author = userDao.getUser(e.authorId)?.toDomain()
                e.toDomain(author ?: User())
            }
        }

    fun observeHighlights(userId: String): Flow<List<Highlight>> =
        highlightDao.observeHighlights(userId).map { entities ->
            entities.map { e -> Highlight(id = e.id, userId = e.userId, title = e.title, coverUrl = e.coverUrl, createdAt = e.createdAt) }
        }

    // ── Remote refresh ────────────────────────────────────────────────────────

    suspend fun refreshUser(userId: String): Result<Unit> = runCatching {
        val remote = supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<RemoteUser>()
        userDao.upsertUser(remote.toEntity())
    }

    suspend fun refreshUserPosts(userId: String): Result<Unit> = runCatching {
        val remote = supabase.postgrest["posts"]
            .select { filter { eq("author_id", userId) } ; order("created_at", Order.DESCENDING) }
            .decodeList<RemotePost>()
        postDao.upsertPosts(remote.map { it.toEntity() })
    }

    suspend fun getCurrentUser(): Result<User> = runCatching {
        supabase.postgrest["profiles"]
            .select()
            .decodeSingle<RemoteUser>()
            .toDomain()
    }

    suspend fun hasProfile(userId: String): Boolean = runCatching {
        val rows = supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeList<RemoteUser>()
        rows.isNotEmpty()
    }.getOrElse { false }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchUsers(query: String): List<User> =
        if (query.length >= 2) userDao.searchUsers(query).map { it.toDomain() } else emptyList()

    // ── Follow / Unfollow / Toggle ────────────────────────────────────────────

    suspend fun followUser(targetUserId: String): Result<Unit> = runCatching {
        supabase.postgrest["follows"].insert(mapOf("following_id" to targetUserId))
        userDao.getUser(targetUserId)?.copy(
            isFollowedByMe = true,
            followersCount = (userDao.getUser(targetUserId)?.followersCount ?: 0) + 1,
        )?.let { userDao.upsertUser(it) }
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> = runCatching {
        supabase.postgrest["follows"].delete {
            filter { eq("following_id", targetUserId) }
        }
        userDao.getUser(targetUserId)?.copy(
            isFollowedByMe = false,
            followersCount = maxOf(0, (userDao.getUser(targetUserId)?.followersCount ?: 1) - 1),
        )?.let { userDao.upsertUser(it) }
    }

    /** Toggle follow — used by CommentsViewModel, FeedViewModel. */
    suspend fun toggleFollow(targetUserId: String): Result<Unit> {
        val isFollowing = userDao.getUser(targetUserId)?.isFollowedByMe ?: false
        return if (isFollowing) unfollowUser(targetUserId) else followUser(targetUserId)
    }

    // ── Block / Unblock ───────────────────────────────────────────────────────

    suspend fun blockUser(targetUserId: String): Result<Unit> = runCatching {
        // Insert block
        supabase.postgrest["blocks"].insert(mapOf("blocked_id" to targetUserId))
        // Optimistically unfollow both directions locally
        unfollowUser(targetUserId)
        // Update local entity
        userDao.getUser(targetUserId)?.copy(isBlockedByMe = true)?.let { userDao.upsertUser(it) }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> = runCatching {
        supabase.postgrest["blocks"].delete { filter { eq("blocked_id", targetUserId) } }
        userDao.getUser(targetUserId)?.copy(isBlockedByMe = false)?.let { userDao.upsertUser(it) }
    }

    suspend fun getBlockedUsers(): List<User> = runCatching {
        supabase.postgrest["blocks"]
            .select { filter { } }
            .decodeList<Map<String, String>>()
            .mapNotNull { row ->
                val id = row["blocked_id"] ?: return@mapNotNull null
                userDao.getUser(id)?.toDomain()
            }
    }.getOrElse { emptyList() }

    // ── Contact suggestions ───────────────────────────────────────────────────

    /**
     * Upload hashed contacts and return matching VibeHub users.
     * This calls the contact-suggestions Edge Function — raw phones never leave device.
     */
    suspend fun findUsersFromContacts(phoneHashes: List<String>): List<User> = runCatching {
        // Edge function call via Supabase Functions SDK
        val response = supabase.postgrest.rpc(
            function = "find_users_from_hashes",
            parameters = mapOf("hashes" to phoneHashes),
        ).decodeList<RemoteUser>()
        response.map { it.toDomain() }
    }.getOrElse { emptyList() }

    /** Store hashed phone numbers for reverse-lookup by others. */
    suspend fun uploadContactHashes(hashes: List<String>): Result<Unit> = runCatching {
        val rows = hashes.map { mapOf("phone_hash" to it) }
        supabase.postgrest["contact_hashes"].upsert(rows)
    }

    // ── Location-based suggestions ────────────────────────────────────────────

    suspend fun getNearbyUsers(lat: Double, lng: Double, radiusKm: Double = 50.0): List<User> =
        runCatching {
            supabase.postgrest.rpc(
                "users_near_location",
                mapOf("p_lat" to lat, "p_lng" to lng, "p_radius_km" to radiusKm, "p_limit" to 20),
            ).decodeList<RemoteUser>().map { it.toDomain() }
        }.getOrElse { emptyList() }

    // ── Privacy ───────────────────────────────────────────────────────────────

    suspend fun updateAccountPrivacy(isPrivate: Boolean): Result<Unit> = runCatching {
        supabase.postgrest["profiles"].update(mapOf("is_private" to isPrivate))
    }

    suspend fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        website: String? = null,
        location: String? = null,
        avatarUrl: String? = null,
    ): Result<Unit> = runCatching {
        val updates = buildMap {
            displayName?.let { put("display_name", it) }
            bio?.let         { put("bio", it) }
            website?.let     { put("website", it) }
            location?.let    { put("location", it) }
            avatarUrl?.let   { put("avatar_url", it) }
        }
        if (updates.isNotEmpty()) supabase.postgrest["profiles"].update(updates)
    }
}
