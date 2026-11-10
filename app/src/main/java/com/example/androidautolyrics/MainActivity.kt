package com.example.androidautolyrics

import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidautolyrics.playback.PlaybackRepository
import com.example.androidautolyrics.playback.YouTubeMusicNotificationListener
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var permissionStatus: TextView
    private lateinit var nowPlaying: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionStatus = TextView(this).apply {
            textSize = 16f
        }
        nowPlaying = TextView(this).apply {
            textSize = 20f
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
            addView(nowPlaying, matchWidth(topMargin = 48))
        }
        setContentView(content)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlaybackRepository.state.collect { snapshot ->
                    nowPlaying.text = snapshot?.track?.let {
                        "Detected:\n${it.title}\n${it.artist}"
                    } ?: "No YouTube Music song detected"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionStatus.text = if (hasNotificationAccess()) {
            "Notification access is enabled"
        } else {
            "Notification access is required"
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val service = ComponentName(this, YouTubeMusicNotificationListener::class.java)
        return Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.split(':')
            ?.any { ComponentName.unflattenFromString(it) == service } == true
    }

    private fun matchWidth(topMargin: Int = 16): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = topMargin
        }
}
