# mero-stream

## Scope and architecture

Separate Android application, API 28 minimum, native Android Views. No browser engine or shell runtime. `MainActivity` owns search/episode navigation and remote focus. `Provider` owns bounded HTTPS requests and ani-cli-derived stream resolution. `StreamPlayer` owns Media3 ExoPlayer, HLS, lifecycle and track menus. One player is released on Back/destruction; background playback pauses. Movie providers are deferred.

## Source and licensing

The inspected ani-cli version searches **HiAnime (hianime.at)**. Its implemented resolver selects **ZokoAnime**; the provider also advertises HD-1 but this app does not implement that resolver. This is not an AniWatch plugin. The catalogue is HTML and JSON extraction, not a guaranteed public API. Changes upstream can break it. See [pinned attribution](../../stream/README.md) and GPL-3.0 license in the module.

Media3 1.11.1, Apache-2.0, is pinned for ExoPlayer/HLS/UI. Track menus enumerate supported tracks from the prepared source: subtitles can be disabled, audio selected, video quality selected or automatic. Sidecar WebVTT subtitles are supplied from provider configuration. SUB/DUB source choice is separate from audio tracks within a stream. A source with one rendition/audio track cannot offer alternatives that do not exist.

## Milestones

- **2026-11-12 / initial prototype:** native search, episode parsing and platform VideoView built and installed. End-to-end playback was not verified; owner reported broken playback and clipped/weakly focused rows. This was not an accepted release.
- **2026-11-12 / focus correction:** density-aware row padding, content-sized multi-line titles, blue focused/pressed background and border replace fixed 64-pixel rows.
- **2026-11-12 / player correction:** Media3 replaces VideoView, preserves HLS Referer on segment requests, attaches provider subtitles, provides remote-accessible audio/subtitle/resolution menus and SUB/DUB source choice.
- **Acceptance:** see on-device result recorded below. Build/lint alone do not establish playback or long-session stability.

## Build and test

Use the private signing/build environment documented for this checkout. Run `rtk ./gradlew :stream:assembleDebug :stream:assembleDebugAndroidTest :stream:lintDebug`. Install both stream APKs using the private ADB target, wake the device, and run instrumentation `dev.mero.stream.test/dev.mero.stream.StreamChecks`. This performs live search, selects an episode and requires more than 15 seconds of advancing player time, reporting actual video height and track group count. It does not prove every anime/server works or long-session stability. No raw stream URLs should appear in public logs.

## On-device acceptance — 2026-11-12

Media3 build/lint passed. The installed live-provider instrumentation returned `PASS: search -> episode -> advancing native playback`; the assertion requires player position greater than 15 seconds after native search and episode selection. This replaces the earlier unverified VideoView prototype. Test APK installation initially timed out and was retried after stopping the browser. No numeric video-height/track-count fields were present in captured output, so no resolution claim is made. Each track-menu choice, visual focus appearance and long playback still need additional acceptance; track menus are implemented, not all combinations demonstrated.

- **Branding:** code-native blue play-chevron/cyan cursor vector used for launcher icon, Fire TV banner and native home header. No raster assets or additional runtime dependency.
