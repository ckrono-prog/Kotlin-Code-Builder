package com.vibehub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehub.BuildConfig
import com.vibehub.ui.screens.home.RemoteSong
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class SongSearchUiState(
    val query: String = "",
    val tracks: List<RemoteSong> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val playingId: String? = null,
)

@Serializable
private data class SongsResponse(
    val tracks: List<RemoteSong> = emptyList(),
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SongSearchViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongSearchUiState())
    val uiState: StateFlow<SongSearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { q ->
                    if (q.length >= 2) fetchSongs(q) else _uiState.update { it.copy(tracks = emptyList(), isLoading = false) }
                }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q, error = null) }
        queryFlow.value = q
    }

    fun togglePreview(song: RemoteSong) {
        _uiState.update { it.copy(playingId = if (it.playingId == song.id) null else song.id) }
    }

    private suspend fun fetchSongs(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            // Call the songs Edge Function — never call Jamendo directly from the app
            val edgeFnUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/songs?q=${query.encodeUrl()}&limit=30"
            val session   = supabase.auth.currentSessionOrNull()
            val token     = session?.accessToken ?: BuildConfig.SUPABASE_ANON_KEY

            val resp: SongsResponse = httpClient.get(edgeFnUrl) {
                header("Authorization", "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            }.body()

            _uiState.update { it.copy(tracks = resp.tracks, isLoading = false, error = resp.error) }
        }.onFailure { e ->
            _uiState.update { it.copy(isLoading = false, error = "Could not load music: ${e.message}") }
        }
    }

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
