# Mero TV development session — 2026-11-11 UTC

## Scope and device

The owner resumed remote-scrolling and in-app extension work. The connected unit still reports AFTSSS / sheldonp, PS7717.5746N, incremental 0036408029828, kernel 4.4.162+, API 28, and ARMv7. Approximately 922 MiB of physical memory is reported. No firmware, root, GPU-driver, CUDA, or streaming configuration changes were made in this session.

The app was updated in place under its original signing identity. Five saved entries remain; all four requested additions are present. Destinations, keys, profiles, screenshots, and raw diagnostics remain private. The shipped library remains empty on a fresh installation.

## Implementation

- Remote pointer-edge scrolling, page scrolling with fast-forward/rewind, and a direct D-pad Scroll mode are available in both browser surfaces.
- The default protected surface embeds GeckoView 159.0.20261102144711 and the unchanged, Mozilla-signed uBlock Origin Lite 2026.1103.1743 package. Version, hashes, licenses, and corresponding source are under `third_party/`.
- Toolbar and Menu controls persistently enable/disable the extension and open its native dashboard. Disabled protection must be enabled before opening its settings. Changes reload the page with cache bypass and retain website cookies/local storage.
- Full uBlock Origin was evaluated but its tested setup repeatedly lost processes. Lite is the selected implementation, with fewer controls than full uBO. This does not establish that every possible full-uBO configuration is incompatible.
- Add-on `onReady` occurred before Lite finished initializing its rules. The app now waits for the pinned dashboard’s first contentful paint: the dashboard hides its body until its background initialization has completed. A native preparation surface covers this internal page. A 30-second deadline shows an inline failure, with no requested website loaded. Readiness is reused within the running engine and invalidated when the extension restarts or is disabled.
- One web content process, disabled process prelaunch, a 128 MiB JavaScript heap limit, and a 4096 KiB maximum nursery constrain the tested profile. Fission is disabled; ordinary origin checks and the web content sandbox remain. The extension has a separate process. No page/native messaging bridge is introduced.
- The current page URL takes precedence over the original launch URL when restoring Activity state. Media playback requests an awake screen; the remote Play/Pause button controls the active media session.

## Observed checks

Build, lint, and standalone HTTP(S) URL-boundary checks pass. Real-device instrumentation passes SQLite create/edit/reorder/delete/reopen checks using a separate database, and verifies signed-extension startup, disable/enable behavior, and restoration of the prior protection preference.

The controlled fixture’s ordinary script loads in both states. Its generic tracking-script request is blocked when ON, executes when OFF, and is blocked again after reenabling. The same check passes after a cold process start, including an already cached test request. Cookies and local storage survive toggling. The dashboard opens through the native Menu.

The tested profile is Optimal with seven enabled lists: uBlock filters, EasyList, EasyPrivacy, Peter Lowe’s list, badware risks, URLhaus, and AdGuard Mobile. Four registered content-script groups include MAIN-world scriptlets and `<all_urls>` matches, which cover YouTube. A query looking only for literal `youtube.com` in those matches would incorrectly report zero coverage.

A cold protected fixture completed in approximately 26 seconds in the latest measured debug run. This includes engine initialization and local page loading, and is not a startup performance guarantee.

A local 1920×1080 H.264/60 fps fixture uses the MediaTek hardware AVC decoder. Fullscreen and Back work. An initial sample reported 388 dropped frames out of 540; hardware decoding alone does not establish smooth 1080p60 playback in this engine.

A public YouTube video played inside Mero TV with protection enabled, initially at 640×360 through the MediaTek VP9 decoder. No ad UI was observed during this test. This is a playback observation, not proof that every YouTube ad format is blocked. A settled short interval dropped 7 of 405 frames. Another run reached approximately 111 seconds of playback, but remote debugger queries intermittently timed out and frame drops increased. A later screenshot still showed the video and the app remained alive. Earlier exploratory runs lost app processes; a cause was not established. Sustained smooth playback and higher YouTube resolutions remain open acceptance work.

## Remaining acceptance work

Protected-engine D-pad/Select clicking, edge scrolling, fast-forward page scrolling, direct Scroll mode, remote Play/Pause, fullscreen, and Back checks pass. The final H.264 sample still dropped 241 of 432 frames, reinforcing the playback limitation. The library is open with protection enabled; the temporary fixture server and ADB test tunnels are removed. Reboot durability, prolonged playback on personal sites, higher YouTube resolution, daily-use stability, and release signing/distribution remain unverified. Alexa-button dictation stays deferred at the owner’s request. No development changes from this session have been committed or pushed yet.

## Follow-up: custom-browser playback performance

The owner requested correction of YouTube performance inside Mero TV; the native YouTube app was not under test. Hardware WebRender compositing and MediaTek hardware decoding are already active. Made Gecko remote debugging and page-console forwarding opt-in instead of enabling them in every debug build. A longer fullscreen fixture stalled at metadata and showed a black screen; the app was stopped and the native library visibly restored. The test server lacked byte-range responses, so that run is not a valid steady-playback comparison. Corrected the private media server and moved the next benchmark to a short inline player with automatic return to the library. Performance correction and measurement are in progress.

Owner stopped development for the day. The latest parallel-startup build/lint/install completed, and its no-debugger OFF/ON filtering checks passed. Its video remained at metadata with zero advancing frames during the sample; startup improvement and playback correction are not established. Test processes/server/tunnels are stopped, and the native library is observed in the foreground with protection preference ON. Continue from [the new handoff](development-handoff-2026-11-11.md) when the owner resumes.
