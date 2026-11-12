# Mero TV: build and run

The development build uses Android Views and SQLite for the library. Its default protected browser embeds GeckoView and the official signed uBlock Origin Lite extension. The system WebView remains available through the library’s Browser mode control, without extensions. It targets the confirmed Android 9/API 28 TV; no root session or host service is needed for ordinary browsing. There are no bundled websites or privileged JavaScript bridges.

## Build

Use JDK 17, Android SDK platform 37.2, and build tools 36.0.0. The wrapper pins Gradle 9.4.1 and its distribution checksum; Android Gradle Plugin is 9.2.1. The app still targets API 35 and runs on API 28; changing the compile SDK does not change device firmware. Set `JAVA_HOME` and `ANDROID_HOME`, or put the SDK location in ignored `local.properties`. For updates, set `ANDROID_USER_HOME` to the existing development signing directory before building; changing this directory can generate a different debug key and prevent an in-place update. Keep its path and keystore private.

```bash
rtk ./gradlew assembleDebug assembleDebugAndroidTest lintDebug
rtk bash scripts/check-url-policy.sh
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. It uses Android’s existing private development signing key. Retain that key for compatible development updates; release signing and distribution are not configured. Do not commit a key or use `adb uninstall` to update an installed library.

Set `ANDROID_SERIAL` privately to the intended authorized target before installation:

```bash
rtk adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
rtk adb -s "$ANDROID_SERIAL" shell am start -n dev.mero.tv/.BrowserActivity
```

## Remote controls

- Library: D-pad selects actions and cards; Select opens. Hold Select or press Menu on a card for edit, reorder, and delete. Deletion asks for confirmation.
- Page: D-pad moves the visible cursor; Select clicks. Moving to the top or bottom edge scrolls. Fast-forward and rewind scroll by a page outside fullscreen video. Menu opens native browser controls.
- Scroll mode: choose Scroll with D-pad from Menu, or use the toolbar Scroll button. D-pad then scrolls directly; switch back for pointer control.
- Protection: the uBOL toolbar button and Menu control enable or disable the extension globally. A protection change reloads the page with cache bypass. Menu also opens the extension’s own settings and filter-list controls. The tested profile uses Optimal mode and seven active filter lists. Lite has fewer controls than full uBlock Origin. Settings require protection to be enabled.
- Back: dismiss a dialog, exit fullscreen, navigate page history, then return to the library. HOME retains its system behavior.
- Fullscreen: player controls request fullscreen; Back exits. Menu remains available for browser actions. The remote Play/Pause key targets a media session when the site exposes one; active playback keeps the screen awake.

The app marks HTTP navigation as unencrypted. Website camera/microphone permissions are not implemented and remain denied by default. Unsolicited popups are blocked; a user-requested HTTP(S) window can replace the current page after confirmation. Local file/data/JavaScript/intent addresses are rejected. Tel/mail links require confirmation before opening another app. TLS errors retain each engine’s default rejection behavior. External-app links are available only through the system-WebView confirmation flow; the protected browser accepts web addresses and its own internal settings.

## Checks and controlled fixtures

```bash
rtk adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
rtk adb -s "$ANDROID_SERIAL" shell am instrument -w dev.mero.tv.test/dev.mero.tv.BrowserChecks
```

The default instrumentation uses `library-checks.db`, not the owner's `library.db`. It verifies an empty library, create/edit/delete, transactional ordering, boundary moves, and persistence after closing/reopening SQLite.

With the fixture server and tunnel below running, this additional check opens a foreground browser window, verifies the signed extension’s startup and persistent disable/enable state, and restores the prior preference:

```bash
rtk adb -s "$ANDROID_SERIAL" shell am instrument -w -e checkProtection true dev.mero.tv.test/dev.mero.tv.BrowserChecks
```

This checks the extension lifecycle. Verify actual filtering separately with `blocking.html`: the control script must load, while the test request is blocked with protection enabled and loads when disabled. Check a completed fresh page after each toggle. The fixture’s script name matches a generic rule in the pinned package.

`tests/fixtures/` provides pages for cursor clicking, history, scrolling, explicit popups, text input, and fullscreen video. Serve these from an ignored local directory. Generate the video locally rather than committing media:

```bash
rtk mkdir -p .private/browser-fixtures
rtk cp tests/fixtures/*.html tests/fixtures/*.js .private/browser-fixtures/
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

The latest decoder correction and measured playback/filtering results are in [the reliability record](browser-reliability-2026-11-11.md). The current bundled blocker is2026.1110.1816.

Build, lint, standalone URL-boundary checks, on-device SQLite checks, and extension lifecycle checks pass. On the unit, the controlled request is blocked when enabled and executes when disabled; this also passes after a cold process start. Toggling preserves cookies and local storage. Seven lists and all-site scriptlet registration were observed. The owner confirmed pointer clicking in the initial system-WebView build. Playback observations and their limits are recorded in [the current session report](local-session-2026-11-11.md). The protected engine’s D-pad clicking, edge scrolling, page scrolling, and direct Scroll mode also pass on the device.

Still pending: broad remote workflows on real sites, long-duration playback and dropped-frame measurements, process/reboot durability of user-entered cards, renderer-crash testing, release signing, and a controlled TV Bro WebView/Gecko comparison. The initial build is a development prototype.

## Source and asset attribution

Browser implementation and native vector logo assets were created for this project. The protected surface bundles GeckoView and an unchanged signed uBlock Origin Lite package; versions, hashes, licenses, and corresponding source are recorded under [GeckoView](../third_party/geckoview/SOURCE.md) and [uBlock Origin Lite](../third_party/ublock-origin-lite/SOURCE.md). No TV Bro source is incorporated. TV Bro v2.1.6's official Gecko-excluded APK was separately installed as a reference; its custom license was inspected at that tag. See [upstream release](https://github.com/truefedex/tv-bro/releases/tag/v2.1.6) and [license](https://github.com/truefedex/tv-bro/blob/v2.1.6/LICENSE.md).

Gradle wrapper files are generated by Gradle 8.9 and are covered by Gradle's [Apache-2.0 license](https://github.com/gradle/gradle/blob/v8.9.0/LICENSE). Android SDK and build tooling are external dependencies, not bundled in the app. The optional system WebView supplies its own engine and notices. The test pattern is generated locally with FFmpeg.

## Device memory profile

The confirmed 1 GB ARMv7 device uses one web content process, disables speculative process prelaunch, and limits each JavaScript heap to 128 MiB with a 4096 KiB maximum nursery. The app writes this fixed configuration into its private storage. It starts extension preparation as soon as the foreground Gecko window opens, alongside engine initialization, without first rendering a throwaway page. It waits for the pinned extension dashboard’s first contentful paint before loading the first protected website: that dashboard hides its body until its background initialization has finished. The running engine reuses this readiness result for subsequent sites; disabling or restarting the extension invalidates it. Ordinary use does not depend on an ADB configuration file or host service.

The separate GPU process is disabled to avoid the observed surface-allocation timeout; hardware compositing runs in the parent process, so GPU-process crash isolation is lost. Fission site isolation is disabled for this memory budget; web frames share a content process. Gecko’s content sandbox, origin checks, and separate extension process remain enabled. The app exposes no native messaging bridge to pages. These limits are a tested device profile, not a total-memory guarantee for arbitrary sites.

Remote debugging and console forwarding are off for ordinary use, including development builds. A local developer can opt in with the private `development` preference `engineDiagnostics=true` before restarting the app; this is honored only when the APK is debuggable. Release builds always disable it. Attaching a debugger changes the memory/performance workload, so playback benchmarks should report their state. Browser profiles, site data, screenshots, and diagnostics remain private.
