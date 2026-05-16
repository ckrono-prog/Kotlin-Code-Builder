package com.vibehub.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.data.repository.AuthRepository
import com.vibehub.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val isCheckingUsername: Boolean = false,
    val usernameAvailable: Boolean? = null,
    val suggestions: List<String> = emptyList(),
    val detectedLocation: String = "",
    val profileSaved: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val usernameQuery = MutableStateFlow("")

    init {
        loadSuggestions()

        // Debounced username check
        viewModelScope.launch {
            usernameQuery
                .debounce(500)
                .filter { it.length >= 3 }
                .collectLatest { name ->
                    _uiState.update { it.copy(isCheckingUsername = true) }
                    val available = checkUsernameRemote(name)
                    _uiState.update { it.copy(isCheckingUsername = false, usernameAvailable = available) }
                }
        }
    }

    fun checkUsername(username: String) {
        _uiState.update { it.copy(usernameAvailable = null) }
        usernameQuery.value = username
    }

    private suspend fun checkUsernameRemote(username: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val result = supabase.postgrest["profiles"]
                .select { filter { eq("username", username) } }
                .decodeList<Map<String, String>>()
            result.isEmpty()
        }.getOrElse { true }
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            val email    = authRepo.currentUserEmail ?: return@launch
            val local    = email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "")
            val base     = local.take(20).ifEmpty { "user" }

            val suggestions = buildList {
                add(base)
                add("${base}${(1000..9999).random()}")
                add("${base}_${(10..99).random()}")
                add("vibe_$base")
                add("${base}hub")
            }.filter { it.length >= 3 }

            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    fun detectLocation(context: Context) {
        viewModelScope.launch {
            runCatching {
                // Use last known location via FusedLocationProviderClient
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                @Suppress("MissingPermission")
                val location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)

                if (location != null) {
                    val geocoder = android.location.Geocoder(context)
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses.first()
                        val city    = addr.locality ?: addr.subAdminArea ?: ""
                        val country = addr.countryName ?: ""
                        val loc     = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                        _uiState.update { it.copy(detectedLocation = loc) }
                    }
                }
            }
        }
    }

    fun saveProfile(
        avatarUri: Uri?,
        displayName: String,
        username: String,
        phone: String,
        location: String,
    ) {
        val uid = authRepo.currentUserId ?: run {
            _uiState.update { it.copy(error = "Not logged in") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                // Upload avatar if selected
                var avatarUrl = ""
                if (avatarUri != null) {
                    val bytes    = context.contentResolver.openInputStream(avatarUri)?.readBytes() ?: ByteArray(0)
                    val path     = "avatars/$uid/${UUID.randomUUID()}.jpg"
                    supabase.storage["vibehub-media"].upload(path, bytes, upsert = true)
                    avatarUrl    = supabase.storage["vibehub-media"].publicUrl(path)
                }

                // Upsert profile row
                supabase.postgrest["profiles"].upsert(
                    buildJsonObject {
                        put("id",             uid)
                        put("display_name",   displayName.trim())
                        put("username",       username.trim().lowercase())
                        put("avatar_url",     avatarUrl)
                        put("phone",          phone.ifBlank { null })
                        put("location",       location.trim())
                        put("onboarding_done", true)
                    }
                )

                _uiState.update { it.copy(profileSaved = true, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save profile") }
            }
        }
    }
}
