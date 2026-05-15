package com.vibehub.util

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a shared ExoPlayer SimpleCache for video pre-fetching and offline playback.
 * Cache size: 500 MB (reels + home feed videos).
 */
@Singleton
class VideoCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheDir = File(context.cacheDir, "vibehub_video_cache")

    val cache: SimpleCache by lazy {
        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(500L * 1024 * 1024), // 500 MB
            StandaloneDatabaseProvider(context),
        )
    }

    fun release() {
        cache.release()
    }
}
