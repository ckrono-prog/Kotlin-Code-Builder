package com.vibehub.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.MessagingRepository
import com.vibehub.domain.model.Conversation
import com.vibehub.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val draftText: String = "",
    val replyTo: Message? = null,
    val error: String? = null,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            messagingRepository.observeConversations().collect { convos ->
                _uiState.update { it.copy(conversations = convos) }
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        messagingRepository.refreshConversations()
        _uiState.update { it.copy(isLoading = false) }
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            messagingRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            messagingRepository.refreshMessages(conversationId)
            messagingRepository.markConversationRead(conversationId)
        }
    }

    fun setDraftText(text: String) = _uiState.update { it.copy(draftText = text) }
    fun setReplyTo(message: Message?) = _uiState.update { it.copy(replyTo = message) }

    fun sendMessage() = viewModelScope.launch {
        val text = _uiState.value.draftText.trim()
        if (text.isEmpty()) return@launch
        _uiState.update { it.copy(isSending = true, draftText = "", replyTo = null) }
        messagingRepository.sendMessage(
            conversationId = conversationId,
            text = text,
            replyToMessageId = _uiState.value.replyTo?.id,
        )
        _uiState.update { it.copy(isSending = false) }
    }

    fun deleteMessage(messageId: String) = viewModelScope.launch {
        messagingRepository.deleteMessage(messageId)
    }

    fun editMessage(messageId: String, newText: String) = viewModelScope.launch {
        messagingRepository.editMessage(messageId, newText)
    }
}
