# Local exploration: 2026-11-10 UTC

This session connected directly to the owner's TV over authorized network ADB. Raw evidence, endpoints, app inventory, downloaded APK, release metadata, and logs remain under ignored `.private/`. No exploit, firmware write, reboot, or OTA configuration change was performed.

## Observed device baseline

| Field | Observed value |
| --- | --- |
| Model / device | AFTSSS / sheldonp |
| Fire OS | 7.7.1.7 (PS7717/5746) |
| Build | PS7717.5746N |
| Incremental | 0036408029828 |
| Android API / ABIs | 28 / armeabi-v7a,armeabi |
| Kernel | 4.4.162+, armv7l; build timestamp Oct 28 02:11:13 UTC 2026 |
| Advertised security patch | 2026-03-01 |
| SELinux | Enforcing |
| Active WebView | com.amazon.webview.chromium |
| WebView version | 138.amazon-webview-v138-7204-tv.7204.244.304 |
| WebView version code / target SDK | 61996032 / 35 |
| Available memory before install | 232344 kB, approximately 227 MiB |
| Free /data space | 2.5G as reported by df |
| Display mode | 1920 × 1080 at 59.94 Hz |

The firmware differs from the previous owner-reported baseline. The PS7715 GhostLock profile discussion is historical; compatibility with this new build has not been evaluated. The cause and timing of the firmware change were not observed. Display mode does not establish streamed video resolution.

## Moonlight client experiment

Installed the unmodified official **Moonlight Android v12.2**, non-root APK, from its [upstream release](https://github.com/moonlight-stream/moonlight-android/releases/tag/v12.2). SHA-256 matched the GitHub release asset digest:

```text
a871f6190365386ef3ac5722a016d4da015ee1555631ec257f6e5dc3f4a45fe0
```

The downloaded APK contains armeabi-v7a libraries. Package Manager confirms `com.limelight`, versionName `12.2`, versionCode `315`, minimum SDK `21`, target SDK `36`. Installation returned Success. Starting `com.limelight/.PcView` returned Status: ok; the activity was subsequently observed resumed with a running process. Its idle memory snapshot was 42600 kB PSS, approximately 42 MiB. This is not a streaming benchmark.

At this initial checkpoint, pairing and streaming had not yet been attempted. Later results below supersede that state. No other TV applications were installed during the streaming session.

## Computer hosting assessment

The local computer runs EndeavourOS with Hyprland on Wayland, an NVIDIA GTX 1060 6GB, and NVIDIA driver 580.178.04. At this initial checkpoint, Sunshine was not installed and its user service was inactive.

The [official setup guide](https://github.com/moonlight-stream/moonlight-docs/wiki/Setup-Guide) pairs Moonlight clients with a Sunshine host. The latest Sunshine release observed was `v2026.914.233613`. Its [pinned installation documentation](https://github.com/LizardByte/Sunshine/blob/v2026.914.233613/docs/getting_started.md) lists CUDA 13.1.1 and minimum driver 590.48.01 for supplied binaries. The installed driver is below that requirement for the bundled CUDA path; this does not establish that all capture/encoding paths fail. At this checkpoint no Sunshine package or driver had been installed or changed.

The initial next step was to select and test a host build/encoding path compatible with the installed driver and Wayland capture. Then pair locally and measure a conservative H.264 desktop stream before testing 1080p60. Verify audio and input separately. Keep pairing credentials, host configuration, and stream diagnostics private.

## Resumed session: working desktop stream

Installed the official Sunshine `v2025.924.154138` AppImage in the owner's local application directory, extracted for user-service execution. SHA-256 matched the release asset digest:

```text
83921dd4e09fd517007150bc6a1a93b63db22a56f7da99025a3d7765c24b75fa
```

The executable reports commit `86188d47a7463b0f73b35de18a628353adeaa20e`. Its [pinned compatibility documentation](https://github.com/LizardByte/Sunshine/blob/v2025.924.154138/docs/getting_started.md) specifies bundled CUDA 12.9.1 and minimum NVIDIA driver 575.57.08. The existing 580.178.04 driver was retained. NVIDIA's [architecture matrix](https://docs.nvidia.com/datacenter/tesla/drivers/cuda-toolkit-driver-and-architecture-matrix.html) lists CUDA 12.x and driver R580 as the final branches for Pascal. The system also has CUDA Toolkit 13.3.1 installed; Sunshine uses its bundled compatible runtime and does not require that toolkit. No toolkit or driver was changed.

Sunshine uses `wlr` capture and `nvenc`, selecting monitor 0 (1920 × 1080). H.264 and HEVC encoder probes succeeded; unsupported AV1 probes failed as expected. The configured desktop stream advertises H.264 only. The `mero-sunshine.service` user unit is enabled under the graphical session target. Actual startup after a new login remains untested.

The initial network failure was firewalld blocking inbound traffic. With the owner's supplied sudo authorization, installed runtime and permanent rich rules limited to this TV's private source address: TCP 47984, 47989, 48010; UDP 47998–48000, 48002, 48010. The admin UI remains limited to the local computer; UPnP is disabled. Rules must be reviewed if the TV's address changes. No broad public-zone port opening was used. Discovery remains unavailable through Avahi, so the computer was added by private LAN address.

Installed a udev `uaccess` rule for `/dev/uinput` and granted the current user access for this boot. Keyboard and mouse initialization no longer reported access failures; a DS5 gamepad access warning remains. Persistence of permissions after reboot and gamepad functionality remain untested.

Created Sunshine admin credentials privately, paired Moonlight using its displayed PIN, and launched Desktop. Observed host logs: `CLIENT CONNECTED`, `h264_nvenc` encoder creation, approximately 7.3 Mb/s configured streaming bitrate, and Opus 48 kHz stereo initialization. The TV's active activity is `com.limelight/.Game`; client logs identify `OMX.MTK.VIDEO.DECODER.AVC` and AudioTrack. These verify stream setup, not subjective audio/video quality, measured latency, dropped frames, or input operation. Long-duration stability remains untested. No firmware update, driver update, exploit, or reboot was performed.

Credentials, pairing state, endpoints, configuration, and logs remain private. Stop the host with `rtk systemctl --user stop mero-sunshine`; restart with `rtk systemctl --user restart mero-sunshine`. The user service uses the pinned local application directory and the ignored private configuration in this checkout.

### Image-quality adjustment

The first stream negotiated 1280 × 720 at 60 fps. After the owner requested better image quality, selected 1080p in Moonlight; its bitrate dialog reported 20.0 Mb/s. Reconnected Desktop. Client logs now confirm `video/avc`, width 1920, height 1080, frame-rate 60, and hardware decoder output at 1920 × 1080. Sunshine logs report `CLIENT CONNECTED`, approximately 15 Mb/s encoder streaming bitrate (distinct from the client's configured 20 Mb/s), and stereo Opus initialization. Sustained frame delivery and subjective image quality remain to be assessed.

### Initial network latency sample

While streaming, 15 ICMP probes from the TV to the host returned 0% packet loss, RTT minimum/average/maximum 1.806/2.844/11.589 ms. This short sample does not establish sustained UDP performance or end-to-end input-to-display latency. Host Ethernet reports 1000 Mb/s full duplex. TV Wi-Fi reports 5500 MHz, RSSI -53 dBm, negotiated link speed 702 Mb/s; link speed is not measured throughput. Audio-system latency estimates in raw logs are not video or input-to-display measurements. No stream settings were changed during this check.

### Lowest-latency pacing and performance overlay test

Directly inspected Moonlight's frame-pacing dialog: **Prefer lowest latency was already selected**. No pacing mode change was needed. Enabled both Show performance stats while streaming and Show latency message after streaming, then reconnected the 1080p60 H.264 Desktop stream with the existing 20 Mb/s client bitrate setting.

Four sampled overlay captures spanned approximately 68 seconds of ordinary desktop use. Observed received/rendered FPS pairs were 60.07/60.07, 60.05/60.05, 60.46/60.46, and 60.05/60.05. Each sample reported 0.00% network frame drops, 1 ms estimated network latency, and 0–1 ms reported variance. These are sampled rolling statistics, not continuous cumulative drop counters. Matching rendered/received rates do not prove every frame was physically displayed by the television.

All samples identify OMX.MTK.VIDEO.DECODER.AVC and report 0.00 ms average decoding time. This value is not accepted as a physical decode-time measurement. The client source accumulates timestamp deltas only when nonnegative and below one second; the precise reason this unit reports zero has not been established. Android media.metrics did not expose a newly attributable current-session AVC timing record after stream close. Therefore hardware decoding is confirmed, but decode time and input-to-photon delay remain unmeasured. No reliable improvement from changing pacing can be claimed because minimum-latency pacing was already active.

Restarted Desktop after collecting metrics. The performance overlay and latency message remain enabled for further observation. This was a desktop workload; no controlled gaming or high-motion stress benchmark was performed. Raw captures and codec records remain private.

## First custom browser build

Implemented and installed `dev.mero.tv` version 0.1.0 as an ordinary non-root Android development APK. Native Views supply the library and browser controls; SQLite stores sites; the installed Amazon WebView supplies page rendering. No third-party browser source or bundled engine was incorporated. The owner confirmed remote point-and-click. The latest branding uses a standalone MERO TV wordmark (no separate M emblem), a faceted gem/play launcher icon, and the requested headline “The hidden stream gem”. Native vectors and static canvas accents add no UI runtime dependency.

Gradle 8.9 / AGP 8.7.3 / JDK 17 builds and Android lint pass. Standalone URL-boundary checks pass. On-device instrumentation passes empty library, create, edit, transactional reorder, boundary moves, database close/reopen persistence, and deletion, using a distinct `library-checks.db`. The initial test harness incorrectly tried test-package storage under the target UID; this was corrected without modifying the owner library.

The app's initial native-library memory snapshot was 24379 kB PSS (approximately 23.8 MiB) plus 3605 kB swap PSS. This is one app-process snapshot before opening a page, not total browsing memory. An early startup observation reported 947 ms TotalTime; builds and startup conditions changed later, so this is not a controlled performance benchmark.

Controlled static pages were served over a temporary ADB reverse tunnel to a host loopback HTTP server. A locally generated 1920 × 1080, 60 fps H.264 test clip played after a remote Select gesture; client codec logs reported OMX.MTK.VIDEO.DECODER.AVC and 1920 × 1080 output. Fullscreen initially retained the browser chrome's vertical offset. Keeping the WebView at fullscreen origin while hiding only the native toolbar/label corrected the observed video bounds; the refined screenshot covers the full 1920 × 1080 display. Back restored native browser controls. These observations do not measure sustained delivery, dropped frames, audio quality, or personal-site playback compatibility.

Installed the official TV Bro v2.1.6 Gecko-excluded reference APK after reviewing its current and pinned license. Its SHA-256 is `d8634edfe8d94b4fb9a52005d68a090a7f1e85b7f39c2cf01a25dc9dd60942b2`; the download metadata's digest was checked when supplied. Package `com.phlox.tvwebbrowser` reported version 2.1.6 and was observed resumed. Initial app PSS was 90951 kB plus 944 kB swap PSS. It had a different startup workload, so this does not establish a controlled performance advantage for either browser. A matched WebView/Gecko comparison remains pending.

The public app has no bundled destinations. Local test media, raw screenshots, APKs, signing material, and logs remain private. Build/run instructions and unverified milestones are in [browser-build.md](browser-build.md).
