package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.FeedRepository
import com.vibehub.domain.model.Post
import com.vibehub.domain.model.Story
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
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        // Observe local DB
        viewModelScope.launch {
            feedRepository.observeFeed()
                .collect { posts ->
                    _uiState.update { it.copy(posts = posts) }
                }
        }
        viewModelScope.launch {
            feedRepository.observeStories()
                .collect { stories ->
                    _uiState.update { it.copy(stories = stories) }
                }
        }
        // Initial fetch
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
        feedRepository.toggleLike(post.id, post.isLikedByMe)
    }

    fun toggleSave(post: Post) = viewModelScope.launch {
        feedRepository.toggleSave(post.id, post.isSavedByMe)
    }

    fun markStoryViewed(storyId: String) = viewModelScope.launch {
        feedRepository.markStoryViewed(storyId)
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
