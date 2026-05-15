package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.AuthRepository
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val loggedOut: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepo.getCurrentUser()
                .onSuccess { user -> _uiState.update { it.copy(currentUser = user) } }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepo.logout()
                .onSuccess { _uiState.update { it.copy(loggedOut = true, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch { userRepo.blockUser(userId) }
    }

    fun updatePrivacySettings(isPrivate: Boolean) {
        viewModelScope.launch { userRepo.updateAccountPrivacy(isPrivate) }
    }
}
