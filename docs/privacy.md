# Privacy and local-only data

This repository is public. Zac intentionally withheld his ADB/network endpoint and asked that the local agent map sensitive information without committing it.

## Local storage convention

Use `.private/` (ignored by Git) or an external private directory for:

- ADB network targets, USB serial numbers, MAC addresses, SSIDs, Wi-Fi credentials, and network maps.
- Raw command output, logs, screenshots, photos of serial/MAC labels, account information, and app diagnostics.
- ADB keys, signing keys, authentication tokens, cookies, browsing profiles/history, and personal website-library exports.
- Firmware downloads, partition images, recovery backups, package binaries, and detailed exploit execution logs.

The collector creates `.private/recon/<timestamp>/` with restrictive host permissions. Its files are **private by default**, even though most individual commands are narrow. A directory name or an allowlist is not proof that every future firmware's output is anonymous. Review before publishing a summary.

Network access succeeded according to the owner. No endpoint is stored in the public device record. Use `ANDROID_SERIAL` in the local shell and avoid printing it. The README shows an interactive prompt so the value need not be put into shell history. An environment variable can still be visible to other processes running as the same local user; it is a local convenience, not a secret vault.

## Before committing

```bash
git status --short
git diff --cached --stat
git diff --cached
git check-ignore -v .private/recon/example/report.txt .env local.properties
```

Review the actual staged diff. Do not stage `.` blindly after a device session. `.gitignore` applies to untracked files and does not redact files already tracked or remove earlier commits. If private material was committed, stop further publication and address the exposure and any affected credentials deliberately.

The checked-in records contain model-wide identifiers and the owner-provided non-sensitive firmware/resource inventory. Full serial numbers, MAC addresses, IP addresses, ADB keys, and original label photos are intentionally excluded.

## Browser storage

Store bookmarks/settings and the browser profile in the application's private storage on the device. A future export should be an explicit user action. Prefer exporting site definitions/settings only; omit cookies, tokens, and history. Imported URLs must be validated and remain data, never shell commands. The public app starts with an empty library, with no private or hardcoded service addresses.

An ordinary browser should run without root. A future root-capable utility must be separate from the browser process and must never provide arbitrary visited pages with privileged JavaScript/native bridges.
