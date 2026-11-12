# Application and milestone index

- **Mero TV** (`app`, `dev.mero.tv`): native site library and protected Gecko browser. Development deferred in favor of mero-stream. [Build](../browser-build.md), [browser milestones and limitations](../browser-reliability-2026-11-11.md), [initial device session](../local-session-2026-11-10.md).
- **mero-stream** (`stream`, `dev.mero.stream`): native anime search, episodes and HLS player. [Architecture and milestones](mero-stream.md).
- **SmartTube** (`org.smarttube.stable`): upstream native YouTube TV client, installed as a separate application. [Version, provenance, account flow and acceptance](smarttube.md).
- **Moonlight / Sunshine**: upstream streaming applications, not modules in this repository. Pairing and host/device observations live in [the session record](../local-session-2026-11-10.md). No changes to those applications accompany mero-stream.

Raw device evidence, personal destinations, stream tokens, credentials and signing material remain under ignored `.private/`. A successful build is not a playback milestone.
