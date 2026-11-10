package com.example.androidautolyrics.playback

import android.app.Notification
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class YouTubeMusicNotificationListener : NotificationListenerService() {
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            controller?.let(PlaybackRepository::update)
        }

        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
            controller?.let(PlaybackRepository::update)
        }

        override fun onSessionDestroyed() {
            detach()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications
            ?.filter { it.packageName == YOUTUBE_MUSIC_PACKAGE }
            ?.maxByOrNull(StatusBarNotification::getPostTime)
            ?.let(::attach)
    }

    override fun onNotificationPosted(notification: StatusBarNotification) {
        if (notification.packageName == YOUTUBE_MUSIC_PACKAGE) {
            attach(notification)
        }
    }

    override fun onNotificationRemoved(notification: StatusBarNotification) {
        if (notification.packageName != YOUTUBE_MUSIC_PACKAGE) {
            return
        }
        val replacement = activeNotifications
            ?.filter { it.packageName == YOUTUBE_MUSIC_PACKAGE }
            ?.maxByOrNull(StatusBarNotification::getPostTime)
        if (replacement == null) {
            detach()
        } else {
            attach(replacement)
        }
    }

    @Suppress("DEPRECATION")
    private fun attach(notification: StatusBarNotification) {
        val extras = notification.notification.extras
        val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        } ?: return

        controller?.unregisterCallback(callback)
        controller = MediaController(this, token).also {
            it.registerCallback(callback)
            PlaybackRepository.update(it)
        }
    }

    private fun detach() {
        controller?.unregisterCallback(callback)
        controller = null
        PlaybackRepository.clear()
    }

    override fun onDestroy() {
        controller?.unregisterCallback(callback)
        controller = null
        super.onDestroy()
    }

    private companion object {
        const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    }
}
