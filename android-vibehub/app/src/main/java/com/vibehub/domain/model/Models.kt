package com.vibehub.domain.model

import kotlinx.serialization.Serializable

// ──────────────────────────────────────────────────────────────────────────────
// Core domain models — plain Kotlin data classes, UI-layer safe
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val bio: String = "",
    val location: String = "",
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val joinedAt: String = "",
    val isFollowedByMe: Boolean = false,
    val isFollowingMe: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isPrivate: Boolean = false,
    val website: String = "",
    val pronouns: String = "",
    val gender: String = "",
    val birthday: String = "",
    val creatorLevel: Int = 0,
    val totalEarnings: Double = 0.0,
    val totalLikes: Long = 0L,
    val totalViews: Long = 0L,
    // Names of mutual friends who also follow this user (for Friends screen)
    val followedBy: List<String> = emptyList(),
)

@Serializable
data class Post(
    val id: String = "",
    val authorId: String = "",
    val author: User = User(),
    val caption: String = "",
    val mediaUrls: List<String> = emptyList(),
    val mediaType: MediaType = MediaType.IMAGE,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val savesCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Long = 0L,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val createdAt: String = "",
    val location: String = "",
    val musicTrack: MusicTrack? = null,
    val aspectRatio: Float = 1f,
    val isPinned: Boolean = false,
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val commentPermission: CommentPermission = CommentPermission.EVERYONE,
    val reelsData: ReelsData? = null,
)

@Serializable
data class ReelsData(
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val audioName: String = "",
    val hasAudio: Boolean = true,
)

@Serializable
data class Story(
    val id: String = "",
    val authorId: String = "",
    val author: User = User(),
    val mediaUrl: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val musicTrack: MusicTrack? = null,
    val expiresAt: String = "",
    val viewsCount: Int = 0,
    val isViewedByMe: Boolean = false,
    val stickers: List<StorySticker> = emptyList(),
    val poll: StoryPoll? = null,
    val quiz: StoryQuiz? = null,
    val link: String? = null,
    val createdAt: String = "",
)

@Serializable
data class StorySticker(
    val id: String = "",
    val type: StickerType = StickerType.EMOJI,
    val content: String = "",
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

@Serializable
data class StoryPoll(
    val question: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val votesA: Int = 0,
    val votesB: Int = 0,
    val myVote: String? = null,
)

@Serializable
data class StoryQuiz(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val myAnswerIndex: Int? = null,
)

@Serializable
data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val author: User = User(),
    val text: String = "",
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val parentId: String? = null,
    val repliesCount: Int = 0,
    val replies: List<Comment> = emptyList(),
    val createdAt: String = "",
)

@Serializable
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val sender: User = User(),
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaType: MediaType? = null,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val reactions: Map<String, Int> = emptyMap(),
    val replyToMessageId: String? = null,
    val expiresAt: String? = null,
    val createdAt: String = "",
)

@Serializable
data class Conversation(
    val id: String = "",
    val type: ConversationType = ConversationType.DIRECT,
    val participants: List<User> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val groupName: String? = null,
    val groupAvatarUrl: String? = null,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val theme: String = "default",
    val isEncrypted: Boolean = true,
)

@Serializable
data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val actorId: String = "",
    val actor: User = User(),
    val postId: String? = null,
    val commentId: String? = null,
    val text: String = "",
    val isRead: Boolean = false,
    val createdAt: String = "",
)

@Serializable
data class MusicTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val coverUrl: String = "",
    val previewUrl: String = "",
    val durationSeconds: Int = 0,
)

@Serializable
data class Highlight(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val stories: List<Story> = emptyList(),
    val createdAt: String = "",
)

@Serializable
data class LiveStream(
    val id: String = "",
    val hostId: String = "",
    val host: User = User(),
    val title: String = "",
    val thumbnailUrl: String = "",
    val viewerCount: Int = 0,
    val isLive: Boolean = true,
    val streamUrl: String = "",
    val giftCount: Int = 0,
    val startedAt: String = "",
)

@Serializable
data class CreatorAnalytics(
    val totalViews: Long = 0L,
    val totalLikes: Long = 0L,
    val totalShares: Long = 0L,
    val totalComments: Long = 0L,
    val followersGained: Int = 0,
    val followersLost: Int = 0,
    val reachCount: Long = 0L,
    val impressions: Long = 0L,
    val engagementRate: Double = 0.0,
    val topPosts: List<Post> = emptyList(),
    val audienceByAge: Map<String, Int> = emptyMap(),
    val audienceByGender: Map<String, Int> = emptyMap(),
    val audienceByLocation: Map<String, Int> = emptyMap(),
    val dailyViews: List<DailyMetric> = emptyList(),
    val totalEarnings: Double = 0.0,
    val monthlyEarnings: Double = 0.0,
)

@Serializable
data class DailyMetric(
    val date: String = "",
    val value: Long = 0L,
)

@Serializable
data class Product(
    val id: String = "",
    val sellerId: String = "",
    val seller: User = User(),
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "USD",
    val imageUrls: List<String> = emptyList(),
    val category: String = "",
    val isDigital: Boolean = false,
    val stock: Int = 0,
    val ordersCount: Int = 0,
    val rating: Float = 0f,
)

// ──────────────────────────────────────────────────────────────────────────────
// Enums
// ──────────────────────────────────────────────────────────────────────────────

enum class MediaType { IMAGE, VIDEO, CAROUSEL, REEL, AUDIO }
enum class PostVisibility(val label: String) {
    PUBLIC("Everyone"),
    FOLLOWERS("Followers"),
    CLOSE_FRIENDS("Close Friends"),
    PRIVATE("Only Me"),
}
enum class CommentPermission(val label: String) {
    EVERYONE("Everyone"),
    FOLLOWERS("Followers"),
    CLOSE_FRIENDS("Close Friends"),
    NO_ONE("No One"),
}
enum class ConversationType { DIRECT, GROUP }
enum class NotificationType {
    LIKE, COMMENT, FOLLOW, MENTION, SHARE, LIVE, STORY_REACT,
    GIFT, PURCHASE, SYSTEM, CHALLENGE
}
enum class StickerType { EMOJI, GIF, TEXT, MENTION, HASHTAG, LOCATION, MUSIC, POLL, QUIZ, COUNTDOWN }
