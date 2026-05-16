package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val users = userRepo.getBlockedUsers()
            _uiState.update { it.copy(users = users, isLoading = false) }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            userRepo.unblockUser(userId)
                .onSuccess {
                    _uiState.update { it.copy(users = it.users.filter { u -> u.id != userId }) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
