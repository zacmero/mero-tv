# Handoff to Zac's local agent

Updated: **2026-11-09 (Brazil local date)**. This handoff follows the owner's successful network ADB inspection. Remote repository work did not connect to the Fire Stick.

## What Zac wants

1. Start with hardware/software archaeology and pursue Kamakiri if a compatible route can be established; gaining root is a real project goal.
2. Understand GhostLock and use it as a possible bridge to the older unlock route where appropriate.
3. Build a fast, reliable TV browser with a visually clear library of user-entered sites. The initial use case is a personal library of anime and film sites, with exact destinations supplied privately by the user. No addresses have been supplied, resolved, visited, or hardcoded.
4. Add, edit, remove, organize, and reopen sites with the remote; retain the library after app closure and reboot.
5. Aim for hardware-assisted 1080p playback where the source/player/engine supports it, with easy browsing and modest memory use.
6. Keep everyday browser use independent of GhostLock. Root, HOME-button replacement, and custom firmware can progress separately.
7. Map network/device secrets locally and keep them ignored by Git. The user deliberately withheld the network endpoint from the conversation.

## Current evidence

- Device identity is now confirmed from owner-supplied ADB results: `AFTSSS` / `sheldonp`.
- Fire OS `7.7.1.5`, build `PS7715.5586N`, incremental `0035736900228`, kernel `4.4.162+`, machine `armv7l`.
- Advertised Android patch date: `2026-03-01`. Firehome UI version: `7300121.PS7715`.
- App ABIs: `armeabi-v7a,armeabi`; shell UID 2000. SELinux enforcement is still to be queried.
- Approximately 243 MiB `MemAvailable` and 2.1G free on `/data` in that snapshot. These values fluctuate.
- A matching GhostLock profile exists, but the inspected current upstream source marks it `derived-untested`; exact-unit root/unlock has not been demonstrated.
- No exploit has run, no partition has been written, no backup/recovery path has been validated, and no browser APK has been built or tested by this project.

Use [device.md](docs/device.md) and [exploit-paths.md](docs/exploit-paths.md) as the detailed record. Do not infer bootloader state from an ordinary shell session.

## Suggested first local session

1. Inspect the checkout and preserve any user edits. Configure the already-authorized ADB target privately; avoid publishing `adb devices` output.
2. Run [the read-only collector](scripts/collect-device.sh) and inspect its files under `.private/recon/`.
3. Confirm identity still matches. Determine SELinux mode and the actual WebView provider/version. If `dumpsys webviewupdate` is unsupported, inspect packages locally and query the provider from a small Android diagnostic app; do not assume the engine is Google's current WebView.
4. Verify current display settings and supported playback using an ordinary browser and a known test video. Record the codec, actual video dimensions, dropped frames, remote behavior, and RAM. A TV set to 1080p and a player rendering 1080p are different observations.
5. Benchmark the current appropriate TV Bro release as a reference candidate. Verify its APK provenance, Android/ARM compatibility, and current reuse license. Decide whether to adapt it or build the small native shell described in [browser-plan.md](docs/browser-plan.md).
6. Investigate the exact GhostLock package and source profile independently of browser development. Record the commit/version and package hashes locally. Read the current device-specific instructions, understand automatic reboots/Wi-Fi/OTA changes, and establish the required USB power/data and recovery arrangement before execution.
7. For any progression to preloader modification/Kamakiri, use the exact matching current procedure and images. A report of success on `PS7716.5666N` does not establish success on this unit. Avoid generic downgrade or partition-write recipes.

## Milestones and completion evidence

| Milestone | Evidence required |
|---|---|
| Archaeology baseline | Reviewed identity, WebView provider, SELinux mode, display/codec inventory; raw evidence private |
| Browser viability | Real APK launches on stock firmware and survives a normal reboot with bookmarks intact |
| Usable TV browsing | Complete a browse/play/back/reopen flow with only the bundled remote |
| Playback target | Actual 1080p stream with recorded codec and acceptable behavior in a controlled test; site results tracked separately |
| Root experiment | Exact source/package recorded and observed UID 0; also record side effects and reboot behavior |
| Unlock/recovery | Observed unlocked boot path and usable recovery under the matching method; recovery coverage documented |
| Stable daily setup | Browser needs no host-side root ritual; HOME/default-launch behavior is explicitly documented |

## Public updates versus private evidence

Commit curated findings, implementation, reproducible non-sensitive test cases, and source links. Keep browser history/cookies, entered site lists, device identifiers, accounts, network topology, raw logs, and backup images private. Update dates and evidence status when the local results supersede this handoff.
