package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.CommentsRepository
import com.vibehub.domain.model.Comment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val repo: CommentsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.getCommentsForPost(postId)
                .onSuccess { comments ->
                    _uiState.update { it.copy(comments = comments, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun postComment(postId: String, text: String, parentId: String? = null) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.addComment(postId, text, parentId)
                .onSuccess { comment ->
                    _uiState.update { state ->
                        state.copy(comments = state.comments + comment)
                    }
                }
        }
    }

    fun likeComment(comment: Comment) {
        viewModelScope.launch {
            repo.toggleLike(comment.id)
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repo.deleteComment(comment.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(comments = state.comments.filter { it.id != comment.id })
                    }
                }
        }
    }
}
