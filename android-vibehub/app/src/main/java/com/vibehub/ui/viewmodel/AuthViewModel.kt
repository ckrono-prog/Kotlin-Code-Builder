package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.AuthRepository
import com.vibehub.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isEmailConfirmed: Boolean = false,
    val profileComplete: Boolean = false,
    val error: String? = null,
    val step: AuthStep = AuthStep.LOGIN,
    val pendingEmail: String = "",
)

enum class AuthStep {
    LOGIN,
    REGISTER,
    OTP_VERIFY,
    PROFILE_SETUP,
    FORGOT_PASSWORD,
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn()
            if (loggedIn) {
                val confirmed = authRepository.isEmailConfirmed()
                val profileOk = userRepository.hasProfile(authRepository.currentUserId ?: "")
                _uiState.update {
                    it.copy(
                        isLoggedIn       = true,
                        isEmailConfirmed = confirmed,
                        profileComplete  = profileOk,
                        step             = when {
                            !confirmed  -> AuthStep.OTP_VERIFY
                            !profileOk  -> AuthStep.PROFILE_SETUP
                            else        -> AuthStep.LOGIN
                        },
                    )
                }
            }
        }
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signUpWithEmail(email, password)
            .onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, step = AuthStep.OTP_VERIFY, pendingEmail = email, isLoggedIn = true)
                }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = parseError(e.message)) }
            }
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signInWithEmail(email, password)
            .onSuccess {
                val confirmed = authRepository.isEmailConfirmed()
                val profileOk = userRepository.hasProfile(authRepository.currentUserId ?: "")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        isEmailConfirmed = confirmed,
                        profileComplete = profileOk,
                        step = when {
                            !confirmed -> AuthStep.OTP_VERIFY
                            !profileOk -> AuthStep.PROFILE_SETUP
                            else       -> AuthStep.LOGIN
                        },
                    )
                }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = parseError(e.message)) }
            }
    }

    fun verifyOtp(email: String, token: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.verifyEmailOtp(email, token)
            .onSuccess {
                val profileOk = userRepository.hasProfile(authRepository.currentUserId ?: "")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmailConfirmed = true,
                        profileComplete  = profileOk,
                        step             = if (profileOk) AuthStep.LOGIN else AuthStep.PROFILE_SETUP,
                    )
                }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "Invalid or expired code. Try again.") }
            }
    }

    fun resendOtp(email: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.resendOtp(email)
            .onSuccess  { _uiState.update { it.copy(isLoading = false) } }
            .onFailure  { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun resetPassword(email: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.resetPassword(email)
            .onSuccess { _uiState.update { it.copy(isLoading = false, step = AuthStep.LOGIN, error = "Reset email sent — check your inbox.") } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun markProfileComplete() {
        _uiState.update { it.copy(profileComplete = true) }
    }

    fun signOut() = viewModelScope.launch {
        authRepository.signOut()
        _uiState.update { AuthUiState(isLoggedIn = false) }
    }

    fun setStep(step: AuthStep) = _uiState.update { it.copy(step = step, error = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun parseError(msg: String?): String = when {
        msg == null                                -> "An error occurred. Please try again."
        msg.contains("already registered")        -> "This email is already registered. Sign in instead."
        msg.contains("Invalid login credentials") -> "Wrong email or password."
        msg.contains("Email not confirmed")       -> "Please confirm your email first."
        msg.contains("rate limit")                -> "Too many attempts. Please wait a moment."
        msg.contains("disposable")                -> "Disposable emails are not allowed."
        else                                      -> msg
    }
}
