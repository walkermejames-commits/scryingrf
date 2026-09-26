# Reference repository audit

Audit date: 2026-09-26. References were cloned to `C:\Users\edward\.codex\reference-scrying`, outside the production source. This table records the shallow clone tip inspected. No reference source has been copied into Scrying.

| Repository | Commit inspected | Licence | Useful lessons / relevant areas | Reuse decision | Compatibility and risks |
|---|---|---|---|---|---|
| https://github.com/eduardo8188/BLE-Hound | `43a2af8cda70c59234203744879c49a8867f7489` | MIT, GH0ST3CH 2026 | BLE discovery and tracker-family presentation | Reimplement concepts only | Tracker labels must remain evidence-based; no surveillance claims |
| https://github.com/jeb482/bledoubt | `435ce7ce972fbefa963b1d9c7b27a7441989f827` | MIT, Jimmy Briggs and Christine Geeng 2022 | Persistent observation and rotating-ID research | Reimplement only | Avoid treating persistence as malicious intent |
| https://github.com/santansarah/ble-scanner | `9e76dab6a244e1eded2480e5e19a812612698326` | MIT, Sarah 2023 | Kotlin BLE UI and RSSI presentation | Reimplement only | Verify Android permissions and background limits |
| https://github.com/darryncampbell/WiFi-RTT-Trilateration | clone did not populate | No source or licence inspected | Intended RTT/ranging study | Do not reuse | Treat as unlicensed/unavailable until a successful audit |
| https://github.com/TarikToha/WiFi-Map | `c0fd4c5086a301fbc5f7c7d3749be16a93de7d4b` | MIT, Tarik Reza Toha 2025 | GPS-associated Wi-Fi observations and exports | Reimplement only | Location data is sensitive; RSSI is not ranging |
| https://github.com/samyak2403/EMFMagneticFieldDetector | `47e77665429b198411492e14739d3c8c8d119c27` | MIT, Samyak Kamble 2025 | Magnetometer X/Y/Z/history UI | Reimplement only | Magnetometer is not a general RF detector |
| https://github.com/parawanderer/OpenTagViewer | `335b2589ea2c997522599de5a79ef14622ed6115` | MIT, Shane B. 2025 | Local tracker advertisement interpretation | Reimplement only | Never access third-party accounts or private locations |
| https://github.com/getnopeek/nopeek-android | `b6d2053933f921224f607e0ad319b3018a0b0b7e` | MIT, NoPeek 2025 | Device-signature registry design | Reimplement only | Bound parser inputs; signatures are probabilistic |
| https://github.com/weliem/blessed-android | `44bba83cc4cb5f19a1da28cd4548c4a5b8a1c816` | MIT, Martijn van Welie 2019 | Replaceable BLE abstraction | Evaluate later, no current dependency | Confirm current Android maintenance before adoption |
| https://github.com/jermsmit/civops-android-termux | `e79f609898307c0433312db640692e5ed57d70dc` | MIT, Jermal Smith 2026 | Offline timelines and NEW/LOST/RETURNED vocabulary | Reimplement only | Use neutral consumer terminology |

All listed inspected licence files identify the MIT licence. Dependencies and source-level copyright notices require a separate, file-specific audit before any direct source reuse. This project has no direct reuse, so no reference-specific attribution is currently required beyond this record.
