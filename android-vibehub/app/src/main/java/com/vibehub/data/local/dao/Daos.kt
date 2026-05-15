package com.vibehub.data.local.dao

import androidx.room.*
import com.vibehub.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────────────────────────────────────
// User DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Upsert
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE cachedAt < :before")
    suspend fun deleteStale(before: Long)

    @Query("""
        SELECT u.* FROM users u
        INNER JOIN follows f ON u.id = f.follower_id
        WHERE f.following_id = :userId
    """)
    suspend fun getFollowers(userId: String): List<UserEntity>

    @Query("""
        SELECT u.* FROM users u
        INNER JOIN follows f ON u.id = f.following_id
        WHERE f.follower_id = :userId
    """)
    suspend fun getFollowing(userId: String): List<UserEntity>
}

// ──────────────────────────────────────────────────────────────────────────────
// Post DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeFeed(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE authorId = :userId ORDER BY createdAt DESC")
    fun observeUserPosts(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE authorId = :userId AND isPinned = 1")
    suspend fun getPinnedPosts(userId: String): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id")
    fun observePost(id: String): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE mediaType = 'REEL' ORDER BY createdAt DESC")
    fun observeReels(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE caption LIKE '%' || :query || '%' OR hashtags LIKE '%' || :query || '%'")
    suspend fun searchPosts(query: String): List<PostEntity>

    @Upsert
    suspend fun upsertPost(post: PostEntity)

    @Upsert
    suspend fun upsertPosts(posts: List<PostEntity>)

    @Query("UPDATE posts SET isLikedByMe = :liked, likesCount = likesCount + :delta WHERE id = :postId")
    suspend fun updateLike(postId: String, liked: Boolean, delta: Int)

    @Query("UPDATE posts SET isSavedByMe = :saved WHERE id = :postId")
    suspend fun updateSave(postId: String, saved: Boolean)

    @Query("DELETE FROM posts WHERE cachedAt < :before")
    suspend fun deleteStale(before: Long)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
}

// ──────────────────────────────────────────────────────────────────────────────
// Story DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface StoryDao {

    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun observeStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE authorId = :userId ORDER BY createdAt DESC")
    fun observeUserStories(userId: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isViewedByMe = 0 ORDER BY createdAt DESC")
    fun observeUnseenStories(): Flow<List<StoryEntity>>

    @Upsert
    suspend fun upsertStory(story: StoryEntity)

    @Upsert
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("UPDATE stories SET isViewedByMe = 1 WHERE id = :storyId")
    suspend fun markViewed(storyId: String)

    @Query("DELETE FROM stories WHERE expiresAt < :now")
    suspend fun deleteExpired(now: String)
}

// ──────────────────────────────────────────────────────────────────────────────
// Message DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessage(id: String): MessageEntity?

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId")
    suspend fun markAllRead(conversationId: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun softDelete(messageId: String)

    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE id = :messageId")
    suspend fun editMessage(messageId: String, newText: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteAllInConversation(conversationId: String)
}

// ──────────────────────────────────────────────────────────────────────────────
// Conversation DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY cachedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversations(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearUnread(conversationId: String)

    @Query("UPDATE conversations SET isArchived = 1 WHERE id = :conversationId")
    suspend fun archive(conversationId: String)

    @Query("UPDATE conversations SET isArchived = 0 WHERE id = :conversationId")
    suspend fun unarchive(conversationId: String)
}

// ──────────────────────────────────────────────────────────────────────────────
// Notification DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsertNotification(n: NotificationEntity)

    @Upsert
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markRead(notificationId: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE cachedAt < :before")
    suspend fun deleteStale(before: Long)
}

// ──────────────────────────────────────────────────────────────────────────────
// Highlight DAO
// ──────────────────────────────────────────────────────────────────────────────

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeHighlights(userId: String): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsertHighlight(highlight: HighlightEntity)

    @Upsert
    suspend fun upsertHighlights(highlights: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)
}
