package com.vibehub.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ──────────────────────────────────────────────────────────────────────────────
// Room Entities — local offline-first cache
// ──────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val coverUrl: String,
    val bio: String,
    val location: String,
    val isVerified: Boolean,
    val isPremium: Boolean,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val joinedAt: String,
    val isFollowedByMe: Boolean,
    val isFollowingMe: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isPrivate: Boolean,
    val website: String,
    val pronouns: String,
    val gender: String = "",
    val birthday: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "posts")
@TypeConverters(StringListConverter::class)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val caption: String,
    val mediaUrls: String,       // JSON-encoded List<String>
    val mediaType: String,
    val likesCount: Int,
    val commentsCount: Int,
    val savesCount: Int,
    val sharesCount: Int,
    val viewsCount: Long,
    val hashtags: String,        // JSON-encoded List<String>
    val isLikedByMe: Boolean,
    val isSavedByMe: Boolean,
    val createdAt: String,
    val location: String,
    val visibility: String,
    val isPinned: Boolean,
    val aspectRatio: Float,
    val musicTrackJson: String?,  // JSON-encoded MusicTrack
    val reelsDataJson: String?,   // JSON-encoded ReelsData
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val mediaUrl: String,
    val mediaType: String,
    val expiresAt: String,
    val viewsCount: Int,
    val isViewedByMe: Boolean,
    val createdAt: String,
    val stickersJson: String,  // JSON-encoded
    val pollJson: String?,
    val quizJson: String?,
    val link: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val participantsJson: String, // JSON-encoded List<String> of user ids
    val lastMessageJson: String?,
    val unreadCount: Int,
    val groupName: String?,
    val groupAvatarUrl: String?,
    val isMuted: Boolean,
    val isArchived: Boolean = false,
    val theme: String,
    val isEncrypted: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val mediaUrl: String?,
    val mediaType: String?,
    val isRead: Boolean,
    val isDeleted: Boolean,
    val isEdited: Boolean,
    val reactionsJson: String,
    val replyToMessageId: String?,
    val expiresAt: String?,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val actorId: String,
    val postId: String?,
    val commentId: String?,
    val text: String,
    val isRead: Boolean,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val coverUrl: String,
    val storyIdsJson: String,
    val createdAt: String,
)

// ──────────────────────────────────────────────────────────────────────────────
// Type Converters
// ──────────────────────────────────────────────────────────────────────────────

class StringListConverter {
    @TypeConverter
    fun fromList(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    fun toList(json: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(json) }.getOrElse { emptyList() }
}
