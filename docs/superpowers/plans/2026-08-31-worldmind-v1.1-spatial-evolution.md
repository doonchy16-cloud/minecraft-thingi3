# Worldmind V1.1 Spatial Evolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Worldmind's accumulated history physically visible as coherent spatial evolution and add deterministic admin test commands.

**Architecture:** Add an 8x8 persistent spatial field to each 128x128 region, feed it with loaded observation and real movement segments, derive spatial process plans from regional decisions, and materialize clustered geometry inside loaded chunks only. Existing history/civilization/place systems remain authoritative and are extended, not replaced.

**Tech Stack:** Java 25, Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-31-worldmind-v1.1-spatial-evolution-design.md`

## Global Constraints
- Never force-load remote chunks.
- Never require rendering for abstract simulation.
- Preserve HOTFIX2 saves through safe default field initialization.
- Worldseals remain exact physical safety boundaries.
- No-op materialization never resets physical age.
- Thirty simulated days must create screenshot-visible coherent change for an active eligible process.

---

### Task 1: Persistent spatial field
**Files:** Create `src/main/java/dev/worldmind/region/SpatialField.java`, `SpatialCell.java`, `SpatialSignal.java`; modify `RegionState.java`; test `SpatialFieldTest.java`.
**Produces:** fixed 8x8 field, coordinate mapping, signal accumulation/decay, neighbor queries.
- [ ] Write failing tests for block-to-cell mapping, bounded accumulation, neighbor propagation, and legacy null initialization.
- [ ] Run tests and confirm RED.
- [ ] Implement minimal field and RegionState lazy initialization.
- [ ] Run tests and confirm GREEN.
- [ ] Commit.

### Task 2: Actual movement corridors
**Files:** Create `src/main/java/dev/worldmind/observe/PlayerMovementTracker.java`; modify `RegionalObserver.java`; test `PlayerMovementTrackerTest.java`.
**Produces:** sampled movement segments written into route cells; stationary players produce no route heat.
- [ ] Write failing stationary/moving/repeated-path tests.
- [ ] Verify RED.
- [ ] Implement segment rasterization into spatial cells.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 3: Spatial observation
**Files:** Modify `RegionalObserver.java`, `RegionSignalInterpreter.java`; test `SpatialObservationTest.java`.
**Produces:** forest/farm/settlement/build/disturbance signals at sampled micro-cell coordinates.
- [ ] Write failing test that two distant samples update different micro-cells.
- [ ] Verify RED.
- [ ] Implement per-sample spatial updates while preserving aggregate pressure signals.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 4: Process planning and intensity magnitude
**Files:** Create `SpatialProcess.java`, `SpatialProcessPlan.java`, `SpatialProcessPlanner.java`; modify `RegionTransformationPlan.java`, `WorldSimulationService.java`; test `SpatialProcessPlannerTest.java`.
**Produces:** process type, anchor cells, frontier/corridor/patch geometry, mutation target count scaling with age/intensity.
- [ ] Write failing tests for 5/30/100-day scaling and process selection.
- [ ] Verify RED.
- [ ] Implement deterministic planner.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 5: Clustered materializer
**Files:** Rewrite focused parts of `RegionalMaterializer.java`; create `BiomeVegetationPalette.java`; test pure geometry in `SpatialMaterializationGeometryTest.java`.
**Produces:** patches/frontiers/corridors/growth edges mapped from micro-cells to loaded block targets.
- [ ] Write failing geometry tests proving clustered adjacency and corridor continuity.
- [ ] Verify RED.
- [ ] Implement target geometry and intensity-aware budgets.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 6: Worldseal spatial masking and success accounting
**Files:** Modify `RegionalMaterializer.java`, `RegionState.java`; extend `RegionMaterializationClockTest.java` and create `SpatialProtectionMaskTest.java`.
**Produces:** protected targets excluded before planning/apply; age resets only on real changes.
- [ ] Write failing mask and no-op tests.
- [ ] Verify RED.
- [ ] Implement masking/accounting.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 7: Diagnostic/test commands
**Files:** Create `src/main/java/dev/worldmind/command/WorldmindCommands.java`; modify `WorldmindMod.java`, `WorldmindRuntime.java`; test command-independent `WorldmindAdminServiceTest.java` with new `WorldmindAdminService.java`.
**Produces:** status/inspect/advance/materialize/history/debug service and Fabric command bindings.
- [ ] Write failing service tests for advance 30d and inspect output.
- [ ] Verify RED.
- [ ] Implement admin service then command registration.
- [ ] Verify GREEN and compile Fabric API binding in CI.
- [ ] Commit.

### Task 8: V1.1 acceptance and release metadata
**Files:** Modify `gradle.properties`, `fabric.mod.json`, `README.md`, `CHANGELOG.md`; create `docs/V1_1_ACCEPTANCE.md`; extend CI scan.
**Produces:** version 1.1.0, documented commands, acceptance recipe, release JAR.
- [ ] Run all unit tests.
- [ ] Run Java 25 Fabric build.
- [ ] Run forbidden forced-chunk API scan.
- [ ] Verify production JAR contents and version.
- [ ] Package source and playable JAR with SHA-256 hashes.
