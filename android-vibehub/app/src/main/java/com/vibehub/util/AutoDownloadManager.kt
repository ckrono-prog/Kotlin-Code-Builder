package com.vibehub.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

val Context.downloadPrefs: DataStore<Preferences> by preferencesDataStore(name = "download_settings")

@Singleton
class AutoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.downloadPrefs

    object Keys {
        val AUTO_DOWNLOAD_ENABLED = booleanPreferencesKey("auto_download_enabled")
        val DOWNLOAD_OVER_WIFI    = booleanPreferencesKey("download_over_wifi")
        val DOWNLOAD_OVER_MOBILE  = booleanPreferencesKey("download_over_mobile")
        val MAX_CACHE_MB          = intPreferencesKey("max_cache_mb")
    }

    val autoDownloadEnabled: Flow<Boolean> = store.data.map { it[Keys.AUTO_DOWNLOAD_ENABLED] ?: false }
    val downloadOverWifi   : Flow<Boolean> = store.data.map { it[Keys.DOWNLOAD_OVER_WIFI]    ?: true }
    val downloadOverMobile : Flow<Boolean> = store.data.map { it[Keys.DOWNLOAD_OVER_MOBILE]  ?: false }
    val maxCacheMb         : Flow<Int>     = store.data.map { it[Keys.MAX_CACHE_MB]           ?: 500 }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) =
        store.edit { it[Keys.AUTO_DOWNLOAD_ENABLED] = enabled }

    suspend fun setDownloadOverWifi(enabled: Boolean) =
        store.edit { it[Keys.DOWNLOAD_OVER_WIFI] = enabled }

    suspend fun setDownloadOverMobile(enabled: Boolean) =
        store.edit { it[Keys.DOWNLOAD_OVER_MOBILE] = enabled }

    suspend fun setMaxCacheMb(mb: Int) =
        store.edit { it[Keys.MAX_CACHE_MB] = mb.coerceIn(100, 2000) }

    /**
     * Returns true if downloading is allowed right now based on current
     * network conditions and user preferences.
     */
    fun canDownloadNow(wifiEnabled: Boolean, mobileEnabled: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isMobile = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        return (isWifi && wifiEnabled) || (isMobile && mobileEnabled)
    }

    // ── Local storage helpers ──────────────────────────────────────────────────

    fun getDownloadsDir(): File {
        val dir = File(context.filesDir, "vibehub_downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDownloadedFile(videoId: String): File =
        File(getDownloadsDir(), "$videoId.mp4")

    fun isDownloaded(videoId: String): Boolean =
        getDownloadedFile(videoId).exists()

    fun deleteDownload(videoId: String): Boolean =
        getDownloadedFile(videoId).delete()

    fun deleteAllDownloads(): Long {
        val dir = getDownloadsDir()
        var totalBytes = 0L
        dir.listFiles()?.forEach { file ->
            totalBytes += file.length()
            file.delete()
        }
        return totalBytes
    }

    fun getDownloadedSizeBytes(): Long =
        getDownloadsDir().listFiles()?.sumOf { it.length() } ?: 0L

    fun getDownloadedVideoIds(): List<String> =
        getDownloadsDir().listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
}
