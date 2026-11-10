# Read-only archaeology workflow

The first owner inspection already established the model, codename, build, ABI, kernel, shell identity, memory, and `/data` capacity. [Device record](device.md). The next useful uncertainty is the actual WebView/media environment and the exact conditions for any root experiment.

## Run the collector locally

Requirements: Bash, Android platform tools (`adb`), and an already-connected, authorized Fire TV. On Arch, `android-tools` supplies ADB. Do not put network identifiers into this repository.

```bash
read -r -p 'ADB target (local IP:port or USB serial): ' ANDROID_SERIAL
export ANDROID_SERIAL
bash scripts/collect-device.sh
```

The script requires an explicit target and checks for `AFTSSS` / `sheldonp`. If a different device is selected, it stops after identity collection. It captures a fixed set of properties rather than an unrestricted `getprop` dump. All output goes to ignored `.private/recon/` folders. Optional diagnostic commands may be absent on a particular Fire OS build; failures are recorded without attempting to repair the system.

The script performs only device reads: identity/properties, kernel and user information, SELinux mode, memory/storage, WebView status, and filtered package listings. It makes no connection attempts to guessed addresses and performs no root, reboot, installation, package disabling, data clearing, or partition write. ADB may start its usual host-side daemon when invoked.

## Questions to answer next

| Question | Local check | Why it matters |
|---|---|---|
| Is the selected target still the recorded unit? | Model, device, incremental, kernel and ABI | Avoid applying a recipe to another stick or changed firmware |
| Is SELinux enforcing? | `adb shell getenforce` | The previously supplied security context does not answer this |
| Which web engine will our app use? | `adb shell dumpsys webviewupdate`; provider query from a diagnostic app if needed | Site support and engine updateability depend on the installed provider |
| What is the actual display mode? | Settings and a targeted local display inspection | Output resolution is separate from video stream resolution |
| Which decoders are available to the app? | Small app using Android `MediaCodecList` and an actual browser playback test | Hardware capability alone cannot guarantee web-player compatibility |
| Can we use the complete flow with the remote? | Address entry, pointer/focus, embedded video, fullscreen, Back | TV usability is a product requirement |
| Is USB data and power reliable for recovery work? | Appropriate cable/host inspection using current device-specific instructions | Successful LAN ADB does not prove the USB recovery path works |

For per-provider package details, first identify the package locally, then query that specific package. Do not publicly paste whole package, network, or activity dumps. Keep site test URLs and browsing state private.

## Evidence handling

- Raw reports are local evidence, not auto-publishable inventory. Curate a concise summary into `device.md` after inspection.
- Current root is not established by a matching exploit profile. A working exploit must produce an observed privileged result.
- Do not treat `ro.build.version.security_patch` or a kernel build date as an independent vulnerability verdict.
- Record recovery requirements before a boot-component change. A partial user-data copy is not a complete device recovery backup.
- Preserve the firmware version while researching compatibility; record any update or change that occurs.

## Sources

- [Amazon: ADB connection and developer options](https://developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html)
- [Android: ADB and shell commands](https://developer.android.com/tools/adb)
- [Android: WebView.getCurrentWebViewPackage](https://developer.android.com/reference/android/webkit/WebView#getCurrentWebViewPackage())
- [Android: MediaCodecList](https://developer.android.com/reference/android/media/MediaCodecList)
- [Arch: android-tools](https://archlinux.org/packages/extra/x86_64/android-tools/)
