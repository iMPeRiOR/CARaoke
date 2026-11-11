# CARaoke

CARaoke shows the currently active, time-synchronized lyric line on Android
Auto while a song is playing in YouTube Music.

It:

1. Uses `NotificationListenerService` to discover YouTube Music's media session.
2. Reads the current title, artist, album, duration, playback position, pause
   state, and seeks.
3. Fetches synchronized LRC lyrics from the public
   [LRCLIB API](https://lrclib.net).
4. Exposes the current lyric line as an Android Auto media-browser row.

## Requirements

- Android Studio or a command-line JDK
- JDK 17 or newer
- Android SDK 35
- A phone running Android 8.0 (API 26) or newer
- Android Auto
- YouTube Music

## Clone

```shell
git clone https://github.com/iMPeRiOR/CARaoke.git
cd CARaoke
```

## Build

### Android Studio

1. Open the repository folder in Android Studio.
2. Let Gradle synchronization finish.
3. Select the `app` run configuration.
4. Build the project or run it on a connected phone.

### Command line

Set `JAVA_HOME` to a JDK 17 or newer installation and make sure Android SDK 35
is installed.

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```shell
./gradlew assembleDebug
```

The generated APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To run all local checks:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Use `./gradlew` instead of `.\gradlew.bat` on macOS or Linux.

## Install

### Android Studio

Connect the phone with USB debugging enabled and run the `app` configuration.

### ADB

From the repository root:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

On macOS or Linux, use forward slashes:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If Android Auto cached an older CARaoke build that used the Car App template,
perform a clean reinstall:

```shell
adb uninstall com.example.androidautolyrics
adb install app/build/outputs/apk/debug/app-debug.apk
```

A clean uninstall removes CARaoke's settings, including notification access.

## Configure the phone

1. Open **CARaoke** on the phone.
2. Tap **Open notification access**.
3. Enable **YouTube Music song detection**.
4. Start a song in YouTube Music.
5. Return to CARaoke and confirm that it shows:
   - the detected title and artist;
   - the current synchronized lyric; or
   - a specific loading, unavailable, or network error.

CARaoke sends the title, artist, album, and duration to LRCLIB. It does not read
notification message text or upload unrelated notification data.

## Enable CARaoke in Android Auto

Sideloaded media apps require Android Auto developer mode and the **Unknown
sources** option.

1. Open Android Auto settings on the phone.
2. Scroll to the version information near the bottom.
3. Tap **Version and permission info** repeatedly until developer mode is
   enabled.
4. Open the overflow menu in Android Auto settings.
5. Select **Developer settings**.
6. Enable **Unknown sources**.
7. Force-stop and reopen Android Auto, restart the phone, or disconnect and
   reconnect the car.
8. Open **Android Auto > Customize launcher**.
9. Enable **CARaoke**.

Then connect to the car, start a song in YouTube Music, and open **CARaoke**
from the Android Auto launcher. The current lyric is displayed as a single
media-browser row and updates as playback advances.

You can also test without a car using the
[Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu).

## Troubleshooting

### CARaoke is missing from Customize launcher

- Confirm Android Auto developer mode is enabled.
- Confirm **Developer settings > Unknown sources** is enabled.
- Install the debug APK, not an old template-based build.
- Cleanly uninstall and reinstall CARaoke if Android Auto cached an older
  manifest.
- Restart Android Auto or the phone after installation.
- Confirm the phone and Android Auto use the same Android user/profile.

### A song is detected but no lyric is displayed

- Check the status shown by the CARaoke phone app.
- Confirm the phone has internet access.
- Try a song that has synchronized lyrics on
  [LRCLIB](https://lrclib.net).
- Confirm YouTube Music still has an active media notification.
- Pause, seek, or change songs to force a media-session update.

### The lyric text is too small

Android Auto controls media-row typography; CARaoke cannot set a custom font
size. On the phone, try **Settings > Accessibility > Display size and text >
Bold text** or **Outline text**, then reconnect Android Auto.

## How it works

`YouTubeMusicNotificationListener` obtains YouTube Music's media-session token.
`PlaybackRepository` tracks metadata and calculates the current position.
`LyricsRepository` requests synchronized lyrics from LRCLIB and `LrcParser`
selects the active line.

`LyricsMediaBrowserService` publishes one browsable media item whose title is
the current lyric. It notifies Android Auto whenever the active timestamp
changes. Selecting the row opens an empty browser level because CARaoke does
not provide audio or child media content.

## Limitations

- LRCLIB does not have synchronized lyrics for every recording.
- YouTube Music must expose an active media-session token.
- Android Auto controls the media browser's layout and font size.
- This media-browser approach is intended for private and development use.
  CARaoke observes audio from another app instead of providing its own playable
  catalog, so it is unlikely to satisfy Google Play's media-app requirements.
- Displaying lyrics while driving may be restricted by local law and can
  distract the driver. Use responsibly and prioritize passenger viewing.
