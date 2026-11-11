package com.example.androidautolyrics

import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidautolyrics.lyrics.LrcParser
import com.example.androidautolyrics.lyrics.LyricsRepository
import com.example.androidautolyrics.lyrics.LyricsState
import com.example.androidautolyrics.playback.PlaybackRepository
import com.example.androidautolyrics.playback.YouTubeMusicNotificationListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var permissionStatus: TextView
    private lateinit var installSourceStatus: TextView
    private lateinit var nowPlaying: TextView
    private lateinit var lyricsStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionStatus = TextView(this).apply {
            textSize = 16f
        }
        installSourceStatus = TextView(this).apply {
            textSize = 16f
        }
        nowPlaying = TextView(this).apply {
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }
        lyricsStatus = TextView(this).apply {
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }
        val button = Button(this).apply {
            text = getString(R.string.open_notification_access)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 48)
            addView(TextView(context).apply {
                text = getString(R.string.app_name)
                textSize = 30f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = getString(R.string.setup_instructions)
                textSize = 17f
                setPadding(0, 32, 0, 32)
            })
            addView(permissionStatus, matchWidth())
            addView(button, matchWidth())
            addView(installSourceStatus, matchWidth(topMargin = 32))
            addView(nowPlaying, matchWidth(topMargin = 48))
            addView(lyricsStatus, matchWidth(topMargin = 32))
        }
        setContentView(ScrollView(this).apply { addView(content) })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    renderPlaybackAndLyrics()
                    delay(250)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionStatus.text = if (hasNotificationAccess()) {
            getString(R.string.notification_access_enabled)
        } else {
            getString(R.string.notification_access_required)
        }
        installSourceStatus.text = if (isInstalledByGooglePlay()) {
            getString(R.string.play_install_detected)
        } else {
            getString(R.string.sideload_install_detected)
        }
    }

    private fun renderPlaybackAndLyrics() {
        val playback = PlaybackRepository.state.value
        nowPlaying.text = playback?.track?.let {
            getString(R.string.detected_song, it.title, it.artist)
        } ?: getString(R.string.no_song_detected)

        lyricsStatus.text = when (val lyrics = LyricsRepository.state.value) {
            is LyricsState.Available -> {
                if (playback == null || lyrics.trackKey != playback.track.key) {
                    getString(R.string.lyrics_loading)
                } else {
                    LrcParser.lineAt(lyrics.lines, playback.currentPositionMs())?.text?.let {
                        getString(R.string.current_lyric, it)
                    } ?: getString(R.string.lyrics_before_first_line)
                }
            }
            is LyricsState.Failed -> getString(R.string.lyrics_failed, lyrics.message)
            is LyricsState.Loading -> getString(R.string.lyrics_loading)
            is LyricsState.Unavailable -> getString(R.string.lyrics_unavailable)
            LyricsState.Idle -> getString(R.string.lyrics_waiting)
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val service = ComponentName(this, YouTubeMusicNotificationListener::class.java)
        return Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.split(':')
            ?.any { ComponentName.unflattenFromString(it) == service } == true
    }

    @Suppress("DEPRECATION")
    private fun isInstalledByGooglePlay(): Boolean {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            packageManager.getInstallerPackageName(packageName)
        }
        return installer == GOOGLE_PLAY_PACKAGE
    }

    private fun matchWidth(topMargin: Int = 16): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = topMargin
        }

    private companion object {
        const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
    }
}
