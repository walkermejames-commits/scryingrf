# Architecture

The first increment deliberately keeps one Android module so the smallest installable app is easy to inspect. `ScannerRepository` wraps Android BLE and Wi-Fi APIs; `TechnologyNode` is the shared domain representation; the Compose UI displays observations and authorised resources; `ProjectCompiler` is a deterministic offline rule engine.

The intended next extraction is `core:model`, `radio:ble`, `radio:wifi`, `resources:inventory`, and `projects:compiler`. Room will replace transient state. Hilt was deferred to avoid introducing dependency injection before multiple modules exist; constructor injection is used now and Hilt is the planned module-boundary choice.
