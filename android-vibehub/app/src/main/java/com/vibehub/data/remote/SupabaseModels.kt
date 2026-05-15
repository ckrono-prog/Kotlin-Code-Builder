package com.vibehub.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote DTOs — match Supabase table column names exactly.
 * These are never exposed beyond the repository layer.
 */

@Serializable
data class RemoteUser(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url")   val avatarUrl: String = "",
    @SerialName("cover_url")    val coverUrl: String = "",
    val bio: String = "",
    val location: String = "",
    @SerialName("is_verified")  val isVerified: Boolean = false,
    @SerialName("is_premium")   val isPremium: Boolean = false,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("posts_count")  val postsCount: Int = 0,
    @SerialName("joined_at")    val joinedAt: String = "",
    @SerialName("is_private")   val isPrivate: Boolean = false,
    val website: String = "",
    val pronouns: String = "",
    @SerialName("creator_level")  val creatorLevel: Int = 0,
    @SerialName("total_earnings") val totalEarnings: Double = 0.0,
    @SerialName("total_likes")    val totalLikes: Long = 0L,
    @SerialName("total_views")    val totalViews: Long = 0L,
)

@Serializable
data class RemotePost(
    val id: String = "",
    @SerialName("author_id")     val authorId: String = "",
    val caption: String = "",
    @SerialName("media_urls")    val mediaUrls: List<String> = emptyList(),
    @SerialName("media_type")    val mediaType: String = "IMAGE",
    @SerialName("likes_count")   val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("saves_count")   val savesCount: Int = 0,
    @SerialName("shares_count")  val sharesCount: Int = 0,
    @SerialName("views_count")   val viewsCount: Long = 0L,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    @SerialName("created_at")    val createdAt: String = "",
    val location: String = "",
    val visibility: String = "PUBLIC",
    @SerialName("is_pinned")     val isPinned: Boolean = false,
    @SerialName("aspect_ratio")  val aspectRatio: Float = 1f,
)

@Serializable
data class RemoteStory(
    val id: String = "",
    @SerialName("author_id")  val authorId: String = "",
    @SerialName("media_url")  val mediaUrl: String = "",
    @SerialName("media_type") val mediaType: String = "IMAGE",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    val link: String? = null,
)

@Serializable
data class RemoteMessage(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id")  val senderId: String = "",
    val text: String = "",
    @SerialName("media_url")  val mediaUrl: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("is_read")    val isRead: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_edited")  val isEdited: Boolean = false,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class RemoteConversation(
    val id: String = "",
    val type: String = "DIRECT",
    @SerialName("participant_ids") val participantIds: List<String> = emptyList(),
    @SerialName("group_name")      val groupName: String? = null,
    @SerialName("group_avatar_url") val groupAvatarUrl: String? = null,
    @SerialName("is_encrypted")    val isEncrypted: Boolean = true,
    @SerialName("created_at")      val createdAt: String = "",
)

@Serializable
data class RemoteNotification(
    val id: String = "",
    val type: String = "LIKE",
    @SerialName("actor_id")   val actorId: String = "",
    @SerialName("post_id")    val postId: String? = null,
    @SerialName("comment_id") val commentId: String? = null,
    val text: String = "",
    @SerialName("is_read")    val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)
