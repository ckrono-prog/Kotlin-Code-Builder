package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.CommentsRepository
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.Comment
import com.vibehub.util.RateLimiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val page: Int = 0,
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentsRepo: CommentsRepository,
    private val userRepo: UserRepository,
    private val rateLimiter: RateLimiter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private val pageSize = 20

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, page = 0) }
            commentsRepo.getCommentsForPost(postId)
                .onSuccess { all ->
                    _uiState.update {
                        it.copy(
                            comments   = all.take(pageSize),
                            totalCount = all.size,
                            hasMore    = all.size > pageSize,
                            isLoading  = false,
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun loadMoreComments(postId: String) {
        val nextPage = _uiState.value.page + 1
        viewModelScope.launch {
            commentsRepo.getCommentsForPost(postId)
                .onSuccess { all ->
                    val loaded = all.take((nextPage + 1) * pageSize)
                    _uiState.update {
                        it.copy(
                            comments = loaded,
                            hasMore  = all.size > loaded.size,
                            page     = nextPage,
                        )
                    }
                }
        }
    }

    fun postComment(postId: String, text: String, parentId: String? = null) {
        if (text.isBlank()) return
        if (!rateLimiter.tryAcquire(RateLimiter.Action.COMMENT)) {
            _uiState.update { it.copy(error = rateLimiter.errorMessage(RateLimiter.Action.COMMENT)) }
            return
        }
        viewModelScope.launch {
            commentsRepo.addComment(postId, text.trim(), parentId)
                .onSuccess { comment ->
                    _uiState.update { it.copy(comments = it.comments + comment, totalCount = it.totalCount + 1, error = null) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            commentsRepo.deleteComment(comment.id)
                .onSuccess {
                    _uiState.update { it.copy(comments = it.comments.filter { c -> c.id != comment.id }, totalCount = it.totalCount - 1) }
                }
        }
    }

    fun likeComment(comment: Comment) {
        viewModelScope.launch { commentsRepo.toggleLike(comment.id) }
    }

    fun followUser(userId: String) {
        viewModelScope.launch { userRepo.toggleFollow(userId) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
