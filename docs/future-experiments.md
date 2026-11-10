# Further experiments and preserved research links

Research carried forward from 2026-11-08 and checked/organized for the 2026-11-09 handoff. These are optional later directions. The immediate priority is archaeology and the lightweight website-library browser.

## Model-specific custom Android

[LineageOS 20 for sheldon/sheldonp](https://github.com/amazon-oss/releases/releases/tag/lineage-20-sheldon-v0.5) is an unofficial Android 13 option. The most recent sheldon release found in this review is **v0.5**. The [release index](https://github.com/amazon-oss/releases/releases) labels v0.4 broken and directs users to build 5.

The maintainer requires a matching TrustZone update before installing this ROM. The linked artifact is named `sheldon-tz-PS7669.zip`. Google apps are separate. These notes identify prerequisites; they are not a flashing procedure, and there has been no ROM installation on Zac's unit. Obtain current instructions from the [maintainer's XDA thread](https://xdaforums.com/t/rom-unofficial-13-lineageos-20-for-fire-tv-stick-lite-3rd-gen-sheldon-p.4792510/) before selecting images. Do not promise particular streaming DRM levels, every remote feature, or a complete compatibility list: those have not been validated here.

The [older LineageOS 18.1 releases](https://github.com/mt8695/android_device_amazon_sheldon/releases) preserve the device's earlier Android 11 development history. Fixes reported for those releases do not automatically carry over to a different ROM. Bootloader access, a working recovery, persistent root, and a replacement OS are distinct milestones.

## Useful projects without a root prerequisite

| Experiment | Design idea | Research starting point |
|---|---|---|
| Suzy/Sera TV interface | Foreground avatar/status/audio client; agent processing remains on the PC/server | [Amazon web-app development](https://developer.amazon.com/docs/fire-tv/getting-started-with-web-apps.html) |
| Mero lab display | Router uptime, ESP32 measurements, Askey telephony events, rendering queues | Native Android or browser client calling a small backend |
| Local terminal | Try Python, SSH, Git, or short scripts with appropriate ARM Android builds and a keyboard | [Termux](https://termux.dev/en/) and its [installation documentation](https://github.com/termux/termux-app#installation) |
| Remote PC display | Moonlight on the stick, Sunshine on the Arch PC; measure input and decode latency | [Moonlight Android](https://github.com/moonlight-stream/moonlight-android), [Sunshine](https://github.com/LizardByte/Sunshine) |
| Personal media/display | Local music and I/O Experiments playback; possible menu/ambient display for Tijolo e Malte | Test actual formats and playback lifecycle on the unit |

Termux provides a non-root Android terminal environment, but its TV usability and always-on behavior remain to be tested. Moonlight has an Amazon Appstore distribution; Sunshine supports Linux and NVIDIA hardware encoding. Neither fact establishes latency or driver compatibility on Zac's complete setup. The Fire Stick's one-gigabyte memory budget favors focused clients over a whole multi-service Mero backend.

## Physical exploration

The exact [FCC internal-photo exhibit listing](https://fccid.io/2AUQI-7436/Internal-Photos/Internal-Photos-20200629-v1-2AUQI-7436-IntPho-4884500) is worth preserving for future board comparison. A verified UART pinout or exact board revision has not been established. Powered micro-USB OTG peripherals are a candidate for testing; use model-specific evidence and measure power/data reliability.

Generic Arch/Debian images are not an established boot option for this device. Replacing an Android image with a generic Linux distribution still depends on the boot chain, device tree, storage layout, and hardware drivers. Prefer the demonstrated model-specific Android work as the initial firmware research base.
