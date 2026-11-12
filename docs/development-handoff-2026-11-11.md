# Development paused — 2026-11-11 UTC

The owner explicitly stopped development for today and plans to resume tomorrow. No further development or device tests should run until resumed.

## Device and cleanup

Mero TV’s native library is observed in the foreground. The stored protection preference is ON. All five saved entries remain, including the four requested additions. Temporary fixture/performance servers, active test scripts, ADB reverse tcp:8765, and debugger forward tcp:6081 were stopped/removed. No firmware, display setting, root, driver, CUDA, or host-streaming configuration changes were made. The native YouTube app was not the subject of performance testing.

The screen went black during a fullscreen local video test that stalled before displaying frames. It was stopped and the library visibly restored. Keep subsequent tests inline with visible controls and automatic library restoration; announce on-TV tests before running them.

## Installed implementation and passing evidence

The current development APK uses GeckoView 159.0.20261102144711 and signed uBlock Origin Lite 2026.1103.1743. It is updated in place under the original signing identity. The system WebView remains selectable. The site library is empty on a new install; personal destinations are not bundled.

Build/lint, URL-boundary checks, SQLite instrumentation, and extension lifecycle instrumentation passed. Actual ON/OFF/ON request filtering, cold-start filtering, native settings access, and cookie/local-storage preservation passed. The final installed build also passed normal-use filtering OFF then ON without attaching a debugger. Seven active lists and all-site MAIN-world scriptlets were observed in the tested Optimal profile.

Protected-engine D-pad pointer/Select clicking, edge scrolling, fast-forward page scrolling, direct Scroll mode, remote Play/Pause, local fullscreen, and Back checks passed. Local hardware AVC and YouTube hardware VP9 decoding were observed. These do not establish smooth sustained HD playback.

## Active unfinished work: custom-browser YouTube performance

The owner clarified that slow YouTube inside Mero TV should be corrected. Hardware WebRender compositing is already enabled; forcing graphics preferences is unsupported by current evidence.

Implemented and installed two changes:

1. Gecko remote debugging and page-console forwarding are now opt-in through the private `development` preference `engineDiagnostics=true`, honored only for debuggable APKs. Ordinary use defaults to OFF.
2. Extension preparation starts immediately after opening the foreground Gecko window. It no longer waits for a throwaway startup page to paint. The pinned extension dashboard’s first contentful paint still gates loading the requested website; its body is hidden until background initialization finishes. Readiness is cached within the running engine and invalidated on extension restart/disable.

Performance remains unresolved. One corrected, debugger-detached inline 1920×1080/30 fps run rendered 451 additional frames with zero additional drops over 15 seconds after startup. It had initial drops and roughly 35 seconds to fixture metadata readiness after the Activity launch command. The latest run with parallel extension startup still took roughly 33 seconds to metadata readiness and never advanced video time during the sample. Its zero frames must NOT be reported as zero-drop playback success. Investigate whether the Play action was delivered, media buffering/requests, and initialization timings before attributing a cause. Filtering still passed in that run.

Earlier debugger-attached 1080p60 fixtures dropped many frames; YouTube initially played at 640×360, with no ad UI seen in one public-video test. Debugger queries timed out during longer runs. One collected later run kept the app alive and showed video in a screenshot; earlier exploratory runs lost processes without an established cause. Do not claim all YouTube ads are blocked or that normal sustained HD playback is verified.

## Private tooling and files

- `.private/build-env.json` contains the correct JDK17/Gradle/SDK/signing environment. Use it for every build; do not generate a replacement signing identity or uninstall/clear owner data.
- `.private/update_apk.py --library` builds/lints, verifies both APK certificates against the installed identity, installs in place, and returns to the library. Without `--library` it opens a fixture.
- The signing backup is private; never print or commit keys, credentials, endpoints, profiles, screenshots, or raw logs. The original signing store remains untouched.
- `.private/adb-target` contains the authorized endpoint. Use the existing ADB helper and suppress exception command lines that could print it.
- `.private/perf-server.py` serves private fixtures and receives unprivileged page metrics. It now supports byte-range responses; Python’s simple static handler did not. It is stopped.
- `.private/perf_inline.py` runs a short inline fixture, tests OFF/ON filtering, and restores the library in `finally`. The latest raw metrics are `.private/last-inline-performance.json`; earlier successful steady-playback metrics are `.private/gecko-no-diagnostics-range-30.json`.
- RDP helpers and older native QA require explicitly enabling engine diagnostics before restart. Attaching the debugger changes the workload and must be stated in performance measurements.
- An unused untracked `app/src/main/assets/engine-start.html` remains after removing its code path. Remove it during resumed cleanup, then rebuild; do not treat it as a functional requirement.

## Repository state

All current extension/performance changes are uncommitted and unpushed. Preserve the pre-existing untracked `.serena/` directory. A reviewed scan of the public changed/new files found no private destination or endpoint values. `git diff --check` passed before the final handoff edits. No force operations are needed.

Read this record, `docs/local-session-2026-11-11.md`, and `docs/browser-build.md` before resuming. Alexa-button dictation remains deferred. Reboot durability, prolonged personal-site playback, higher YouTube resolution, and release distribution are still open.
