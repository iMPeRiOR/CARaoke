package com.example.androidautolyrics.playback

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import com.example.androidautolyrics.lyrics.LyricsRepository
import com.example.androidautolyrics.model.NowPlayingTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSnapshot(
    val track: NowPlayingTrack,
    val basePositionMs: Long,
    val updateTimeMs: Long,
    val speed: Float,
    val isPlaying: Boolean,
) {
    fun currentPositionMs(nowMs: Long = SystemClock.elapsedRealtime()): Long {
        val elapsed = if (isPlaying) {
            ((nowMs - updateTimeMs).coerceAtLeast(0) * speed).toLong()
        } else {
            0L
        }
        val position = (basePositionMs + elapsed).coerceAtLeast(0)
        return if (track.durationMs > 0) position.coerceAtMost(track.durationMs) else position
    }
}

object PlaybackRepository {
    private val mutableState = MutableStateFlow<PlaybackSnapshot?>(null)
    val state: StateFlow<PlaybackSnapshot?> = mutableState.asStateFlow()

    fun update(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = (
            metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            )?.trim().orEmpty()
        if (title.isBlank() || artist.isBlank()) {
            return
        }

        val track = NowPlayingTrack(
            title = title,
            artist = artist,
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim(),
            durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
        )
        val playback = controller.playbackState
        mutableState.value = PlaybackSnapshot(
            track = track,
            basePositionMs = playback?.position?.coerceAtLeast(0) ?: 0,
            updateTimeMs = playback?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = playback?.playbackSpeed ?: 0f,
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
        )
        LyricsRepository.load(track)
    }

    fun clear() {
        mutableState.value = null
        LyricsRepository.clear()
    }
}
