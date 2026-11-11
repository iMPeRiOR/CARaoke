package com.example.androidautolyrics.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.androidautolyrics.R
import com.example.androidautolyrics.lyrics.LrcParser
import com.example.androidautolyrics.lyrics.LyricsRepository
import com.example.androidautolyrics.lyrics.LyricsState
import com.example.androidautolyrics.playback.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LyricsMediaBrowserService : MediaBrowserServiceCompat() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mediaSession: MediaSessionCompat
    private var refreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, SESSION_TAG).apply {
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
                    .build(),
            )
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        refreshJob = scope.launch {
            var lastItemId: String? = null
            while (isActive) {
                val itemId = currentItem().mediaId
                if (itemId != lastItemId) {
                    lastItemId = itemId
                    notifyChildrenChanged(ROOT_ID)
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        if (parentId == ROOT_ID) {
            result.sendResult(mutableListOf(currentItem()))
        } else {
            result.sendResult(mutableListOf())
        }
    }

    override fun onDestroy() {
        refreshJob?.cancel()
        scope.cancel()
        mediaSession.release()
        super.onDestroy()
    }

    private fun currentItem(): MediaBrowserCompat.MediaItem {
        val playback = PlaybackRepository.state.value
        val title: String
        val subtitle: String
        val itemId: String

        if (playback == null) {
            title = getString(R.string.car_no_song)
            subtitle = getString(R.string.car_open_youtube_music)
            itemId = "status:no-song"
        } else {
            subtitle = getString(
                R.string.car_track_subtitle,
                playback.track.title,
                playback.track.artist,
            )
            when (val lyrics = LyricsRepository.state.value) {
                is LyricsState.Available -> {
                    val line = if (lyrics.trackKey == playback.track.key) {
                        LrcParser.lineAt(lyrics.lines, playback.currentPositionMs())
                    } else {
                        null
                    }
                    title = line?.text ?: getString(R.string.car_waiting_first_line)
                    itemId = "line:${playback.track.key}:${line?.timestampMs ?: -1}"
                }
                is LyricsState.Failed -> {
                    title = getString(R.string.car_lyrics_failed)
                    itemId = "status:failed:${playback.track.key}"
                }
                is LyricsState.Unavailable -> {
                    title = getString(R.string.car_lyrics_unavailable)
                    itemId = "status:unavailable:${playback.track.key}"
                }
                LyricsState.Idle,
                is LyricsState.Loading,
                -> {
                    title = getString(R.string.car_lyrics_loading)
                    itemId = "status:loading:${playback.track.key}"
                }
            }
        }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId(itemId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
        return MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE,
        )
    }

    private companion object {
        const val ROOT_ID = "caraoke-root"
        const val SESSION_TAG = "CARaoke"
        const val REFRESH_INTERVAL_MS = 250L
    }
}
