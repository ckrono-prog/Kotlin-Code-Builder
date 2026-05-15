package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.FeedRepository
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.Post
import com.vibehub.domain.model.Story
import com.vibehub.util.RateLimiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val insightsPost: Post? = null,
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val userRepository: UserRepository,
    private val rateLimiter: RateLimiter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            feedRepository.observeFeed().collect { posts ->
                _uiState.update { it.copy(posts = posts) }
            }
        }
        viewModelScope.launch {
            feedRepository.observeStories().collect { stories ->
                _uiState.update { it.copy(stories = stories) }
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        feedRepository.refreshFeed()
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        feedRepository.refreshStories()
        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun loadMore() = viewModelScope.launch {
        if (_uiState.value.isLoading) return@launch
        _uiState.update { it.copy(isLoading = true) }
        val nextPage = _uiState.value.page + 1
        feedRepository.refreshFeed(page = nextPage)
            .onSuccess { _uiState.update { it.copy(page = nextPage) } }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun toggleLike(post: Post) = viewModelScope.launch {
        if (!rateLimiter.tryAcquire(RateLimiter.Action.LIKE)) return@launch
        feedRepository.toggleLike(post.id, post.isLikedByMe)
    }

    fun toggleSave(post: Post) = viewModelScope.launch {
        feedRepository.toggleSave(post.id, post.isSavedByMe)
    }

    fun markStoryViewed(storyId: String) = viewModelScope.launch {
        feedRepository.markStoryViewed(storyId)
    }

    fun followAuthor(authorId: String) = viewModelScope.launch {
        if (!rateLimiter.tryAcquire(RateLimiter.Action.FOLLOW)) {
            _uiState.update { it.copy(error = rateLimiter.errorMessage(RateLimiter.Action.FOLLOW)) }
            return@launch
        }
        userRepository.toggleFollow(authorId)
    }

    fun openInsights(post: Post) {
        _uiState.update { it.copy(insightsPost = post) }
    }

    fun closeInsights() {
        _uiState.update { it.copy(insightsPost = null) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
