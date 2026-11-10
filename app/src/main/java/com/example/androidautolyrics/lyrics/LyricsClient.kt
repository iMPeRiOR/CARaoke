package com.example.androidautolyrics.lyrics

import android.net.Uri
import com.example.androidautolyrics.model.NowPlayingTrack
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.roundToLong

class LyricsClient {
    fun fetch(track: NowPlayingTrack): String? {
        val exactUrl = endpoint("get", track, includeAlbum = true, includeDuration = true)
        request(exactUrl)?.let { return JSONObject(it).optSyncedLyrics() }

        val searchUrl = endpoint("search", track, includeAlbum = false, includeDuration = false)
        val response = request(searchUrl) ?: return null
        val records = JSONArray(response)
        val durationSeconds = track.durationMs / 1_000.0

        return (0 until records.length())
            .asSequence()
            .map(records::getJSONObject)
            .filter { !it.isNull("syncedLyrics") }
            .minByOrNull {
                abs(it.optDouble("duration", durationSeconds) - durationSeconds)
            }
            ?.optSyncedLyrics()
    }

    private fun endpoint(
        operation: String,
        track: NowPlayingTrack,
        includeAlbum: Boolean,
        includeDuration: Boolean,
    ): URL {
        val builder = Uri.parse("$BASE_URL/$operation").buildUpon()
            .appendQueryParameter("track_name", track.title)
            .appendQueryParameter("artist_name", track.artist)

        if (includeAlbum && !track.album.isNullOrBlank()) {
            builder.appendQueryParameter("album_name", track.album)
        }
        if (includeDuration && track.durationMs > 0) {
            builder.appendQueryParameter(
                "duration",
                (track.durationMs / 1_000.0).roundToLong().toString(),
            )
        }
        return URL(builder.build().toString())
    }

    private fun request(url: URL): String? {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)

        return try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException(
                    "LRCLIB returned HTTP ${connection.responseCode}: " +
                        connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty(),
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optSyncedLyrics(): String? =
        optString("syncedLyrics").takeUnless { it.isBlank() || it == "null" }

    private companion object {
        const val BASE_URL = "https://lrclib.net/api"
        const val USER_AGENT =
            "AndroidAutoLyrics v1.0 (https://github.com/1413090/androidautolyrics)"
    }
}
