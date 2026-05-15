package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val suggested: List<User> = emptyList(),
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    // Track dismissed suggestions locally (could persist to DB)
    private val dismissedIds = mutableSetOf<String>()
    // Track local follow state overrides
    private val followOverrides = mutableMapOf<String, Boolean>()

    fun loadAll(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val suggestedDeferred  = async { userRepo.getSuggestedUsers() }
                val followersDeferred  = async { userRepo.getFollowers(userId) }
                val followingDeferred  = async { userRepo.getFollowing(userId) }

                val suggested  = suggestedDeferred.await().getOrDefault(emptyList())
                val followers  = followersDeferred.await().getOrDefault(emptyList())
                val following  = followingDeferred.await().getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        suggested  = suggested.filter { u -> u.id !in dismissedIds },
                        followers  = followers,
                        following  = following,
                        isLoading  = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val current = followOverrides[userId] ?: false
            followOverrides[userId] = !current
            userRepo.toggleFollow(userId)
        }
    }

    fun removeFollower(userId: String) {
        viewModelScope.launch {
            userRepo.removeFollower(userId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(followers = state.followers.filter { it.id != userId })
                    }
                }
        }
    }

    fun dismissSuggestion(userId: String) {
        dismissedIds.add(userId)
        _uiState.update { state ->
            state.copy(suggested = state.suggested.filter { it.id != userId })
        }
    }
}
