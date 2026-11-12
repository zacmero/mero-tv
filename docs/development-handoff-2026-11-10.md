# Development handoff — paused at owner request, 2026-11-10

## Current state

Work is paused for the day. No new commit or push was made for the extension work. Preserve all current edits and the pre-existing untracked `.serena/` directory.

The installed development APK is 0.2.0. It adds GeckoView 159.0.20261102144711 and an unmodified Mozilla-signed uBlock Origin Lite 2026.1103.1743 package. Build, Android test APK assembly, and lint passed for the latest installed source. Gradle 9.4.1, AGP 9.2.1, JDK 17, and compile SDK 37.2 are used; device min API 28 and target API 35 remain unchanged. No firmware or driver changes were performed for this work.

The native library and system-WebView alternative remain. Four owner-requested destinations were imported privately, preserving the original entry: five total. Destinations are not bundled or recorded here.

## Observed results

- Full uBlock Origin 1.76.0 installed and toggled, but its background/page setup repeatedly lost processes as memory rose. It is retained disabled in the experimental browser profile, and is no longer the bundled blocker.
- uBlock Origin Lite blocked a controlled generic filter request while the control script loaded. Disabling it allowed the test script. Re-enabling exposed a startup/reload race; the latest code now waits for GeckoView's extension `onReady` callback. That change builds and installs, but its complete off/on filtering cycle still needs verification.
- Fast-forward page scrolling, direct D-pad Scroll mode, and pointer edge scrolling each reached the fixture's bottom on the device.
- YouTube's mobile homepage opened inside Mero TV. Video playback and YouTube ad blocking have NOT been verified.
- The last working Lite fixture checks used a constrained runtime configuration. Removing those limits and restoring the default Fission setting produced unreliable white-page rendering. The precise minimal configuration is not yet established.

## Resume here

1. Read the project handoff/device/privacy instructions and inspect the current Git diff. Keep raw evidence and site data private.
2. The last probe restored a private debug configuration with Fission off, one content process, prelaunch off, a 128 MiB JS heap limit, and a 4096 KiB maximum nursery. The source APK itself currently uses default Fission and disables remote debugging. The constrained configuration is still loaded from the device's development configuration file; it is not a durable app setting. The latest captured constrained-profile result has not been inspected.
3. Inspect `.private/lite-ready-limits.png`, `.private/final-off.png`, `.private/final-on.png`, and the private logs. Establish a reliable, minimal memory configuration and implement it in the app before claiming readiness. Avoid reviving the previous broad tuning experiments without evidence.
4. Verify cold startup, actual off/on filtering, settings/filter-list access, and remote scrolling with the final configuration. Re-run the on-device extension instrumentation, which now checks startup/options-page readiness as well as enable/disable persistence. Library instrumentation must keep using its separate test database.
5. Test YouTube video playback, fullscreen/Back, and filtering inside Mero TV. A homepage load or an enabled label does not prove ad blocking. Keep playback evidence and request details private.
6. Finish source/license notices for Lite, update the build guide and session evidence, and review public diffs for private data. The old full-uBO notice folder remains from the prototype; the new Lite folder currently contains its license only. The pinned Lite source tag is `2026.1103.1743` in `uBlockOrigin/uBOL-home`, commit `e03161e379f8316b28a42df33c397d50716b2ab7`.

## Cleanup performed at pause

The experimental browser was force-stopped. The original screensaver setting was restored. The local fixture HTTP server and the test ADB reverse/forward tunnels were stopped/removed. No test service is intended to continue overnight. Private development configurations and evidence are retained for resumption.

Relevant private inputs: `.private/adb-target`, `.private/requested-sites.json`, `.private/geckoview-test-config.yaml`. Do not print their personal data or commit them. Restore the fixture server and ADB reverse tunnel only when testing resumes. Alexa dictation remains deferred by the owner.
