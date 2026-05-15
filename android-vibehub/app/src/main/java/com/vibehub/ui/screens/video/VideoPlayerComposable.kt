package com.vibehub.ui.screens.video

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vibehub.util.VideoCache
import javax.inject.Inject

/**
 * A composable video player that:
 *  - Supports all aspect ratios (letterbox / crop / stretch)
 *  - Uses the shared [VideoCache] for offline playback
 *  - Auto-plays when visible, pauses when off-screen
 */
@Composable
fun CachedVideoPlayer(
    videoUrl: String,
    videoCache: VideoCache,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    muted: Boolean = false,
    loop: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onPlaybackReady: () -> Unit = {},
) {
    val context = LocalContext.current

    val cacheDataSourceFactory = remember(videoCache) {
        CacheDataSource.Factory()
            .setCache(videoCache.cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = autoPlay
            volume = if (muted) 0f else 1f
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) onPlaybackReady()
                }
            })
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
            }
        },
        modifier = modifier,
        update = { view ->
            view.player = exoPlayer
        },
    )
}

/**
 * Smart aspect ratio selector based on video metadata ratio
 */
fun Float.toExoResizeMode(): Int = when {
    this > 1.5f -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH  // landscape / wide
    this < 0.75f -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT // portrait / tall
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT                  // square / normal
}
