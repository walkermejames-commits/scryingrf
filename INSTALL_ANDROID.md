# Install Scrying on Android

Download [scrying-0.3.0-debug.apk](dist/scrying-0.3.0-debug.apk) to an Android 9 (API 28) or newer device.

1. Open the downloaded APK using Files/Downloads.
2. If Android asks, allow the app you used to open the file to install unknown apps. This is required only because this is a locally built debug APK, not a Play Store package.
3. Tap **Install**, then open **Scrying**.
4. On Home, tap **Enable sensing** and approve only the permissions you are comfortable granting. Without them, the app remains usable for demo resources and manual inventory.
5. Tap **Scan now** to passively observe BLE and Wi-Fi results. Android and the device hardware may limit scan frequency.

APK SHA-256:

`A853401DCEAA7AB61E4048BC75A84033401709C257558F29AE13B48D77F57F0F`

This is a debug-signed build for direct testing. It contains no analytics, account, telemetry, or remote service. A production release requires a separately managed release signing key; none has been fabricated or embedded in this repository.
