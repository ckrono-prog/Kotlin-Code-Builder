package com.vibehub.data.repository

import com.vibehub.data.local.dao.HighlightDao
import com.vibehub.data.local.dao.PostDao
import com.vibehub.data.local.dao.UserDao
import com.vibehub.data.local.entities.HighlightEntity
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
            entities.map { e ->
                Highlight(
                    id = e.id, userId = e.userId,
                    title = e.title, coverUrl = e.coverUrl,
                    createdAt = e.createdAt,
                )
            }
        }

    suspend fun refreshUser(userId: String): Result<Unit> = runCatching {
        val remote = supabase.postgrest["users"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<RemoteUser>()
        userDao.upsertUser(remote.toEntity())
    }

    suspend fun refreshUserPosts(userId: String): Result<Unit> = runCatching {
        val remote = supabase.postgrest["posts"]
            .select {
                filter { eq("author_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<RemotePost>()
        postDao.upsertPosts(remote.map { it.toEntity() })
    }

    suspend fun searchUsers(query: String): List<User> {
        return if (query.length >= 2) {
            userDao.searchUsers(query).map { it.toDomain() }
        } else emptyList()
    }

    suspend fun followUser(targetUserId: String): Result<Unit> = runCatching {
        supabase.postgrest["follows"].insert(
            mapOf("following_id" to targetUserId)
        )
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

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        location: String,
        website: String,
    ): Result<Unit> = runCatching {
        supabase.postgrest["users"].update(
            mapOf(
                "display_name" to displayName,
                "bio"          to bio,
                "location"     to location,
                "website"      to website,
            )
        )
    }

    suspend fun getCurrentUser(): Result<User> = runCatching {
        val remote = supabase.postgrest["users"]
            .select { limit(1) }
            .decodeSingle<RemoteUser>()
        remote.toDomain()
    }

    suspend fun getUserById(userId: String): Result<User> = runCatching {
        userDao.getUser(userId)?.toDomain()
            ?: run {
                refreshUser(userId)
                userDao.getUser(userId)?.toDomain() ?: User()
            }
    }

    suspend fun getSuggestedUsers(): Result<List<User>> = runCatching {
        val remote = supabase.postgrest["users"]
            .select {
                order("followers_count", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(20)
            }
            .decodeList<RemoteUser>()
        remote.map { it.toDomain() }
    }

    suspend fun getFollowers(userId: String): Result<List<User>> = runCatching {
        // Join follows → users to get follower profiles
        userDao.getFollowers(userId).map { it.toDomain() }
    }

    suspend fun getFollowing(userId: String): Result<List<User>> = runCatching {
        userDao.getFollowing(userId).map { it.toDomain() }
    }

    suspend fun toggleFollow(targetUserId: String): Result<Unit> = runCatching {
        val current = userDao.getUser(targetUserId)
        if (current?.isFollowedByMe == true) {
            unfollowUser(targetUserId)
        } else {
            followUser(targetUserId)
        }
    }

    suspend fun removeFollower(followerId: String): Result<Unit> = runCatching {
        supabase.postgrest["follows"].delete {
            filter { eq("follower_id", followerId) }
        }
    }

    suspend fun blockUser(userId: String): Result<Unit> = runCatching {
        supabase.postgrest["blocks"].insert(mapOf("blocked_id" to userId))
        userDao.getUser(userId)?.copy(isBlockedByMe = true)?.let { userDao.upsertUser(it) }
    }

    suspend fun unblockUser(userId: String): Result<Unit> = runCatching {
        supabase.postgrest["blocks"].delete { filter { eq("blocked_id", userId) } }
        userDao.getUser(userId)?.copy(isBlockedByMe = false)?.let { userDao.upsertUser(it) }
    }

    suspend fun updateAccountPrivacy(isPrivate: Boolean): Result<Unit> = runCatching {
        supabase.postgrest["users"].update(mapOf("is_private" to isPrivate))
    }
}
