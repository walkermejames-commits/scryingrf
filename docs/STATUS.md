# Implementation status

| Area | Status | Notes |
|---|---|---|
| Android shell | WORKING | Compose Android 9+ app |
| Demo mode | WORKING | Demo resources are explicitly labelled in names |
| BLE scanning | PARTIAL | Real permission-gated foreground scan; hardware test required |
| Wi-Fi scanning | PARTIAL | Real Android scan API; OS throttling and hardware test required |
| Persistence | NOT STARTED | Current session is in memory; Room is the next implementation increment |
| Resource Pool | WORKING | Explicit ownership transition, no silent promotion |
| Project Compiler | WORKING | Offline rule-based camera/AI plans |
| Baseline/change detection/map/export | NOT STARTED | Model and UI work remain |
| Wi-Fi RTT/magnetometer | NOT STARTED | Must be hardware-gated |
| Hardware tested | NOT TESTED ON HARDWARE | Requires an Android 9+ device |
| Licence audit | PARTIAL | Nine MIT repos audited; RTT repository clone failed/no source inspected |
