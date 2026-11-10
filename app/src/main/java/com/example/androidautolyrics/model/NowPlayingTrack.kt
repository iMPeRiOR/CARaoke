package com.example.androidautolyrics.model

data class NowPlayingTrack(
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
) {
    val key: String = listOf(title, artist, album.orEmpty(), (durationMs / 1_000).toString())
        .joinToString("|") { it.trim().lowercase() }
}
