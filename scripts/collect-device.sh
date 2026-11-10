#!/usr/bin/env bash
# Read-only device inventory. Raw output stays local and ignored by Git.
set +x
set -euo pipefail
umask 077

if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    printf '%s\n' 'Set ANDROID_SERIAL locally to the intended ADB target before running.' >&2
    exit 2
fi
if ! command -v adb >/dev/null 2>&1; then
    printf '%s\n' 'adb is required; install Android platform tools on the local workstation.' >&2
    exit 2
fi

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
private_root="$repo_root/.private/recon"
mkdir -p -- "$private_root"
chmod 700 -- "$repo_root/.private" "$private_root"
report_dir=$(mktemp -d "$private_root/$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX")
ADB=(adb -s "$ANDROID_SERIAL")

if ! "${ADB[@]}" get-state >"$report_dir/connection.txt" 2>"$report_dir/connection.stderr.txt"; then
    printf '%s\n' 'ADB target unavailable or unauthorized. See the newest .private/recon/ report locally.' >&2
    exit 1
fi
if [[ "$(tr -d '\r\n' <"$report_dir/connection.txt")" != device ]]; then
    printf '%s\n' 'ADB target is not in the expected device state. No inventory collected.' >&2
    exit 1
fi

failures=0
capture() {
    local name="$1"
    shift
    if "${ADB[@]}" "$@" >"$report_dir/$name.txt" 2>"$report_dir/$name.stderr.txt"; then
        printf '%s\tOK\n' "$name" >>"$report_dir/status.tsv"
    else
        local rc=$?
        printf '%s\tFAILED\t%s\n' "$name" "$rc" >>"$report_dir/status.tsv"
        failures=$((failures + 1))
    fi
}

capture model shell getprop ro.product.model
capture device shell getprop ro.product.device
if [[ "$(tr -d '\r\n' <"$report_dir/model.txt")" != AFTSSS ||
      "$(tr -d '\r\n' <"$report_dir/device.txt")" != sheldonp ]]; then
    printf '%s\n' 'Target does not match the recorded AFTSSS/sheldonp. Stopped after identity reads.' >&2
    exit 3
fi

# Explicit property allowlist: no serial, Wi-Fi, or network dump.
for prop in \
    ro.build.display.id \
    ro.build.version.name \
    ro.build.version.incremental \
    ro.build.version.release \
    ro.build.version.sdk \
    ro.build.version.security_patch \
    ro.product.cpu.abilist \
    ro.build.fingerprint \
    ro.bootloader; do
    capture "$prop" shell getprop "$prop"
done
capture kernel shell uname -a
capture shell-identity shell id
capture selinux-mode shell getenforce
capture memory shell cat /proc/meminfo
capture data-storage shell df -h /data
capture webview-provider shell dumpsys webviewupdate
capture webview-packages shell pm list packages webview
capture chromium-packages shell pm list packages chromium
capture amazon-rendering-packages shell pm list packages vizzini

printf 'Private report saved: .private/recon/%s/\n' "${report_dir##*/}"
printf 'Optional command failures: %s. Review status.tsv and all raw output locally before publishing a summary.\n' "$failures"
