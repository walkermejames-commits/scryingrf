# Data model

`TechnologyNode` represents a technology entity with ownership and availability states. `Observation` records a passive scan fact. `ProjectPlan` contains goal, assignments, gaps, and an explainable result. `TechnologyNodeEntity` is the Room persistence representation; it stores nodes locally across app restarts. Future persistence will add scan sessions, edges, fingerprint candidates, baseline records, position estimates, and timeline events.

Ownership is distinct from availability: `OBSERVED` and `OWNERSHIP_UNKNOWN` cannot be selected by the Project Compiler. Only `MINE` and `PERMISSION_GRANTED` enter the active Resource Pool.
