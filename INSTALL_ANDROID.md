# Install Scrying on Android

Download [scrying-0.4.1-debug.apk](https://github.com/walkermejames-commits/scryingrf/releases/download/v0.4.1/scrying-0.4.1-debug.apk) to an Android 9 (API 28) or newer device. Do not download or extract the repository's source-code ZIP.

1. Open the downloaded APK using Files/Downloads.
2. If Android asks, allow the app you used to open the file to install unknown apps. This is required only because this is a locally built debug APK, not a Play Store package.
3. Tap **Install**, then open **Scrying**.
4. On Home, use **Review this phone**, **Add people**, or **Run an active survey**. Approve only the Android permissions you are comfortable granting.
5. Use **Contribute** to start a BLE or Wi-Fi observer. Android and the device hardware may limit scan frequency.

APK SHA-256:

`9EE3099D3853B8AF1F9AC3B632F172A648D84C7E938F6C41D21A424A2C464B11`

This is a debug-signed build for direct testing. It contains no advertising, account system, hidden telemetry, or remote network probing. The optional public aggregate hub is disabled by default and requires a separate user action for every report. A production release requires a separately managed release signing key; none has been fabricated or embedded in this repository.
