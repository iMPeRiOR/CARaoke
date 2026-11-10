# Android Auto Lyrics

A small Kotlin app that:

1. Uses `NotificationListenerService` to discover YouTube Music's media session.
2. Reads the current title, artist, duration, playback position, pause state, and seeks.
3. Fetches time-synced LRC lyrics from the public LRCLIB API.
4. Shows only the lyric line matching the current playback position in an AndroidX Car App template.

## Run

Requirements:

- Android Studio with JDK 17
- Android SDK 35
- A phone running Android 8.0 or newer
- Android Auto developer mode for sideloaded templated apps

Open the project in Android Studio and run the `app` configuration. On the phone:

1. Open **Car Lyrics**.
2. Tap **Open notification access** and allow **YouTube Music song detection**.
3. Start a song in YouTube Music.
4. Connect Android Auto and open **Car Lyrics**.

For Desktop Head Unit testing, enable developer mode in the Android Auto phone app, allow unknown sources, start the DHU, and run the debug build.

## Important Android Auto limitation

The AndroidX Car App Library currently accepts templated apps only in Google's supported categories (navigation, POI, IoT, and weather). This sample declares the IoT category so it can be loaded during local/developer testing, but a standalone lyrics app does not naturally qualify for that category and is unlikely to pass Google Play review. Driver-distraction and regional rules may also prohibit displaying lyrics while driving. Test responsibly and do not rely on this sample for Play Store distribution.

Debug builds accept any Car App host so the Desktop Head Unit and Android Auto developer mode can connect. Release builds intentionally have no host allowlist; add the official host certificates required for your distribution channel before shipping.

## Notes

- The app reads media metadata but never reads notification text or uploads notification content.
- Lyrics requests include title, artist, album, and duration. LRCLIB requires a descriptive `User-Agent`; update the project URL in `LyricsClient` if you publish a fork.
- LRCLIB coverage varies. The car screen reports when synchronized lyrics are unavailable.
- YouTube Music must expose an active media-session token in its notification.
