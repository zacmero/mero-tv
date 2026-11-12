# Browser findings

- Repository initially contains research and a read-only collector; no app implementation.
- Current device: Android API 28, ARMv7; active Amazon WebView 138. See docs/local-session-2026-11-10.md.
- Native Views and platform SQLite can implement the first library without UI/runtime dependencies.
- Bluetooth peripheral placement and deferred Fire Stick remote mouse mapping are recorded in docs/future-experiments.md.

- Official stable TV Bro is v2.1.6. The 6.8 MB Gecko-excluded APK provides a WebView reference without a bundled engine. Upstream license was reviewed at master and the pinned release. No upstream code will be incorporated in the first native prototype.
- Build toolchain selected: JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, compile SDK 35. Google documents the JDK 17 / Gradle 8.9 compatibility for AGP 8.7.
- Java with native Android APIs avoids a Kotlin compiler/plugin dependency for this first implementation; SQLite replaces the optional Room layer.

## Extension browser work
- Owner explicitly requires uBlock Origin inside Mero TV, including YouTube.
- Chosen GeckoView stable 159.0.20261102144711; uBO AMO 1.76.0 requires Firefox/Android >=115 and has a Mozilla-signed XPI. Its SHA-256 was verified against AMO metadata.
- GeckoView dependencies require compile SDK 36; upgrading build tooling to AGP 8.13.2 / Gradle 8.13 while retaining target SDK 35 and device min SDK 28. No device OS changes.
- Owner's four requested destinations are stored only in .private/requested-sites.json.

2026-11-11: Mozilla bug 1949623 documents DNR interactions with the in-memory resource cache. Cache reuse is a hypothesis for the enable/reload test failure, not an established cause. Source: https://bugzilla.mozilla.org/show_bug.cgi?id=1949623. Controlled fixture responses now use no-store during diagnosis.

Resume evidence: fresh control-page requests were blocked with Lite enabled; settings showed Optimal mode. In a document-generation-aware OFF/ON test, OFF executed the test script and ON still executed it after the new document completed. A forced cache-bypassing reload blocked it, and a fresh nonce request was also blocked. The app now bypasses cache when protection changes. The tested memory profile is being made app-private and durable; debug transport is available only in debuggable builds.

Further cache evidence: the toggle cycle preserves cookies and DOM storage and blocks requests after re-enable. A cached fixture still loaded its old allowed script after a process restart; a never-cached nonce request in the same cold-start test was blocked. The first protected navigation therefore also bypasses cache, while network/image cache invalidation handles transitions from OFF. These operations exclude cookies, DOM storage, auth sessions, and permissions.

Readiness correction: the cache-only hypothesis did not explain cold-start failures. In the pinned Lite source, `background.js` starts initialization asynchronously; `getOptionsPageData` awaits `isFullyInitialized`. `dashboard.html` begins with `body.loading`, and `css/settings.css` keeps that body hidden until `settings.js` receives the initialized response. Mero now loads that trusted page behind a native preparation overlay and waits for its first contentful paint before loading a website. The bulk cache-clearing workaround was removed; cache-bypassing page refresh remains. This version-specific readiness gate is under real-device validation.

2026-11-11 resumed: controlled comparison with the same 1080p30 H.264 file gives Gecko a decode error (MediaError 3, full buffered range) while the system engine advances normally. Gecko logs show getSurfaceAllocator TimeoutException before any MediaCodec initialization. Pinned GeckoProcessManager.java calls the GPU process via the launcher thread and polls for only 100 ms; without a separate GPU process it returns the parent allocator synchronously. Testing the supported parent compositor configuration targets this observed failure without disabling hardware decoding or the web content sandbox. Upstream related report: https://github.com/mozilla/geckoview/issues/192 (older versions; not proof of the exact same cause).

Fixed-build evidence: three independent protected starts play 1920×1080 H.264 with zero surface-allocator failures; recorded steady intervals have zero additional frame drops. The public checker passes those recordings and rejects the actual pre-fix decode error. Updated Lite 2026.1110.1816 passes no-debugger OFF/ON filtering. A YouTube test reached 148 seconds; a 122.6-second interval rendered 3683 new frames without another drop or observed ad UI. Hardware WebRender remains available and MediaTek VP9 is used; no separate GPU process is active. The later ADB input keyevent command timed out; no app ANR or decoder timeout was captured. Distinguish shell injection overhead/transport failure from an app media-key defect before changing control code.

Media-test correction: muted controlled video advances with zero drops but emits no Gecko MediaSession play callbacks in the instrumentation observer. A failed callback wait therefore does not prove video or remote-key failure. Testing the actual audible Media Session site separately; keep generic frame telemetry as the authority for plain/muted video.

- Follow-up: four other owner-saved destinations opened with protection enabled, distinct expected titles and scrollable content. Real-site ad/player coverage remains unverified: failed-channel observer did not calibrate. Corrected private QA wake-up omission; diagnostics disabled and library restored. Raw destinations/evidence remain private.
