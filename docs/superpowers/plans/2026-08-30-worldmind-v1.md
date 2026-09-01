# Worldmind V1 Corrected Full Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans task-by-task with TDD for behavioral code.

**Goal:** Build Worldmind 1.0.0 as a continuously evolving, performance-bounded living-world simulation for Minecraft Java 26.2/Fabric.

**Architecture:** A Minecraft-independent simulation kernel owns regional cells, ecology, civilizations, history/belief, chronology, anomalies, and planning. Fabric adapters observe only normally loaded surroundings, persist compact state, and materialize bounded deterministic changes in naturally loaded chunks. Existing place intelligence becomes one specialization inside the global regional engine.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Loom 1.17-SNAPSHOT, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-30-worldmind-v1-design.md`

## Global Constraints
- Every generated/known region ages; ungenerated space is not simulated retroactively.
- Never force-load remote chunks or simulate remote entities/blocks tick-by-tick.
- Preserve containers/stateful blocks and Worldseal domains.
- Physical changes are bounded, deterministic, idempotent, and commit-tracked.
- Preserve all pre-correction V1 doctrines: civilizations/history/belief/anomalies/chronology/multiplayer/structure intelligence/layered progression.

---

### Task 1: Global regional simulation kernel
Create RegionKey, RegionState, RegionalSnapshot, evolution pressures, RegionalOutcome, GlobalEvolutionEngine, and tests proving continuous pressure, deterministic outcomes, multi-speed aging, Worldseal suppression, and no requirement for loaded chunks.

### Task 2: Regional registry and scheduler
Extend WorldmindState with known regions, priority queues/last-evaluated markers, and a budgeted GlobalEvolutionScheduler. Tests prove due-cell selection is bounded and old/high-pressure cells outrank dormant cells.

### Task 3: Ecology and route/farm/disturbance models
Add compact ecology/disturbance/route/farm signals and pure engines. Tests cover forest recovery, abandoned farm succession, repeated-route wear, and bounded pressure decay/accumulation.

### Task 4: Civilizations and autonomous history
Add CivilizationState, CultureProfile, CapabilityProfile, ResourceLedger, political/diplomatic state, migration/split/collapse operations, and CivilizationEngine. Tests prove growth, scarcity response, split/migration, and causal autonomous events without remote entities.

### Task 5: History causal graph and belief layers
Add HistoricalEvent, causal links, significance compression, ActorBelief, belief propagation/distortion, and player legacy support. Tests prove objective truth remains stable while official/mythic beliefs diverge deterministically.

### Task 6: Rare anomaly scheduler
Add anomaly pressure and deterministic rarity gates. Tests prove anomalies are possible but orders of magnitude rarer than normal evolution and never required for progression/protection.

### Task 7: Deep place/structure intelligence integration
Keep existing PlaceRecognizer, add StructureIntent/Profile, confidence thresholds, region linkage, and ownership/contributor history. Tests cover restraint under low confidence and stronger constructive eligibility under high confidence.

### Task 8: Generalized transformation plans
Add region-targeted plan type alongside place plans, stable cause/event IDs, status/commit metadata, and serialization support. Tests prove stable IDs and idempotent commit behavior.

### Task 9: Loaded-world observation adapter
Use bounded periodic sampling around players to register/update regional cells and meaningful signals: movement routes, farmland, vegetation/disturbance, village features, player-place signals, and Worldseal locations. Never enumerate unloaded chunks.

### Task 10: Global physical materializer
Materialize wilderness reclamation, farm succession, route wear, village-lite cues, place decay/reclamation/constructive change, and historic-site aging using only loaded blocks. Respect MutationSafetyPolicy and Worldseal protection. Keep mutation count bounded.

### Task 11: Chronology and offline catch-up
Integrate PAUSED/LIVING/CAPPED_LIVING with regional/civilization/history simulation using compressed abstract passes, never missed-tick replay. Tests cover long-absence caps and prioritization.

### Task 12: Runtime orchestration and performance governor
Replace place-first runtime with observation → global scheduler → civilization/history → plan queue → near-player materialization, all under explicit budgets. Add diagnostics counters proving how many cells/plans/mutations run per cycle.

### Task 13: Worldseal survival content and protection index
Retain hard recipe and block content; make protection region-aware and reusable by all physical evolution systems. Tests cover protected core with unprotected surrounding cell evolution.

### Task 14: Persistence/migrations
Extend saved data codecs for regions, civilizations, events, beliefs, and region plans with backward compatibility for pre-corrected source state where feasible. Round-trip tests for pure serialization DTOs.

### Task 15: V1 acceptance harness and build gates
Expand docs/tests beyond Dirt Shack into Global Aging, Forest, Farm, Route, Village, Worldseal, Civilization, Belief, Chronology, Anomaly Rarity, Idempotency, and Performance gates. Run pure Java tests, static forced-chunk scan, Java 25/Fabric compile, then package source/JAR only after terminal evidence.
