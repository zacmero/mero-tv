# Browser findings

- Repository initially contains research and a read-only collector; no app implementation.
- Current device: Android API 28, ARMv7; active Amazon WebView 138. See docs/local-session-2026-11-10.md.
- Native Views and platform SQLite can implement the first library without UI/runtime dependencies.
- Bluetooth peripheral placement and deferred Fire Stick remote mouse mapping are recorded in docs/future-experiments.md.

- Official stable TV Bro is v2.1.6. The 6.8 MB Gecko-excluded APK provides a WebView reference without a bundled engine. Upstream license was reviewed at master and the pinned release. No upstream code will be incorporated in the first native prototype.
- Build toolchain selected: JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, compile SDK 35. Google documents the JDK 17 / Gradle 8.9 compatibility for AGP 8.7.
- Java with native Android APIs avoids a Kotlin compiler/plugin dependency for this first implementation; SQLite replaces the optional Room layer.
