package com.vibehub.data.repository

import com.vibehub.data.local.dao.ConversationDao
import com.vibehub.data.local.dao.MessageDao
import com.vibehub.data.local.dao.UserDao
import com.vibehub.data.local.entities.ConversationEntity
import com.vibehub.data.remote.RemoteConversation
import com.vibehub.data.remote.RemoteMessage
import com.vibehub.domain.model.Conversation
import com.vibehub.domain.model.ConversationType
import com.vibehub.domain.model.Message
import com.vibehub.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
) {
    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeConversations().map { entities ->
            entities.map { e ->
                val participants = Json.decodeFromString<List<String>>(e.participantsJson)
                    .mapNotNull { userId -> userDao.getUser(userId)?.toDomain() }
                Conversation(
                    id = e.id,
                    type = runCatching { ConversationType.valueOf(e.type) }.getOrElse { ConversationType.DIRECT },
                    participants = participants,
                    unreadCount = e.unreadCount,
                    groupName = e.groupName,
                    groupAvatarUrl = e.groupAvatarUrl,
                    isMuted = e.isMuted,
                    theme = e.theme,
                    isEncrypted = e.isEncrypted,
                )
            }
        }

    fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeMessages(conversationId).map { entities ->
            entities.map { e ->
                val sender = userDao.getUser(e.senderId)?.toDomain() ?: User()
                e.toDomain(sender)
            }
        }

    suspend fun refreshConversations(): Result<Unit> = runCatching {
        val remote = supabase.postgrest["conversations"]
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<RemoteConversation>()

        conversationDao.upsertConversations(remote.map { rc ->
            ConversationEntity(
                id = rc.id, type = rc.type,
                participantsJson = Json.encodeToString(rc.participantIds),
                lastMessageJson = null,
                unreadCount = 0,
                groupName = rc.groupName,
                groupAvatarUrl = rc.groupAvatarUrl,
                isMuted = false, theme = "default",
                isEncrypted = rc.isEncrypted,
            )
        })
    }

    suspend fun refreshMessages(conversationId: String): Result<Unit> = runCatching {
        val remote = supabase.postgrest["messages"]
            .select {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
                limit(50)
            }
            .decodeList<RemoteMessage>()

        messageDao.upsertMessages(remote.map { it.toEntity() })
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        mediaUrl: String? = null,
        mediaType: String? = null,
        replyToMessageId: String? = null,
    ): Result<Unit> = runCatching {
        supabase.postgrest["messages"].insert(
            buildMap {
                put("conversation_id", conversationId)
                put("text", text)
                mediaUrl?.let { put("media_url", it) }
                mediaType?.let { put("media_type", it) }
                replyToMessageId?.let { put("reply_to_message_id", it) }
            }
        )
    }

    suspend fun editMessage(messageId: String, newText: String): Result<Unit> = runCatching {
        messageDao.editMessage(messageId, newText)
        supabase.postgrest["messages"].update(
            mapOf("text" to newText, "is_edited" to true)
        ) { filter { eq("id", messageId) } }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        messageDao.softDelete(messageId)
        supabase.postgrest["messages"].update(
            mapOf("is_deleted" to true)
        ) { filter { eq("id", messageId) } }
    }

    suspend fun markConversationRead(conversationId: String): Result<Unit> = runCatching {
        messageDao.markAllRead(conversationId)
        conversationDao.clearUnread(conversationId)
    }

    // Alias for OfflineSyncManager compatibility
    suspend fun sendMessage(conversationId: String, text: String) =
        sendMessage(conversationId, text, null, null, null)

    // Flow of messages for a conversation (alias used by ChatViewModel)
    fun getMessagesFlow(conversationId: String) = observeMessages(conversationId)

    /** Broadcast typing indicator via Supabase Realtime presence channel */
    suspend fun broadcastTyping(conversationId: String, isTyping: Boolean): Result<Unit> =
        runCatching {
            // Wire to Supabase Realtime presence: channel("typing:$conversationId").track(...)
        }

    /** Observe remote user typing state */
    fun observeTyping(conversationId: String): kotlinx.coroutines.flow.Flow<Boolean> =
        kotlinx.coroutines.flow.flow {
            // Wire to Supabase Realtime presence channel
            emit(false)
        }

    /** Observe remote user online state */
    fun observeOnlineStatus(conversationId: String): kotlinx.coroutines.flow.Flow<Boolean> =
        kotlinx.coroutines.flow.flow {
            // Wire to Supabase Realtime presence channel
            emit(false)
        }

    /** Add an emoji reaction to a message */
    suspend fun addReaction(messageId: String, emoji: String): Result<Unit> = runCatching {
        supabase.postgrest["message_reactions"].insert(
            mapOf("message_id" to messageId, "emoji" to emoji)
        )
    }

    /** Archive a conversation */
    suspend fun archiveConversation(conversationId: String): Result<Unit> = runCatching {
        conversationDao.archive(conversationId)
    }

    /** Delete all messages in a conversation */
    suspend fun deleteAllMessages(conversationId: String): Result<Unit> = runCatching {
        messageDao.deleteAllInConversation(conversationId)
        supabase.postgrest["messages"].delete {
            filter { eq("conversation_id", conversationId) }
        }
    }
}
