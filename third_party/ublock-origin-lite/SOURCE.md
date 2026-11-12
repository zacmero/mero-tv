# uBlock Origin Lite

Unmodified Mozilla-signed extension: **2026.1110.1816**. License: **GPL-3.0-only**.

- [Official Firefox/Android listing](https://addons.mozilla.org/firefox/addon/ublock-origin-lite/)
- [Pinned signed package](https://addons.mozilla.org/firefox/downloads/file/5235054/ublock_origin_lite-2026.1110.1816.xpi)
- SHA-256, verified against AMO metadata: `3838bf88742ab921f553795b5b96f21e6429e198c1db3865c414402e5fe55059`
- [Corresponding source and build files](https://github.com/uBlockOrigin/uBOL-home/tree/04195b17ed1b505395ac739a12cd09f65d5cdc0d), release tag `2026.1110.1816`.
- The source tree pins its [uBlock submodule](https://github.com/gorhill/uBlock/tree/2bc679d99afe5bddd8fe3ab60137966a12123f41); obtain the source recursively to retain its inputs.

The build downloads and verifies the XPI into generated assets. The package retains upstream code, filters, translations, licenses, and component notices unchanged. The native app hosts it through GeckoView's extension API. The extension exposes its settings and filter lists inside the browser.

Full uBlock Origin 1.76.0 was evaluated but is not bundled. The confirmed 1 GB unit repeatedly lost processes during that setup. The app disables that earlier experimental extension if it is present, preserving its stored data.
