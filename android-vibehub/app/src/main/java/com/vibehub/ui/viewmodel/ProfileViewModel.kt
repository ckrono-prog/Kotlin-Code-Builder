package com.vibehub.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.Highlight
import com.vibehub.domain.model.Post
import com.vibehub.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val reels: List<Post> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.POSTS,
    val isLoading: Boolean = false,
    val isOwnProfile: Boolean = false,
    val error: String? = null,
)

enum class ProfileTab { POSTS, REELS, SAVED }

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeUser(userId).collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            userRepository.observeUserPosts(userId).collect { posts ->
                _uiState.update {
                    it.copy(
                        posts = posts.filter { p -> p.mediaType.name != "REEL" },
                        reels = posts.filter { p -> p.mediaType.name == "REEL" },
                    )
                }
            }
        }
        viewModelScope.launch {
            userRepository.observeHighlights(userId).collect { highlights ->
                _uiState.update { it.copy(highlights = highlights) }
            }
        }
        loadProfile()
    }

    private fun loadProfile() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        userRepository.refreshUser(userId)
        userRepository.refreshUserPosts(userId)
        _uiState.update { it.copy(isLoading = false) }
    }

    fun setTab(tab: ProfileTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun followUser() = viewModelScope.launch {
        userRepository.followUser(userId)
    }

    fun unfollowUser() = viewModelScope.launch {
        userRepository.unfollowUser(userId)
    }

    fun updateProfile(displayName: String, bio: String, location: String, website: String) =
        viewModelScope.launch {
            userRepository.updateProfile(displayName, bio, location, website)
                .onSuccess { loadProfile() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
}
