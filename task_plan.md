# Mero TV — active browser reliability work

## Goal
Deliver the native TV browser with working in-app ad blocking, reliable playback and remote navigation on the confirmed AFTSSS / sheldonp, API28 ARMv7 unit. Preserve the editable library and private profiles.

## Current phases
1. Restore context and confirm the device — complete; firmware remains PS7717.5746N / incremental 0036408029828 / kernel4.4.162+.
2. Diagnose intermittent decoding — complete. Same 1080p30 clip succeeds in system WebView but hit Gecko’s 100 ms GPU-process surface-allocation deadline. Parent-process hardware compositing avoids that handoff; three independent starts advance without decode errors.
3. Update and harden protection — complete for signed uBlock Origin Lite2026.1110.1816. Verified AMO SHA256 and source pins; startup has a deadline, late completions cannot bypass a failure, and website navigation waits for protection readiness. Normal-use OFF/ON request checks pass.
4. Validate real playback and controls — in progress. YouTube reached148s, with3683 advancing frames and zero additional drops over122.6s and no observed ad UI. Actual player UI/HD/fullscreen checks are running; optional Media Session callbacks must not be treated as universal video telemetry.
5. Final checks, documentation, privacy review, and cleanup — pending.

## Constraints
No firmware, driver, root, or display changes. No arbitrary page/native command bridge. No bundled service destinations. Use the original signing identity; never uninstall or clear owner data. Preserve pre-existing untracked .serena files. Keep endpoints, credentials, profiles, dumps, screenshots, and raw logs private. Keep tests inline until video is already advancing; restore the library automatically.

## Acceptance evidence
- Build/lint and URL-policy checks pass.
- Default SQLite and signed-extension instrumentation passed previously; rerun the current version checks before completion.
- Updated blocker’s actual OFF/ON filtering passes without a debugger. Earlier cold-start filtering and cookie/storage preservation checks pass.
- Reproducible playback fixture/server/checker added; checker passes three actual steady recordings and rejects the pre-fix decode error. Zero advancing frames are always a failed playback test.
- Remote D-pad pointer/Select, edge/page/direct scrolling passed on-device. Recheck final affected controls.
- Hardware WebRender remains available with the GPU process unused; MediaTek AVC/VP9 decoding observed.

## Known limits and avoided mistakes
Cold engine startup remains approximately30s on the tested profile. Hardware support alone does not establish smooth1080p60. A muted plain video can play without Media Session callbacks; inspect actual frame telemetry or player UI instead. A shell input command timed out during a debugger-attached YouTube test; no app ANR or surface timeout was captured, so this alone does not establish an app key-handling defect.

## Checkpoints
Streaming checkpoint e5d7e5c and original browser checkpoint1af60af are published. Extension/reliability work remains uncommitted. Historical session details are in progress.md, findings.md, and docs/development-handoff-2026-11-11.md. Alexa dictation stays deferred. Reboot durability and prolonged personal-site testing remain separate acceptance work.
