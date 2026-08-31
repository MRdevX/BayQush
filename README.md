# BayQush

Android app that forwards incoming SMS to Telegram.

Get the latest signed APK from [Releases](https://github.com/MRdevX/BayQush/releases/latest).

<img width="172" height="384" alt="photo_2026-08-31 18 29 51" src="https://github.com/user-attachments/assets/331d3ca1-22f5-4a56-b733-086babbab454" />

## Setup

1. Allow SMS access.
2. Paste a [BotFather](https://t.me/BotFather) token and a chat ID (your user or a channel, e.g. `-100…`).
3. Forward all SMS, or pick senders from the inbox.
4. Allow background so it still works with the screen off.
5. Send a test message.

On Samsung, also add the app to **Never sleeping apps**.

Credentials stay on the device. The bot token is hidden after you save.

## Build

Requires JDK 21 and the Android SDK. minSdk 24.

```bash
./gradlew assembleDebug
```

Pushes to `main` bump the version and attach a signed APK to the GitHub release.
