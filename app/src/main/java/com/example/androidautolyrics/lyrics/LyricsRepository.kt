package com.example.androidautolyrics.lyrics

import com.example.androidautolyrics.model.NowPlayingTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LyricsState {
    data object Idle : LyricsState
    data class Loading(val trackKey: String) : LyricsState
    data class Available(val trackKey: String, val lines: List<LyricLine>) : LyricsState
    data class Unavailable(val trackKey: String) : LyricsState
    data class Failed(val trackKey: String, val message: String) : LyricsState
}

object LyricsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = LyricsClient()
    private val mutableState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    private var request: Job? = null

    val state: StateFlow<LyricsState> = mutableState.asStateFlow()

    @Synchronized
    fun load(track: NowPlayingTrack) {
        val currentTrackKey = when (val current = mutableState.value) {
            is LyricsState.Available -> current.trackKey
            is LyricsState.Failed -> current.trackKey
            is LyricsState.Loading -> current.trackKey
            is LyricsState.Unavailable -> current.trackKey
            LyricsState.Idle -> null
        }
        if (currentTrackKey == track.key) {
            return
        }

        request?.cancel()
        mutableState.value = LyricsState.Loading(track.key)
        request = scope.launch {
            try {
                val syncedLyrics = client.fetch(track)
                val lines = syncedLyrics?.let(LrcParser::parse).orEmpty()
                mutableState.value = if (lines.isEmpty()) {
                    LyricsState.Unavailable(track.key)
                } else {
                    LyricsState.Available(track.key, lines)
                }
            } catch (error: Exception) {
                mutableState.value = LyricsState.Failed(
                    trackKey = track.key,
                    message = error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    @Synchronized
    fun clear() {
        request?.cancel()
        mutableState.value = LyricsState.Idle
    }
}
