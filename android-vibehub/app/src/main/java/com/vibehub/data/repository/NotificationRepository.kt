package com.vibehub.data.repository

import com.vibehub.data.local.dao.NotificationDao
import com.vibehub.data.local.dao.UserDao
import com.vibehub.data.remote.RemoteNotification
import com.vibehub.domain.model.Notification
import com.vibehub.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val notificationDao: NotificationDao,
    private val userDao: UserDao,
) {
    fun observeNotifications(): Flow<List<Notification>> =
        notificationDao.observeNotifications().map { entities ->
            entities.map { e ->
                val actor = userDao.getUser(e.actorId)?.toDomain() ?: User()
                e.toDomain(actor)
            }
        }

    fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    suspend fun refresh(): Result<Unit> = runCatching {
        val remote = supabase.postgrest["notifications"]
            .select {
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeList<RemoteNotification>()

        remote.map { it.actorId }.distinct().forEach { actorId ->
            runCatching {
                val user = supabase.postgrest["users"]
                    .select { filter { eq("id", actorId) } }
                    .decodeSingle<com.vibehub.data.remote.RemoteUser>()
                userDao.upsertUser(user.toEntity())
            }
        }

        notificationDao.upsertNotifications(remote.map { it.toEntity() })
    }

    suspend fun markRead(notificationId: String): Result<Unit> = runCatching {
        notificationDao.markRead(notificationId)
        supabase.postgrest["notifications"].update(
            mapOf("is_read" to true)
        ) { filter { eq("id", notificationId) } }
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        notificationDao.markAllRead()
        supabase.postgrest["notifications"].update(
            mapOf("is_read" to true)
        )
    }
}
