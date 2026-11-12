# Progress

2026-11-10: Reviewed current changes and private-data exclusions. Created checkpoint commit e5d7e5c. Starting browser reference baseline and build setup.

Implemented native Activity, isolated HTTP(S) URL policy, SQLite site operations, remote pointer/scroll controls, bounded user-requested popups, fullscreen lifecycle and renderer recovery. Added standalone URL security checks and real-device library instrumentation. SDK installation is in progress.

Gradle assembleDebug, assembleDebugAndroidTest, and lintDebug passed. TV Bro v2.1.6 WebView reference launched on the actual unit, initial app PSS 90951 kB; controlled comparison remains pending. Created private local HTTP fixture server and test media; no personal destinations are involved.

Owner confirmed remote point-and-click works on the installed prototype. Owner requests a restrained, elegant cyberpunk identity, especially the Mero TV logo. Visual refinement added to current implementation scope.

Owner refined branding: remove the standalone M emblem, preserve MERO TV lettering, set headline exactly to “The hidden stream gem”. Updated header, launcher banner, and icon (gem/play symbol replaces monogram).

Visual build/lint pass. Local 1080p H.264 clip plays through the MediaTek hardware decoder; corrected fullscreen compositor offset, verified full-display video and Back restoration. Owner requested standalone wordmark and exact headline; implemented and installing final build. Initial native library PSS approximately 23.8 MiB. Documentation records limits; preparing privacy-reviewed browser commit.

Final APK installed and visually inspected: standalone MERO TV wordmark and exact requested headline. Build/lint passed; Gradle wrapper SHA-256 matches official Gradle 8.9 metadata. APK is 98,602 bytes. Temporary fixture server stopped and ADB reverse tunnel removed. Final app left open on its empty library. Publication checkpoint prepared; private artifacts and pre-existing .serena files excluded.

Current task: remote edge/page scrolling, a GeckoView extension-capable protected browser, persistent uBO on/off controls and full filter-list dashboard; add requested bookmarks privately. Alexa dictation deferred.

Gecko protected APK built (ARMv7, ~168 MiB), installed; private bookmark import added four entries and preserved the previous one (five total). On-device signed-extension install + disable/enable checks passed. Initial real request fixture still executed the script despite the UI state, so added Gecko's active-tab registration and diagnostic console logging in debug builds; testing that correction before claiming ad blocking.


Paused at owner request for the day. See [the development handoff](docs/development-handoff-2026-11-10.md) for the exact incomplete extension state, observed checks, private configuration dependencies, and cleanup. No new commit or push for this work.

2026-11-11: Owner resumed development. Device is online with the same confirmed build, kernel, API, and ARMv7 ABI. The latest constrained-profile screenshot still shows the test script loading despite ON; filtering remains unverified for the new readiness logic. Investigating real rule state and cache behavior before changing startup tuning.

2026-11-11: Corrected extension readiness using the pinned dashboard’s post-initialization first paint. Verified actual ON/OFF/ON request filtering, cold startup, native settings, cookie/localStorage preservation, seven enabled lists, and original-key in-place updates. Library and extension instrumentation pass. All five saved entries remain. Gecko pointer clicking, edge/page/direct scrolling pass. YouTube video playback and hardware VP9 decode were observed, with slow startup, debugger timeouts, and dropped frames; sustained smooth HD playback is not established. Added media screen-awake and remote Play/Pause handling; final checks and test cleanup in progress. Details: docs/local-session-2026-11-11.md.

Final installed build passes protected-engine D-pad pointer/Select, edge/page/direct scrolling, remote Play/Pause, fullscreen, and Back. Test sentinels and temporary server/tunnels cleaned up. Library foreground, protection ON. Current development changes remain uncommitted.

Paused at the owner’s explicit request on 2026-11-11. TV library foreground, protection preference ON; test processes, temporary server, and test tunnels removed. Custom-browser YouTube performance remains unfinished. Latest inline run did not advance video time despite successful OFF/ON filtering; do not report zero-frame samples as smooth playback. Current changes remain local and uncommitted. Resume from docs/development-handoff-2026-11-11.md.

2026-11-11 resumed: isolated an actual decode failure before MediaCodec creation. The separate GPU-process surface allocator exceeded Gecko’s 100 ms deadline. Keeping hardware compositing in the parent process removes that handoff; three independent 1080p30 starts now advance without decode errors (one 589-frame sample had zero drops). Same clip succeeds in system WebView. Updating Lite to the current signed 2026.1110.1816 package (AMO hash verified), with no permission additions, before YouTube acceptance. Added bounded protection startup and rejected web navigation until protection is ready.

- Follow-up: four other owner-saved destinations opened with protection enabled, distinct expected titles and scrollable content. Real-site ad/player coverage remains unverified: failed-channel observer did not calibrate. Corrected private QA wake-up omission; diagnostics disabled and library restored. Raw destinations/evidence remain private.
