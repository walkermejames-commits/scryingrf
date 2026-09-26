# Data model

`TechnologyNode` represents a technology entity with ownership and availability states. `Observation` records a passive scan fact. `ProjectPlan` contains goal, assignments, gaps, and an explainable result. The current MVP holds these in memory; future persistence will add scan sessions, edges, fingerprint candidates, baseline records, position estimates, and timeline events.

Ownership is distinct from availability: `OBSERVED` and `OWNERSHIP_UNKNOWN` cannot be selected by the Project Compiler. Only `MINE` and `PERMISSION_GRANTED` enter the active Resource Pool.
