package com.vibehub.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.MessagingRepository
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.Message
import com.vibehub.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val remoteUser: User? = null,
    val draftText: String = "",
    val replyTo: Message? = null,
    val isSending: Boolean = false,
    val remoteIsTyping: Boolean = false,
    val remoteIsOnline: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagingRepo: MessagingRepository,
    private val userRepo: UserRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        observeTypingIndicator()
        observeOnlineStatus()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            messagingRepo.getMessagesFlow(conversationId)
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
        }
    }

    // Wire to Supabase Realtime presence channel
    private fun observeTypingIndicator() {
        viewModelScope.launch {
            messagingRepo.observeTyping(conversationId)
                .collect { isTyping ->
                    _uiState.update { it.copy(remoteIsTyping = isTyping) }
                }
        }
    }

    private fun observeOnlineStatus() {
        viewModelScope.launch {
            messagingRepo.observeOnlineStatus(conversationId)
                .collect { isOnline ->
                    _uiState.update { it.copy(remoteIsOnline = isOnline) }
                }
        }
    }

    fun setDraftText(text: String) {
        _uiState.update { it.copy(draftText = text) }
        // Broadcast typing indicator via Supabase Realtime
        viewModelScope.launch {
            messagingRepo.broadcastTyping(conversationId, text.isNotBlank())
        }
    }

    fun setReplyTo(message: Message?) {
        _uiState.update { it.copy(replyTo = message) }
    }

    fun sendMessage() {
        val text = _uiState.value.draftText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, draftText = "", replyTo = null) }
            messagingRepo.sendMessage(
                conversationId = conversationId,
                text           = text,
                replyToId      = _uiState.value.replyTo?.id,
            )
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messagingRepo.deleteMessage(messageId)
            _uiState.update { state ->
                state.copy(messages = state.messages.map {
                    if (it.id == messageId) it.copy(isDeleted = true, text = "") else it
                })
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            messagingRepo.addReaction(messageId, emoji)
        }
    }

    fun markMessagesRead() {
        viewModelScope.launch {
            messagingRepo.markConversationRead(conversationId)
        }
    }
}
