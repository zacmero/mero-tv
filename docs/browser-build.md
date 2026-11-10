# Mero TV: build and run

The first native development build uses Android Views, SQLite, and the installed WebView. It targets the confirmed Android 9/API 28 TV; no root session or host service is needed for ordinary browsing. There are no bundled websites or privileged JavaScript bridges.

## Build

Use JDK 17, the Android SDK with platform 35, and Android build tools. Gradle 8.9 is pinned by the wrapper, including its distribution checksum; Android Gradle Plugin is pinned to 8.7.3. Set `JAVA_HOME` and `ANDROID_HOME`, or put the SDK location in ignored `local.properties`.

```bash
rtk ./gradlew assembleDebug assembleDebugAndroidTest lintDebug
rtk bash scripts/check-url-policy.sh
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. It uses Android's local development signing key outside this checkout. Retain that key for compatible development updates; release signing and distribution are not configured. Do not commit a key or use `adb uninstall` to update an installed library.

Set `ANDROID_SERIAL` privately to the intended authorized target before installation:

```bash
rtk adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
rtk adb -s "$ANDROID_SERIAL" shell am start -n dev.mero.tv/.BrowserActivity
```

## Remote controls

- Library: D-pad selects actions and cards; Select opens. Hold Select or press Menu on a card for edit, reorder, and delete. Deletion asks for confirmation.
- Page: D-pad moves the visible cursor; Select clicks. Press Menu for library, address, save, reload/stop, cursor/page-focus mode, scrolling mode, and cursor speed.
- Scroll mode: D-pad scrolls instead of moving the cursor. Menu switches back.
- Back: dismiss a dialog, exit fullscreen, navigate page history, then return to the library. HOME retains its system behavior.
- Fullscreen: player controls request fullscreen; Back exits. Menu remains available for browser actions.

The app marks HTTP navigation as unencrypted. Website camera/microphone permissions are not implemented and remain denied by default. Unsolicited popups are blocked; a user-requested HTTP(S) window can replace the current page after confirmation. Local file/data/JavaScript/intent addresses are rejected. Tel/mail links require confirmation before opening another app. TLS errors retain the WebView's default rejection behavior.

## Checks and controlled fixtures

```bash
rtk adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
rtk adb -s "$ANDROID_SERIAL" shell am instrument -w dev.mero.tv.test/dev.mero.tv.BrowserChecks
```

The instrumentation uses `library-checks.db`, not the owner's `library.db`. It verifies an empty library, create/edit/delete, transactional ordering, boundary moves, and persistence after closing/reopening SQLite.

`tests/fixtures/` provides pages for cursor clicking, history, scrolling, explicit popups, text input, and fullscreen video. Serve these from an ignored local directory. Generate the video locally rather than committing media:

```bash
rtk mkdir -p .private/browser-fixtures
rtk cp tests/fixtures/browser.html tests/fixtures/next.html tests/fixtures/media.html .private/browser-fixtures/
rtk ffmpeg -f lavfi -i testsrc2=size=1920x1080:rate=60 -t 10 -c:v libx264 -preset ultrafast -pix_fmt yuv420p -b:v 5M -movflags +faststart .private/browser-fixtures/motion.mp4
rtk python3 -m http.server 8765 --bind 127.0.0.1 --directory .private/browser-fixtures
```

In a separate terminal:

```bash
rtk adb -s "$ANDROID_SERIAL" reverse tcp:8765 tcp:8765
rtk adb -s "$ANDROID_SERIAL" shell am start -a android.intent.action.VIEW -d http://127.0.0.1:8765/browser.html -n dev.mero.tv/.BrowserActivity
```

Remove the test tunnel after testing with `rtk adb -s "$ANDROID_SERIAL" reverse --remove tcp:8765`. The loopback server serves static test files only and is not a privileged device service.

## Evidence and remaining work

Build, lint, standalone URL-boundary checks, and on-device SQLite checks pass. The owner confirmed pointer clicking. Startup, initial app memory, and local H.264 video observations are recorded in the session report. These do not establish compatibility with personal sites or sustained video performance.

Still pending: broad remote workflows on real sites, long-duration playback and dropped-frame measurements, process/reboot durability of user-entered cards, renderer-crash testing, release signing, and a controlled TV Bro WebView/Gecko comparison. The initial build is a development prototype.

## Source and asset attribution

Browser implementation and native vector logo assets were created for this project; no TV Bro source or bundled browser engine is incorporated. TV Bro v2.1.6's official Gecko-excluded APK was separately installed as a reference; its custom license was inspected at that tag. See [upstream release](https://github.com/truefedex/tv-bro/releases/tag/v2.1.6) and [license](https://github.com/truefedex/tv-bro/blob/v2.1.6/LICENSE.md).

Gradle wrapper files are generated by Gradle 8.9 and are covered by Gradle's [Apache-2.0 license](https://github.com/gradle/gradle/blob/v8.9.0/LICENSE). Android SDK and build tooling are external dependencies, not bundled in the app. The system WebView supplies its own engine and notices. The test pattern is generated locally with FFmpeg.
