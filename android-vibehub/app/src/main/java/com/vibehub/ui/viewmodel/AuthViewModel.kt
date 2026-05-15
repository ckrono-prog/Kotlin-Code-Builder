package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val step: AuthStep = AuthStep.LOGIN,
)

enum class AuthStep { LOGIN, REGISTER, OTP, FORGOT_PASSWORD, PROFILE_SETUP }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isLoggedIn = authRepository.isLoggedIn()) }
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signInWithEmail(email, password)
            .onSuccess { _uiState.update { it.copy(isLoading = false, isLoggedIn = true) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signUpWithEmail(email, password)
            .onSuccess { _uiState.update { it.copy(isLoading = false, step = AuthStep.PROFILE_SETUP) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun sendOtp(phone: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.sendOtp(phone)
            .onSuccess { _uiState.update { it.copy(isLoading = false, otpSent = true, step = AuthStep.OTP) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun verifyOtp(phone: String, token: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.verifyOtp(phone, token)
            .onSuccess { _uiState.update { it.copy(isLoading = false, isLoggedIn = true) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun resetPassword(email: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.resetPassword(email)
            .onSuccess { _uiState.update { it.copy(isLoading = false, step = AuthStep.LOGIN) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun signOut() = viewModelScope.launch {
        authRepository.signOut()
        _uiState.update { AuthUiState(isLoggedIn = false) }
    }

    fun setStep(step: AuthStep) = _uiState.update { it.copy(step = step, error = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
