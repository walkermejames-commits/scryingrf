# Scrying

Scrying is a local-first Android technology-environment intelligence system. It separates passive observations from hardware the user has explicitly authorised for use.

## Current MVP

- Android 9+ Compose app with a live BLE scan and a legitimate Android Wi-Fi scan.
- Local Room database for technology-node history; radio identifiers are hashed before entering the model.
- Nearby-device view, explicit **This is mine** transition, demo Resource Pool, and a rule-based Project Compiler.
- Demo resources can produce a local AI or camera-system plan without an LLM or internet connection.

The app does not probe, connect to, authenticate to, or control observed devices. RSSI is shown as signal strength, never as precise distance.

## Build

Open in Android Studio, or run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`). CI writes the debug APK to `app/build/outputs/apk/debug/`; this Windows workspace writes disposable build output to `%TEMP%/scrying-build/app/` to avoid OneDrive file locks.

## Privacy

Scrying has no analytics, account, telemetry, advertising, cloud database, or network probing. It asks for Android sensing permissions only after the user presses **Enable sensing**.

See [docs/STATUS.md](docs/STATUS.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and [docs/REFERENCE_REPOSITORY_AUDIT.md](docs/REFERENCE_REPOSITORY_AUDIT.md).
