# Mero TV browser and bookmark library

Research date: **2026-11-09 (America/Sao_Paulo)**. **Update 2026-11-10:** first native implementation installed and tested in part; see [build/run guide](browser-build.md) and [session evidence](local-session-2026-11-10.md). The historical planning baseline below is preserved.

## Goal and device budget

Build a reliable TV browser with an attractive bookmark library: add, edit, delete, reorder, save the current page, and open user-entered URLs. The use case includes the user's chosen anime and film sites; keep the personal destination list private. No destinations are bundled and no site compatibility has been verified. Do not hardcode service domains or visit guessed domains.

Reported device: **AFTSSS / sheldonp**, Fire OS **7.7.1.5**, build **PS7715.5586N**, incremental **0035736900228**, kernel **4.4.162+**, ARMv7 application environment. RAM snapshot: `MemTotal=922268 kB`, `MemAvailable=248516 kB`; `/data` has **2.1G free as reported by `df`**. Available memory varies; it is not a guaranteed application allowance.

**Normal installed-app persistence is the requirement.** Bookmarks/settings must survive process death and reboot without GhostLock, root, ADB scripts, or a background daemon. Launching the app after reboot is distinct from automatically opening it or replacing Amazon HOME. HOME replacement is a separate experiment and no prerequisite.

## Recommended architecture and engine decision

Start with **Kotlin, native Android Views, a focusable bookmark grid, SQLite/Room storage, and one active WebView**. This avoids an additional JavaScript UI runtime and a bundled browser engine. This is a design choice to validate on the device, not a measured performance claim. Store inactive-page URLs rather than retaining many live WebViews.

Amazon documents **Amazon WebView (AWV)** as a Chromium-based replacement behind the standard `android.webkit.WebView` API on Fire OS 5+, including Fire TV. This does not establish the provider package or Chromium version installed on this stick. [S1]

| Option | Decision |
| --- | --- |
| Installed WebView/AWV | First prototype: small APK, vendor integration; compatibility/security depend on the installed provider. Do not assume installing Google WebView changes the provider: the OS controls eligible packages. [S2] |
| TV Bro adaptation | Benchmark before writing browser plumbing. It already supports remote navigation and bookmarks. Observed releases: **2.1.6 Latest**, **3.0.4 Pre-release**; 3.x separates the Gecko variant and adds bookmark import/export. Pin a tested tag/commit. [S3] |
| Bundled GeckoView | Independent Firefox engine and update cadence; adds APK/native-engine footprint. Android 9 exceeds the documented API 26 baseline, and TV Bro publishes ARMv7 builds. Confirm the exact chosen AAR's ABI/minimum SDK and measure RAM, decoding, and stability; do not infer suitability from installation alone. [S4] |

TV Bro uses a **custom license**. Retain source notices; redistributed modified binaries need a different name without “TV Bro”, different icon/application ID, and an in-app About attribution linking upstream. Preserve dependency notices. GeckoView includes MPL-2.0-covered code and third-party components; distributing it requires the applicable notices/source availability, including changes to covered files. Using the installed WebView API does not bundle a replacement Chromium engine. [S5]

## First local probe: identify the actual WebView

After authorized ADB connection, run these read-only queries:

```bash
adb shell dumpsys webviewupdate
adb shell pm list packages webview
adb shell pm list packages chromium
```

Take the provider package from the first output, then substitute it for `PACKAGE`:

```bash
adb shell dumpsys package PACKAGE | rg 'versionName=|versionCode=|minSdk=|targetSdk='
```

Record only provider name/version, OS/build, and relevant support results. If Amazon's service differs, a tiny probe can call `WebView.getCurrentWebViewPackage()` after initialization. Do not switch, disable, or replace providers. Do not commit whole package/system dumps. Chromium documents `dumpsys webviewupdate` for identifying the selected provider. [S2]

## Minimum app behavior

- **Library:** calm dark UI, readable large cards, unmistakable focus, deterministic D-pad movement. Add/edit URL and title with Android's keyboard; delete with confirmation; reorder and save current page. Persist each accepted edit transactionally in app-private storage.
- **Browsing:** address bar, back/forward, reload/stop, save bookmark, and return-to-library action. Support arbitrary user-entered HTTP(S) URLs; prefer HTTPS, visibly identify HTTP, and require confirmation for external-app intents. Reject executable/local schemes in the address field.
- **Remote:** native focus navigation for chrome; selectable cursor mode for mouse-oriented pages, adjustable speed, center-click, and scrolling. Keep Menu accessible even when the site captures focus. Restore focus after dialogs and full-screen exit.
- **Back order:** close overlay/dialog, exit full-screen, navigate page history, then return to library. The system HOME button retains its system behavior.
- **Media:** implement both `onShowCustomView` and `onHideCustomView`; manage focus/audio lifecycle and screen wakefulness only during playback. Require a user gesture initially; expose per-site autoplay preferences only after testing. [S6]
- **Windows:** block unsolicited popups. Explicitly handle user-requested new windows with visible destination/context and a bounded lifetime; test sites that depend on them. Blocking every popup can break legitimate navigation. [S6]

## Playback, durability, and privacy

Amazon specifies hardware-assisted H.264, HEVC, and VP9 decoding up to **1080p60** for this model. That is a hardware ceiling, not a promise that every website provides or plays 1080p: WebView version, codec profile, MSE/DRM support, player implementation, stream selection, and network conditions matter. Keep hardware acceleration enabled and verify the actual decoder/playback behavior. A browser cannot manufacture a missing 1080p source. [S7]

Enable JavaScript/DOM storage for modern sites, but **never expose a JavaScript-to-native bridge to arbitrary pages**. Keep bookmark operations in native UI. Preserve TLS error handling; do not add a universal certificate-error bypass. Restrict file access and grant website permissions individually. [S8]

Cookies, history, browser profiles, exports, signing keys, and diagnostic logs stay private and git-ignored. Do not log URL query strings or tokens. Export bookmarks only on explicit action; exclude cookies/credentials and warn that URLs themselves may contain secrets. Keep the signing key stable so APK updates preserve app data. Recover to the library after renderer crashes; no endless automatic page reload.

## Phased delivery and acceptance

1. **Baseline:** identify WebView; compare official TV Bro WebView and Gecko builds on this TV using one controlled page/video and Zac's chosen URLs locally. Record exact versions and results, without publishing browsing data.
2. **Small prototype:** native library CRUD, one browser surface, remote cursor, full-screen and Back behavior. Choose clean implementation or licensed adaptation from measured results.
3. **Durability:** verify edits after force-stop, process recreation, device reboot, and same-signature APK update; confirm normal operation with no exploit session or ADB attached.
4. **Playback/stability:** test 30–60 minutes, seeking, pause/resume, repeated full-screen transitions, lost network, popups, and memory pressure. Measure cold start, total app/renderer memory and playback stalls on the actual stick before optimizing. Add import/export and optional engine alternatives only after the baseline passes.

## Primary sources

All reviewed for this handoff on **2026-11-09**; dynamic release/provider details must be rechecked before implementation.

- **S1:** [Amazon AWV architecture](https://developer.amazon.com/docs/fire-tablets/hybrid-apps-overview.html).
- **S2:** Chromium [provider restrictions](https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/webview-providers.md) and [provider diagnostics](https://chromium.googlesource.com/chromium/src/+/refs/heads/main/android_webview/docs/quick-start.md).
- **S3:** TV Bro [repository](https://github.com/truefedex/tv-bro) and [releases](https://github.com/truefedex/tv-bro/releases).
- **S4:** Mozilla [GeckoView architecture](https://firefox-source-docs.mozilla.org/mobile/android/geckoview/index.html), [minimum SDK change](https://bugzilla.mozilla.org/show_bug.cgi?id=1982312), and TV Bro [published ARMv7/minimum-API metadata](https://github.com/truefedex/tv-bro/blob/master/latest_version.json).
- **S5:** [TV Bro license](https://github.com/truefedex/tv-bro/blob/master/LICENSE.md), [GeckoView licensing discussion](https://github.com/mozilla/geckoview/issues/110), [Mozilla MPL FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).
- **S6:** Android [WebChromeClient](https://developer.android.com/reference/android/webkit/WebChromeClient) and [WebSettings](https://developer.android.com/reference/android/webkit/WebSettings).
- **S7:** [Amazon streaming-device specifications](https://developer.amazon.com/docs/fire-tv/device-specifications-fire-tv-streaming-media-player.html) — select Fire TV Stick, third generation, 2020 / AFTSSS.
- **S8:** Android [WebView native-bridge risks](https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges).
