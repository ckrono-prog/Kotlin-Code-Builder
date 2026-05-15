package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.User
import com.vibehub.ui.screens.call.CallState
import com.vibehub.ui.screens.call.CallType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallUiState(
    val remoteUser: User? = null,
    val callType: CallType = CallType.AUDIO,
    val callState: CallState = CallState.RINGING,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isIncoming: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    fun initiateCall(remoteUserId: String, callType: CallType) {
        viewModelScope.launch {
            _uiState.update { it.copy(callType = callType, callState = CallState.RINGING) }
            userRepo.getUserById(remoteUserId)
                .onSuccess { user ->
                    _uiState.update { it.copy(remoteUser = user) }
                }
            // Simulate connecting after 3 seconds (replace with real Supabase Realtime signaling)
            delay(3_000)
            _uiState.update { it.copy(callState = CallState.CONNECTING) }
            delay(1_500)
            _uiState.update { it.copy(callState = CallState.ACTIVE) }
            startTimer()
        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            _uiState.update { it.copy(callState = CallState.ACTIVE) }
            startTimer()
        }
    }

    fun endCall() {
        _uiState.update { it.copy(callState = CallState.ENDED) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCamera() {
        _uiState.update { it.copy(isCameraOff = !it.isCameraOff) }
    }

    fun toggleSpeaker() {
        _uiState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (_uiState.value.callState == CallState.ACTIVE) {
                delay(1_000)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }
}
