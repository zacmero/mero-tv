# Mero TV

Fire TV hardware archaeology, a documented root/unlock research track, and a lightweight browser with a personal website library for Zac's TV.

**Status — 2026-11-09:** the device is confirmed as **Fire TV Stick 3rd generation / AFTSSS / sheldonp**. Network ADB works. The observed shell is unprivileged. This repository currently contains research, a local-agent handoff, and a read-only collection tool; the browser APK has not been implemented and no exploit or firmware write has been performed by this project.

## Start here

1. [Local-agent handoff](HANDOFF.md): current state, next steps, and completion criteria.
2. [Device record](docs/device.md): exact owner-reported inventory and sourced specifications.
3. [Archaeology workflow](docs/archaeology.md): collect the next facts without changing device software.
4. [Exploit paths](docs/exploit-paths.md): GhostLock compatibility evidence, Kamakiri, persistence, and recovery considerations.
5. [Browser plan](docs/browser-plan.md): editable site library, remote navigation, playback, and implementation choices.
6. [Privacy and local data](docs/privacy.md): keep network details, credentials, browser data, and raw evidence out of Git.
7. [Further experiments](docs/future-experiments.md): custom ROMs, terminal tools, agent displays, and remote PC access.
8. [Agent conventions](AGENTS.md): evidence and workflow rules for contributors.

## Confirmed baseline

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

Begin with archaeology. The historical direct Kamakiri route requires Fire OS below 7.2.7.3. The current GhostLock source contains a profile matching this unit's build/incremental/kernel/machine, but marks it **derived-untested**. Treat the unit as a candidate for local validation, not a confirmed successful unlock. A published reproduction on a different build shows a possible temporary-root → controlled preloader change → Kamakiri → TWRP route. [Evidence and exact sources](docs/exploit-paths.md).

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

The immediate deliverable is the reproducible research baseline and product plan. Browser implementation, real-device playback benchmarks, GhostLock execution, backup validation, and any firmware changes are work for the local agent with access to the unit. No claim of successful root, unlock, or browser compatibility is made here.
