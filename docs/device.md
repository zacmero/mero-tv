# Fire TV Stick: device inventory and experiment scope

**Inspection reported by Zac: 2026-11-09.** This document transcribes the supplied settings-screen and terminal results. “Reported” means evidence from this particular device; “published” means specifications or research about its model. No exploit was executed as part of this inventory.

## Confirmed identity and software

| Item | Reported value |
| --- | --- |
| Build model | `AFTSSS` |
| Device codename | `sheldonp` |
| Fire OS, settings screen | `7.7.1.5 (PS7715/5586)` |
| Fire TV Home | `7300121.PS7715` |
| Build identifier from `getprop` | `PS7715.5586N` |
| Incremental build | `0035736900228` |
| Android security patch property | `2026-03-01` |
| Application ABIs | `armeabi-v7a,armeabi` |
| ADB connection | Working over the local network |

Amazon maps `AFTSSS` to **Fire TV Stick, 3rd generation (2020)**; its Lite counterpart is `AFTSS`. This confirms the identification more reliably than the shared casing model. The release year identifies the product generation, not this unit’s manufacturing date. [1]

The earlier photographs showed these non-unique regulatory markings:

| Label | Value |
| --- | --- |
| Model | `S3L46N` |
| FCC ID | `2AUQI-7436` |
| Anatel certification | `09253-20-01698` |
| Electrical input rating | `5.25 V DC`, maximum `1 A` |

The IP address, full device serial, MAC address, and original photographs are intentionally absent. The input rating is not a measurement of continuous power consumption.

### Kernel and current access

Reported `uname` output:

```text
Linux localhost 4.4.162+ #1 SMP PREEMPT Sun Jul 26 02:20:09 UTC 2026 armv7l
```

`localhost` is the generic hostname shown in this output. The July timestamp is the kernel’s reported build timestamp, distinct from the March security-patch property. Neither value alone establishes whether a particular vulnerability is present.

The active session has UID `2000` (`shell`) and GID `2000` (`shell`). Reported supplementary groups are:

```text
1004(input), 1007(log), 1011(adb), 1015(sdcard_rw),
1028(sdcard_r), 3001(net_bt_admin), 3002(net_bt),
3003(inet), 3006(net_bw_stats), 3009(readproc), 3011(uhid)
```

SELinux context: `u:r:shell:s0`.

This is an ordinary ADB shell, not demonstrated root access. Group membership does not override filesystem permissions or SELinux policy. The context does **not** establish whether SELinux is enforcing; that remains unmeasured. The ABIs and `armv7l` identify the current 32-bit software environment. Start with compatible `armeabi-v7a` Android binaries rather than assuming an `arm64-v8a` build will run.

## Memory and storage snapshot

| `/proc/meminfo` field | Reported value | Approximate MiB |
| --- | ---: | ---: |
| `MemTotal` | `922268 kB` | 900.7 |
| `MemFree` | `51024 kB` | 49.8 |
| `MemAvailable` | `248516 kB` | 242.7 |
| `Buffers` | `4300 kB` | 4.2 |
| `Cached` | `255568 kB` | 249.6 |

Linux reports these fields in units conventionally labelled `kB` but representing 1,024 bytes. `MemAvailable` estimates memory usable by new workloads without swapping, including reclaimable memory; it is not simply `MemFree`. The useful initial estimate is therefore **about 243 MiB available**, not just 50 MiB. Do not add `Cached` to `MemAvailable` again. These values describe one moment and will change with applications and background activity. [4]

The `/data` filesystem report was:

```text
Device:     /dev/block/mmcblk0p13
Mounted on: /data
Size:       4.9G
Used:       2.7G
Available:  2.1G
Use:        56%
```

Keep those displayed units as reported. The 4.9G writable filesystem is not the entire physical flash device; other partitions and filesystem overhead occupy additional space. The approximately 2.1G available is the immediate budget for installed experiments, caches, and data.

## Published hardware baseline

Amazon’s specifications describe this model as follows. These are model-level specifications, not measurements of this unit. [2]

| Component | Published specification |
| --- | --- |
| SoC / CPU | MediaTek MT8695D; four cores up to 1.7 GHz |
| GPU | IMG GE8300; OpenGL ES 3.2 |
| RAM / storage | 1 GB DDR4 / 8 GB |
| Software basis | Fire OS 7; Android 9, API 28; 32-bit app ABI |
| Wi-Fi | Dual-band 2.4/5 GHz, 802.11 a/b/g/n/ac, 2×2 MIMO |
| Bluetooth | 5.0 |
| Video | Up to 1080p60; H.264, HEVC, VP9 hardware decoding |
| HDR / audio | HDR10, HDR10+, HLG; Dolby Digital/Plus and supported Atmos content |

A firsthand review of a `sheldonp` adds Cortex-A53 cores, eMMC storage, and micro-USB 2.0 OTG peripheral support. Those details are supporting model evidence; the exact installed memory part and peripheral compatibility remain unverified here. Use Amazon for the RAM specification because the review contains inconsistent summary-table entries. [3]

Amazon's retail specifications also list HDMI output, a micro-USB power connection, Bluetooth Low Energy, and dimensions of approximately 86 × 30 × 13 mm at 32 g. A reliable supply remains important when testing USB data or additional peripherals. [6]

The matching FCC filing lists internal-board and antenna photographs. It does not provide a verified UART pinout for our unit, and schematic entries exposed only metadata in the consulted listing. [5]

## Candidate uses and remaining unknowns

These are proposed experiments, not completed capabilities:

- **Mero/Suzy frontend:** a lightweight Android or browser interface displaying agent status and playing media, with inference and orchestration on another host.
- **OTG expansion:** test a suitable powered adapter with keyboard, mouse, storage, or Ethernet. Actual accessory and filesystem support must be checked.
- **Telephony integration:** explore a network client for the Mero/Askey telephony system. Audio routing, microphone access, latency, and full-duplex calling are untested; the remote’s Alexa microphone must not be assumed available to arbitrary apps.
- **Remote desktop:** evaluate a compatible Android client as a display/control endpoint; usability and decoding latency need measurement.
- **Later custom ROM:** investigate model-specific recovery and ROM projects after establishing the precise firmware’s compatibility and a recovery plan. Root access alone does not establish generic Linux or complete driver support.

Still unknown: WebView/browser provider and version, real workload performance, hardware revision, peripheral compatibility, SELinux mode, backup coverage, and a tested recovery route. Root, bootloader unlock, preloader downgrade, and ROM installation remain **unexecuted** in this record.

## Sources

1. [Amazon: Identify Fire TV Devices](https://developer.amazon.com/docs/device-specs/identify-fire-tv-devices.html).
2. [Amazon: Fire TV streaming media player specifications](https://developer.amazon.com/docs/device-specs/device-specifications-fire-tv-streaming-media-player.html), section “Fire TV Stick – 3rd Gen (2020).”
3. [AndroidPC: firsthand Fire TV Stick 2020 review](https://androidpc.es/review-fire-tv-stick-2020-opinion/), device reported as `sheldonp`.
4. [Linux kernel documentation: `/proc/meminfo`](https://docs.kernel.org/filesystems/proc.html), `MemFree` and `MemAvailable` definitions.
5. [FCC exhibit listing: 2AUQI-7436 internal photographs](https://fccid.io/2AUQI-7436/Internal-Photos/Internal-Photos-20200629-v1-2AUQI-7436-IntPho-4884500), document 4884500.

6. [Amazon: 3rd-generation Fire TV Stick retail specifications](https://www.amazon.com/dp/B07ZZVX1F2).
