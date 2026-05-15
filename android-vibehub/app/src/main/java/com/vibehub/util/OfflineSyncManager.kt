package com.vibehub.util

import com.vibehub.data.repository.FeedRepository
import com.vibehub.data.repository.MessagingRepository
import com.vibehub.data.repository.NotificationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches connectivity and re-syncs all offline data when the network returns.
 * Call [start] once from a long-lived scope (e.g., a Service or Application).
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val connectivity: ConnectivityObserver,
    private val feedRepository: FeedRepository,
    private val messagingRepository: MessagingRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Pending outbound messages queued while offline
    private val outboundQueue = ArrayDeque<PendingMessage>()

    fun start(scope: CoroutineScope) {
        scope.launch {
            connectivity.networkStatus.collect { status ->
                if (status == NetworkStatus.Available) {
                    syncAll(scope)
                }
            }
        }
    }

    private fun syncAll(scope: CoroutineScope) = scope.launch {
        _syncState.value = SyncState.Syncing
        try {
            // Flush outbound queue first
            flushOutboundQueue()

            awaitAll(
                async { feedRepository.refreshFeed() },
                async { feedRepository.refreshStories() },
                async { messagingRepository.refreshConversations() },
                async { notificationRepository.refresh() },
            )
            _syncState.value = SyncState.Done
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    fun enqueueMessage(conversationId: String, text: String) {
        outboundQueue.add(PendingMessage(conversationId, text, System.currentTimeMillis()))
    }

    private suspend fun flushOutboundQueue() {
        val iterator = outboundQueue.iterator()
        while (iterator.hasNext()) {
            val msg = iterator.next()
            messagingRepository.sendMessage(msg.conversationId, msg.text)
                .onSuccess { iterator.remove() }
        }
    }
}

data class PendingMessage(
    val conversationId: String,
    val text: String,
    val queuedAt: Long,
)

sealed class SyncState {
    object Idle    : SyncState()
    object Syncing : SyncState()
    object Done    : SyncState()
    data class Error(val message: String) : SyncState()
}
