# Implementation status

| Area | Status | Notes |
|---|---|---|
| Android shell | WORKING | Four-destination Compose instrument shell (Pulse, Atlas, Circle, Forge), dark-aware theme, and first-run onboarding |
| Device-ready package | WORKING | Debug-signed APK staged in `dist/`; physical-device validation remains required |
| Demo mode | REMOVED | No synthetic resources are presented as real hardware |
| This device profile | WORKING | Real Android hardware, sensor, storage, network, and permission capability profile |
| People records | WORKING | Local-only, explicit people/permission notes |
| Device contributions | PARTIAL | Device health, BLE, and Wi-Fi observer controls work; pairing and sensor streaming remain next |
| BLE scanning | PARTIAL | Real permission-gated foreground scan; hardware test required |
| Wi-Fi scanning | PARTIAL | Real Android scan API; OS throttling and hardware test required |
| Persistence | WORKING | Room stores technology nodes across app restarts |
| Resource Pool | WORKING | Explicit ownership transition, no silent promotion; manual hardware entry |
| Project Compiler | WORKING | Offline rule-based camera/AI plans |
| Baseline/change detection | PARTIAL | Conservative first-seen / stale-resource evidence messages; baseline learning remains |
| Map/export | PARTIAL | Active location survey and MapLibre Atlas UI are implemented; physical-device lifecycle and map rendering validation remain |
| Wi-Fi RTT/magnetometer | NOT STARTED | Must be hardware-gated |
| Hardware tested | NOT TESTED ON HARDWARE | Requires an Android 9+ device |
| Licence audit | PARTIAL | Nine MIT repos audited; RTT repository clone failed/no source inspected |
