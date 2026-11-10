package com.example.androidautolyrics.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.example.androidautolyrics.BuildConfig

class LyricsCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        if (BuildConfig.DEBUG) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this).build()
        }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = LyricsCarSession()
}

private class LyricsCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = LyricsScreen(carContext)
}
