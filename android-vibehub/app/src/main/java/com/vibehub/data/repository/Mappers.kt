package com.vibehub.data.repository

import com.vibehub.data.local.entities.*
import com.vibehub.data.remote.*
import com.vibehub.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

// ──────────────────────────────────────────────────────────
// Remote → Entity (cache to DB)
// ──────────────────────────────────────────────────────────

fun RemoteUser.toEntity() = UserEntity(
    id = id, username = username, displayName = displayName,
    avatarUrl = avatarUrl, coverUrl = coverUrl, bio = bio,
    location = location, isVerified = isVerified, isPremium = isPremium,
    followersCount = followersCount, followingCount = followingCount,
    postsCount = postsCount, joinedAt = joinedAt, isFollowedByMe = false,
    isFollowingMe = false, isBlockedByMe = false,
    isPrivate = isPrivate, website = website, pronouns = pronouns,
)

fun RemotePost.toEntity() = PostEntity(
    id = id, authorId = authorId, caption = caption,
    mediaUrls = json.encodeToString(mediaUrls),
    mediaType = mediaType,
    likesCount = likesCount, commentsCount = commentsCount,
    savesCount = savesCount, sharesCount = sharesCount,
    viewsCount = viewsCount,
    hashtags = json.encodeToString(hashtags),
    isLikedByMe = false, isSavedByMe = false,
    createdAt = createdAt, location = location,
    visibility = visibility, isPinned = isPinned,
    aspectRatio = aspectRatio, musicTrackJson = null, reelsDataJson = null,
)

fun RemoteStory.toEntity() = StoryEntity(
    id = id, authorId = authorId, mediaUrl = mediaUrl,
    mediaType = mediaType, expiresAt = expiresAt,
    viewsCount = viewsCount, isViewedByMe = false,
    createdAt = createdAt, stickersJson = "[]",
    pollJson = null, quizJson = null, link = link,
)

fun RemoteMessage.toEntity() = MessageEntity(
    id = id, conversationId = conversationId, senderId = senderId,
    text = text, mediaUrl = mediaUrl, mediaType = mediaType,
    isRead = isRead, isDeleted = isDeleted, isEdited = isEdited,
    reactionsJson = "{}",
    replyToMessageId = replyToMessageId, expiresAt = expiresAt,
    createdAt = createdAt,
)

fun RemoteNotification.toEntity() = NotificationEntity(
    id = id, type = type, actorId = actorId,
    postId = postId, commentId = commentId, text = text,
    isRead = isRead, createdAt = createdAt,
)

// ──────────────────────────────────────────────────────────
// Remote → Domain (bypass Room, for one-shot queries)
// ──────────────────────────────────────────────────────────

fun RemoteUser.toDomain() = User(
    id = id, username = username, displayName = displayName,
    avatarUrl = avatarUrl, coverUrl = coverUrl, bio = bio,
    location = location, isVerified = isVerified, isPremium = isPremium,
    followersCount = followersCount, followingCount = followingCount,
    postsCount = postsCount, joinedAt = joinedAt,
    isFollowedByMe = false, isFollowingMe = false, isBlockedByMe = false,
    isPrivate = isPrivate, website = website, pronouns = pronouns,
)

fun RemotePost.toDomain(authorUser: User = User()) = Post(
    id = id, authorId = authorId, author = authorUser,
    caption = caption, mediaUrls = mediaUrls,
    mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrElse { MediaType.IMAGE },
    likesCount = likesCount, commentsCount = commentsCount,
    savesCount = savesCount, sharesCount = sharesCount, viewsCount = viewsCount,
    hashtags = hashtags, isLikedByMe = false, isSavedByMe = false,
    createdAt = createdAt, location = location,
    visibility = runCatching { PostVisibility.valueOf(visibility) }.getOrElse { PostVisibility.PUBLIC },
    isPinned = isPinned, aspectRatio = aspectRatio,
)

// ──────────────────────────────────────────────────────────
// Entity → Domain model (feed to UI)
// ──────────────────────────────────────────────────────────

fun UserEntity.toDomain() = User(
    id = id, username = username, displayName = displayName,
    avatarUrl = avatarUrl, coverUrl = coverUrl, bio = bio,
    location = location, isVerified = isVerified, isPremium = isPremium,
    followersCount = followersCount, followingCount = followingCount,
    postsCount = postsCount, joinedAt = joinedAt,
    isFollowedByMe = isFollowedByMe, isFollowingMe = isFollowingMe,
    isBlockedByMe = isBlockedByMe, isPrivate = isPrivate,
    website = website, pronouns = pronouns, gender = gender, birthday = birthday,
)

fun PostEntity.toDomain(authorUser: User = User()) = Post(
    id = id, authorId = authorId, author = authorUser,
    caption = caption,
    mediaUrls = runCatching { json.decodeFromString<List<String>>(mediaUrls) }.getOrElse { emptyList() },
    mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrElse { MediaType.IMAGE },
    likesCount = likesCount, commentsCount = commentsCount,
    savesCount = savesCount, sharesCount = sharesCount, viewsCount = viewsCount,
    hashtags = runCatching { json.decodeFromString<List<String>>(hashtags) }.getOrElse { emptyList() },
    isLikedByMe = isLikedByMe, isSavedByMe = isSavedByMe,
    createdAt = createdAt, location = location,
    visibility = runCatching { PostVisibility.valueOf(visibility) }.getOrElse { PostVisibility.PUBLIC },
    isPinned = isPinned, aspectRatio = aspectRatio,
)

fun StoryEntity.toDomain(authorUser: User = User()) = Story(
    id = id, authorId = authorId, author = authorUser,
    mediaUrl = mediaUrl,
    mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrElse { MediaType.IMAGE },
    expiresAt = expiresAt, viewsCount = viewsCount,
    isViewedByMe = isViewedByMe, createdAt = createdAt, link = link,
)

fun MessageEntity.toDomain(senderUser: User = User()) = Message(
    id = id, conversationId = conversationId,
    senderId = senderId, sender = senderUser,
    text = text, mediaUrl = mediaUrl,
    mediaType = mediaType?.let { runCatching { MediaType.valueOf(it) }.getOrNull() },
    isRead = isRead, isDeleted = isDeleted, isEdited = isEdited,
    replyToMessageId = replyToMessageId, expiresAt = expiresAt, createdAt = createdAt,
)

fun NotificationEntity.toDomain(actorUser: User = User()) = Notification(
    id = id,
    type = runCatching { NotificationType.valueOf(type) }.getOrElse { NotificationType.LIKE },
    actorId = actorId, actor = actorUser,
    postId = postId, commentId = commentId,
    text = text, isRead = isRead, createdAt = createdAt,
)
