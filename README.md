# Scrying

## Install on an Android phone

**Do not download the source-code ZIP.** Download and tap this one installable file on the Android phone:

[**Download Scrying v0.5.0 for Android (.apk)**](https://github.com/walkermejames-commits/scryingrf/releases/download/v0.5.0/scrying-0.5.0-debug.apk)

Scan this QR code on an Android phone to open the James OS APK repository download:

![QR code for Scrying v0.5.0 APK](assets/scrying-v0.5.0-download-qr.png)

After it downloads, open the phone's **Files** app, open **Downloads**, and tap `scrying-0.5.0-debug.apk`. Android will show the installation screen. If asked, allow the browser or Files app to install unknown apps, then return and tap **Install**. Full steps: [INSTALL_ANDROID.md](INSTALL_ANDROID.md).

Future releases can be checked from the app's **More options → Check for updates**. The check is user-triggered; Scrying never downloads or installs an update silently.

Scrying is a local-first Android technology-environment intelligence system. It separates passive observations from hardware the user has explicitly authorised for use.

## Current MVP

- Android 9+ Compose app with a live BLE scan and a legitimate Android Wi-Fi scan.
- Local Room database for technology-node history; radio identifiers are hashed before entering the model.
- Real installed-device capability profile, local People records, explicit device-health/BLE/Wi-Fi contributions, and a rule-based Project Compiler.
- Nearby-device observations are secondary context and never become a resource without an explicit ownership or permission confirmation.

The app does not probe, connect to, authenticate to, or control observed devices. RSSI is shown as signal strength, never as precise distance.

## Build

Open in Android Studio, or run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`). CI writes the debug APK to `app/build/outputs/apk/debug/`; this Windows workspace writes disposable build output to `%TEMP%/scrying-build/app/` to avoid OneDrive file locks.

For device installation, use the generated [Android installation guide](INSTALL_ANDROID.md).

## Privacy

Scrying has no advertising, account system, hidden telemetry, cloud database, or network probing. The optional public aggregate hub sends nothing unless the user turns it on and taps **Share latest aggregate report**. It asks for Android sensing permissions only after the user asks to use a sensing feature.

See [docs/STATUS.md](docs/STATUS.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and [docs/REFERENCE_REPOSITORY_AUDIT.md](docs/REFERENCE_REPOSITORY_AUDIT.md).

The optional public aggregate hub is documented in [docs/PUBLIC_INTELLIGENCE_HUB.md](docs/PUBLIC_INTELLIGENCE_HUB.md).
