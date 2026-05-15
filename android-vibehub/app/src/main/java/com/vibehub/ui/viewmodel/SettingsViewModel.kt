package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.AuthRepository
import com.vibehub.data.repository.UserRepository
import com.vibehub.domain.model.User
import com.vibehub.util.AutoDownloadManager
import com.vibehub.util.VideoCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val loggedOut: Boolean = false,
    val error: String? = null,
    // Download / storage
    val autoDownloadEnabled: Boolean = false,
    val downloadOverWifi: Boolean = true,
    val downloadOverMobile: Boolean = false,
    val maxCacheMb: Int = 500,
    val downloadedSizeMb: Int = 0,
    val cachedSizeMb: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val autoDownloadManager: AutoDownloadManager,
    private val videoCache: VideoCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        observeDownloadSettings()
        refreshStorageStats()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepo.getCurrentUser()
                .onSuccess { user -> _uiState.update { it.copy(currentUser = user) } }
        }
    }

    private fun observeDownloadSettings() {
        viewModelScope.launch {
            combine(
                autoDownloadManager.autoDownloadEnabled,
                autoDownloadManager.downloadOverWifi,
                autoDownloadManager.downloadOverMobile,
                autoDownloadManager.maxCacheMb,
            ) { enabled, wifi, mobile, maxMb ->
                _uiState.update {
                    it.copy(
                        autoDownloadEnabled = enabled,
                        downloadOverWifi    = wifi,
                        downloadOverMobile  = mobile,
                        maxCacheMb          = maxMb,
                    )
                }
            }.collect()
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val dlBytes = autoDownloadManager.getDownloadedSizeBytes()
            _uiState.update {
                it.copy(
                    downloadedSizeMb = (dlBytes / 1024 / 1024).toInt(),
                    cachedSizeMb     = 0, // ExoPlayer cache size query would go here
                )
            }
        }
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch { autoDownloadManager.setAutoDownloadEnabled(enabled) }
    }

    fun setDownloadOverWifi(enabled: Boolean) {
        viewModelScope.launch { autoDownloadManager.setDownloadOverWifi(enabled) }
    }

    fun setDownloadOverMobile(enabled: Boolean) {
        viewModelScope.launch { autoDownloadManager.setDownloadOverMobile(enabled) }
    }

    fun setMaxCacheMb(mb: Int) {
        viewModelScope.launch { autoDownloadManager.setMaxCacheMb(mb) }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            autoDownloadManager.deleteAllDownloads()
            refreshStorageStats()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            // Release and recreate SimpleCache clears the stream cache
            videoCache.release()
            refreshStorageStats()
        }
    }

    fun clearAll() {
        clearDownloads()
        clearCache()
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
