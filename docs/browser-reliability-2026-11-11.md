# Browser reliability correction — 2026-11-11 UTC

## Installed changes

The confirmed AFTSSS / sheldonp still runs PS7717.5746N, incremental0036408029828, kernel4.4.162+, API28, ARMv7. This session changes the app and its private engine configuration, not firmware, drivers, display settings, or the host’s streaming setup. Updates retain the original signing identity and owner data.

The protected browser embeds GeckoView159.0.20261102144711 and the unmodified Mozilla-signed uBlock Origin Lite2026.1110.1816. The latest package was obtained from AMO, its SHA256 verified, and corresponding source/submodule commits recorded in `third_party/ublock-origin-lite/SOURCE.md`. It adds no permissions over the earlier pinned Lite release.

### Decoder failure and correction

A controlled 1920×1080 H.264/30fps clip buffered fully in Gecko, but returned `MediaError`3 / `NS_ERROR_DOM_MEDIA_FATAL_ERR`. The same file played in the system engine. The failed Gecko run logged `getSurfaceAllocator` timing out before hardware decoder initialization.

The exact pinned Java source gives the separate GPU-process allocator only100ms to respond on the launcher thread. On this unit that deadline was missed. Configuring `layers.gpu-process.enabled=false` uses the synchronous parent allocator instead. Hardware WebRender/OpenGL compositing remains available, and MediaTek AVC/VP9 hardware decoding was observed. Three fresh protected-browser starts then decoded the same clip without that error.

This removes GPU-process crash isolation; it does not turn off the web content sandbox or hardware acceleration. Fission remains disabled for the tested memory budget, and the extension still runs in a separate process. No website/native command bridge is introduced.

### Protection startup

Extension preparation begins when the foreground Gecko window opens. A redundant add-on-event promise was removed; native enable completion allows opening the extension dashboard, whose initialized response still gates websites. The pinned dashboard hides its body until its background initialization completes, so its first contentful paint is the tested readiness signal.

The initial protection startup has a45s deadline; reenabling protection has a30s deadline. Timeout or initialization failure shows an inline error. Late completions cannot load the requested website after a failure. Web navigation is denied until the protection decision is ready. Explicit OFF remains available and persistent, as requested.

Remote debugging and page-console forwarding default to OFF. A private development preference can enable them only in a debuggable APK. Diagnostic attachment changes the workload and is stated in measurements below.

## Observed results

- Build/lint and standalone URL-boundary checks pass. Current SQLite instrumentation passes create/edit/reorder/delete/reopen checks in its separate test database. Current signed-extension instrumentation checks the exact pinned version and enable/disable preference restoration.
- The updated blocker passes actual OFF/ON request filtering without a debugger: the ordinary script loads in both states; the generic tracking-script fixture executes only when protection is OFF.
- Three protected1080p30 recordings pass the public playback checker: steady intervals of300,481, and300 advancing frames have zero additional drops. The checker rejects the pre-fix decode error and rejects recordings without advancing frames.
- With diagnostic attachment, a public YouTube test video reached148s. A122.6s steady interval produced3683 advancing frames with no additional drops; no ad UI was observed in that recording.
- A subsequent explicit HD request produced actual1920×1080 YouTube video. Over24.4s,1463 new frames included94 drops (approximately6.4%) while diagnostics were attached. Playback continued without decoder errors or observed ad UI. This does not establish perfectly smooth1080p60 playback.
- First cold protected navigation still took approximately26–36s in these checks. This is an outstanding speed limitation on the tested configuration, not a passed fast-start target.

Four non-YouTube owner-saved destinations were opened in the protected engine. Each returned its expected distinct document title, visible page content, and a scrollable document; none matched the tested challenge-page markers. The attempted failed-channel observer produced no usable calibration records, so no per-site blocked-request count is claimed. Player playback, popup suppression, and ad coverage on those destinations remain unverified. The device must be woken before UI tests; activity launch alone did not exit its screensaver. Diagnostic mode was disabled and the library restored after the checks.

The absence of an ad in a test is not proof that every ad format was offered and blocked. Actual request blocking is verified separately. Third-party site changes can require updated filter packages.

## Reproducing a playback check

`tests/fixtures/playback.html` records trusted Play activation, decoder errors, buffering, frame counters, and actual video time without a debugger. `scripts/serve-browser-fixtures.py` serves only loopback static fixtures and receives their telemetry; it supports single HTTP byte ranges. It exposes no shell or native commands. Keep media and metrics private.

```bash
rtk mkdir -p .private/browser-fixtures
rtk cp tests/fixtures/*.html tests/fixtures/*.js .private/browser-fixtures/
rtk ffmpeg -f lavfi -i testsrc2=size=1920x1080:rate=30 -t 60 -c:v libx264 -preset veryfast -pix_fmt yuv420p -b:v 4M -maxrate 5M -bufsize 8M -g 60 -movflags +faststart .private/browser-fixtures/motion30.mp4
rtk proxy python3 scripts/serve-browser-fixtures.py
```

Use the documented private `ANDROID_SERIAL` selection, add the ADB reverse tunnel, open `http://127.0.0.1:8765/playback.html?run=unique-test`, and click Play. After at least20s of advancing playback:

```bash
rtk proxy python3 scripts/check-playback-result.py .private/playback-metrics.jsonl --run unique-test
```

The checker requires advancing video/frame counters and a measured steady interval; zero frames never count as successful zero-drop playback. Its default steady-drop ceiling is5%. Remove temporary servers/tunnels when finished.

Muted plain video can play without Gecko Media Session callbacks. A callback-only UI experiment did not provide reliable generic playback evidence and was removed from the shipped test harness. A shell media-key injection also timed out without a captured app ANR. Neither result was used as proof of a decoder failure or a successful control test.

## Limits

Normal reboot durability,30–60minute playback on personal sites, every source/player format, and release distribution are not established by these checks. The native YouTube app was not the performance subject. Personal destinations, profiles, screenshots, and raw evidence remain under ignored private storage.
