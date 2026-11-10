package com.example.androidautolyrics.car

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.androidautolyrics.lyrics.LrcParser
import com.example.androidautolyrics.lyrics.LyricsRepository
import com.example.androidautolyrics.lyrics.LyricsState
import com.example.androidautolyrics.playback.PlaybackRepository

class LyricsScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {
    private val handler = Handler(Looper.getMainLooper())
    private var lastText: String? = null

    private val refresh = object : Runnable {
        override fun run() {
            val text = displayText()
            if (text != lastText) {
                lastText = text
                invalidate()
            }
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        handler.post(refresh)
    }

    override fun onStop(owner: LifecycleOwner) {
        handler.removeCallbacks(refresh)
    }

    override fun onGetTemplate(): Template =
        PaneTemplate.Builder(
            Pane.Builder()
                .addRow(
                    Row.Builder()
                        .setTitle("♪")
                        .addText(displayText())
                        .build(),
                )
                .build(),
        )
            .setTitle("Car Lyrics")
            .setHeaderAction(Action.APP_ICON)
            .build()

    private fun displayText(): String {
        val playback = PlaybackRepository.state.value
            ?: return "Play a song in YouTube Music"
        return when (val lyrics = LyricsRepository.state.value) {
            is LyricsState.Available -> {
                if (lyrics.trackKey != playback.track.key) {
                    "Finding synced lyrics..."
                } else {
                    LrcParser.lineAt(lyrics.lines, playback.currentPositionMs())?.text ?: "♪"
                }
            }
            is LyricsState.Loading -> "Finding synced lyrics..."
            is LyricsState.Unavailable -> "No synced lyrics found"
            is LyricsState.Failed -> "Could not load lyrics"
            LyricsState.Idle -> "Finding synced lyrics..."
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 250L
    }
}
