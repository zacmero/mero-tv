# SmartTube — dedicated YouTube client

## Decision and deployment — 2026-11-12

Use upstream SmartTube as a separate native TV app; no YouTube player or ad blocker is reimplemented in mero-stream. The owner responded positively after launch. The existing YouTube app, Mero TV and mero-stream were not removed or modified for deployment.

Confirmed target: AFTSSS / sheldonp, Android API 28, ARMv7, incremental 0036408029828. Installed official **32.71 Stable**, package `org.smarttube.stable`, version code 2461. Actual APK minimum SDK is 17, target SDK 34, native ABI armeabi-v7a, approximately 25.5 MB. Installation and activity launch were observed successful. This is device compatibility/launch acceptance; authenticated use, sustained playback, resolution/frame drops and actual offered-ad coverage are not established by those checks.

## Pinned provenance

- Source: https://github.com/yuliskov/SmartTube
- Release: https://github.com/yuliskov/SmartTube/releases/tag/32.71s
- Asset: `SmartTube_stable_32.71_armeabi-v7a.apk`
- SHA-256: `288e8caaa11417f6b64d2a7f906cead85c0b8a9bb5904753d7cb4f79d117d023`
- Signer certificate SHA-256: `109735aa24fe85e2e1623ebb3b1ecc4bf4263f4c6374e4411d646bfa2eda482a`

The APK signature verified, and the obtained hash matched the official GitHub asset digest. These checks establish artifact identity/integrity, not an independent security audit. Downloaded APK, signature details, device output and UI evidence remain private.

## Features and account use

Upstream documents a TV-focused interface, playback without advertising UI, optional SponsorBlock, resolution/codec controls and no Google Services requirement. SponsorBlock depends on available community segments. Device hardware and the source still limit actual playback quality. See [official documentation](https://github.com/yuliskov/SmartTube#readme).

The app supports the owner's Google/YouTube account through a device-code authorization flow. The owner completes authorization on Google's page; no Google password, account approval, account linkage or extra runtime permission was entered/granted by the agent. App installation does not establish account safety. The maintainer discloses a historical compromised development environment and signing-key concerns in the README; use current official releases rather than third-party APK mirrors. No legacy YouTube replacement/voice-search bridge was installed.

An explicit public test-video VIEW launch was accepted, and the app's MediaSession reported playing during three samples. Its published base position remained zero; the subsequent pause probe did not expose a matching session. Those observations are not counted as verified advancing playback or resolution evidence. No account authorization was performed.
