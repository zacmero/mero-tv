# Custom TV browser implementation

## Goal
Ship an installable native browser with an empty editable site library on the confirmed Android 9 TV. Keep private browsing data and device evidence ignored.

## Phases
1. Commit and push streaming checkpoint — complete (e5d7e5c pushed to main).
2. Establish official TV Bro WebView reference and Android build tools — complete (reference launch; controlled comparison pending).
3. Implement native library, one WebView, remote cursor, fullscreen and Back handling — complete pending build validation.
4. Build, install, and validate library durability and controlled local browsing on the TV — complete for initial database and local video smoke checks; broader acceptance remains documented.
5. Record measured results and publish reviewed implementation — ready for publication in this checkpoint.

## Constraints
No firmware/OTA/driver changes. No privileged web bridge. No bundled service URLs. Preserve existing untracked .serena files. Reboot durability and prolonged site playback require separate evidence.

## Errors
Adoptium API returned HTTP 403; switched to official Temurin GitHub releases. Host lacks Java, Gradle, and Android SDK; install tools in ignored local storage.

Device test harness initially could not open the test-package database because instrumentation runs with the target UID. Fixed by using a distinct named test database in target storage; the owner library is untouched.

## Visual direction added by owner
Elegant, restrained cyberpunk: distinctive angular wordmark, dark ink surfaces, phosphor-green highlights and a restrained violet accent. Preserve readable remote focus and low memory use; use native vector/canvas assets rather than a new rendering dependency.
