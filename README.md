# Mero TV

Fire TV hardware archaeology, a documented root/unlock research track, and a lightweight browser with a personal website library for Zac's TV.

**Status — 2026-11-10:** a first native Mero TV browser APK is implemented and installed on the confirmed **AFTSSS / sheldonp**, Android API 28. The site library starts empty. Build/lint and on-device library checks pass; the owner confirmed remote point-and-click. Local video playback is under validation; personal-site compatibility and reboot durability remain unverified. The current observed build is **PS7717.5746N**. No exploit or firmware write has been performed.

Moonlight 12.2 and Sunshine with CUDA 12.9.1 also provide a working 1080p60 desktop stream. See the [session evidence](docs/local-session-2026-11-10.md) and [browser build/run guide](docs/browser-build.md).

## Start here

1. [Local-agent handoff](HANDOFF.md): current state, next steps, and completion criteria.
2. [Device record](docs/device.md): exact owner-reported inventory and sourced specifications.
3. [Archaeology workflow](docs/archaeology.md): collect the next facts without changing device software.
4. [Exploit paths](docs/exploit-paths.md): GhostLock compatibility evidence, Kamakiri, persistence, and recovery considerations.
5. [Browser plan](docs/browser-plan.md): editable site library, remote navigation, playback, and implementation choices.
6. [Privacy and local data](docs/privacy.md): keep network details, credentials, browser data, and raw evidence out of Git.
7. [Further experiments](docs/future-experiments.md): custom ROMs, terminal tools, agent displays, and remote PC access.
8. [Agent conventions](AGENTS.md): evidence and workflow rules for contributors.

## Historical owner-reported baseline (2026-11-09)

| Field | Owner-reported value |
|---|---|
| Device | AFTSSS / sheldonp |
| Fire OS | 7.7.1.5; UI build PS7715/5586 |
| Android build | PS7715.5586N |
| Incremental | 0035736900228 |
| Kernel / machine | 4.4.162+ / armv7l |
| App ABI | armeabi-v7a,armeabi |
| Current shell | UID 2000 |
| Available RAM in this snapshot | Approximately 243 MiB |
| Available /data storage | 2.1G as reported by df |

These are Zac's inspection results, not measurements taken by the remote research agent. [Full record and interpretation](docs/device.md).

## Two goals with independent progress

### Hardware control

Begin with archaeology. The historical direct Kamakiri route requires Fire OS below 7.2.7.3. The previously inspected GhostLock source contains a profile matching the older PS7715 baseline, marked **derived-untested**; compatibility with the currently observed PS7717 build has not been evaluated. Treat the unit as a candidate for local validation, not a confirmed successful unlock. A published reproduction on a different build shows a possible temporary-root → controlled preloader change → Kamakiri → TWRP route. [Evidence and exact sources](docs/exploit-paths.md).

### Everyday browser

Build or adapt an ordinary Android TV-style app that starts without root and persists the user's site library across reboots. Website addresses are user-entered data. The initial library is empty. Prioritize remote navigation, a fast native menu, one active web page, fullscreen playback, and measured 1080p compatibility.

Replacing the Amazon system home screen or making the HOME button always open this app is a separate firmware-dependent task. It must not become a prerequisite for ordinary browsing. [Design and acceptance criteria](docs/browser-plan.md).

## Local collection

From the local workstation that already has ADB access, choose the device privately:

```bash
read -r -p 'ADB target (local IP:port or USB serial): ' ANDROID_SERIAL
export ANDROID_SERIAL
bash scripts/collect-device.sh
```

The script requires an explicit target, checks the model/codename, and saves reports only in ignored `.private/recon/` directories. It does not connect to guessed targets, reboot, install apps, request root, alter packages, or write device partitions. Reports remain private until manually reviewed and summarized. [Detailed workflow](docs/archaeology.md).

## Scope of this handoff

The first native browser is available for local iteration. Broader website playback, sustained stability, reboot durability, root/unlock, and backup validation remain separate milestones. Read the current session record before device-specific work.
