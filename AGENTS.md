# Working on Mero TV

## Scope and priority

Follow Zac's current instructions. The project combines Fire TV archaeology/root research with a lightweight browser and editable website library. Preserve progress in both areas: ordinary browser use must not depend on running a temporary-root exploit. Do not repeatedly request confirmation for already authorized, routine read-only work or reversible development.

Read `HANDOFF.md`, `docs/device.md`, `docs/exploit-paths.md`, `docs/browser-plan.md`, and `docs/privacy.md` before choosing device-specific actions. This is a public repository.

## Evidence

Distinguish owner-reported observations, primary-source facts, compatibility claims, and proposals. Record exact build/incremental/kernel/ABI before target-specific work. A profile marked `derived-untested` is not a successful reproduction. An SELinux context alone does not tell you whether SELinux is enforcing. Never report a device action as completed without observing its result.

Pin upstream versions/commits and record hashes of independently obtained packages. Match the supported device and current procedure before firmware writes; do not mix a historical successful package with a different current checkout. Preserve boot components and recovery options according to the applicable device-specific procedure. Do not remove upstream checks simply to force a mismatch through.

## Privacy

Keep endpoints, serial numbers, MAC addresses, Wi-Fi details, credentials, signing keys, browser profiles, site exports, screenshots, raw dumps, logs, and firmware backups in `.private/` or outside the checkout. Do not paste these into commits, issues, terminal output intended for publication, or chat. Use allowlisted, manually reviewed summaries in `docs/`. `.gitignore` does not remove data already tracked by Git.

## App requirements

Target the confirmed Android 9 / 32-bit ARM device. Start with an empty editable site library; never hardcode service domains. Optimize remote focus, Back behavior, fullscreen video, and memory use. Reuse a maintained browser only after inspecting its current license and testing the actual APK/engine on the unit. Do not describe a visual prototype as a verified TV browser.

Keep arbitrary web pages separated from privileged operations: no shell/native command bridge or root endpoint exposed to page JavaScript. Measure startup, memory, navigation, playback resolution, and dropped frames on this hardware. Hardware codec capability does not guarantee every site's player will provide 1080p.

## Git workflow

Inspect the current branch and user changes before editing. Preserve concurrent work. Use ordinary commits and fast-forward updates; never force-push shared history. Check the staged file list and diff for private data before committing. Document source/reuse attribution if third-party code is introduced.
