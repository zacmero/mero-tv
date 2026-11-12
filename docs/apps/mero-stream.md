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

## Playback controls and resume milestone — 2026-11-12

PlayerView now fills a FrameLayout rather than sharing a vertical layout with a permanent track bar. Subtitle/audio/resolution controls live in the Media3 bottom control area and follow the player's four-second controller visibility timeout. Back first dismisses visible controls; Back with controls hidden releases playback. Track buttons and choice dialogs share the blue native focus style with catalogue/search controls. Native widescreen content uses aspect-preserving Fit without zoom or stretching; 4:3 sources naturally retain side bars.

Continue Watching stores the most recent episode ID, source mode, search/series metadata and playback position in app-private preferences. Position saves every five seconds and on pause/exit. It resolves a fresh stream on resume rather than persisting expiring stream URLs. Completed episodes clear the entry. This is a single most-recent-episode feature, not a multi-title history.

Fire TV launcher assets now have bitmap icon/banner resources on both the application and launcher activity, replacing reliance on vector/layer-list support in the launcher. Reproduce them with `rtk proxy python3 scripts/render-stream-brand.py` (Pillow required only for asset generation). Actual Amazon launcher cache/display behavior remains a visual check, separate from the installed manifest resource declaration.

Acceptance result: build/lint and installed instrumentation passed `widescreen Frieren playback, full-window surface, auto-hide, Back and resume`. The check requires a video aspect ratio of at least 1.7, advancing playback beyond 15 seconds, a video surface matching the entire playback window, controls hidden after interaction timeout, first Back preserving the player while hiding controls, second Back releasing it, saved progress above ten seconds, and Continue restoring that position through a freshly resolved stream. Earlier auto-hide and first-Back assertions failed; explicit interaction timeout, disabling automatic controller re-show and disabling control animations corrected them. No zoom/stretch mode was added. This does not verify every source or long-session stability.

## Subtitle appearance and Last Watched — 2026-11-12

Subtitles → Appearance / font size opens a remote-friendly menu with a sample preview. Four size presets, white/yellow/cyan text, no/translucent/solid black background and outline/shadow/no edge are applied immediately and stored in a separate app-private preference file. Defaults use white text, no background and black outline. Source-supplied styling/font sizes are overridden for soft subtitles; text burned into the video cannot be restyled. The preview uses readable sp sizes independently of its small preview viewport.

Last Watched lists the latest 20 episode/source-mode entries with show, episode, SUB/DUB, completion status or saved minutes. Selecting an entry resolves a new stream and resumes its position. Rewatching an episode updates its existing entry. Raw stream URLs are never stored. `WatchHistory` owns bounded persistence; `SubtitleAppearance` owns caption preferences/preview; both remain independent of provider extraction.

Installed instrumentation passed `widescreen playback, controls, history resume and subtitle appearance persistence`: live playback, saved history entry and selection/resume, font-size button activation and persistence across a new settings instance, and background-control presence. The test restores the previous caption choices. Build/lint passed. This does not establish every caption format or every appearance combination visually.

## Subtitle placement — 2026-11-12

Appearance includes Vertical position: Low (5%), Standard (10%, default), Higher (15%), High (20%), Middle (35%). Percentages are the bottom margin relative to the video subtitle viewport. Text cues are anchored by their lower edge at the chosen height, overriding embedded vertical positions rather than relying only on padding that positioned WebVTT cues can ignore. Source cue data is retained so changes immediately re-render the active text. Bitmap subtitles retain source placement. Settings share the separate caption preference file and the preview updates immediately.
