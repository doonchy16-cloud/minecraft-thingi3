# Worldmind V1.1 Spatial Evolution Design

## Goal
Worldmind V1.1 makes accumulated world history visibly legible in Minecraft by replacing sparse independent point mutations with persistent, spatially coherent processes while preserving V1 performance, save-safety, chronology, Worldseal protection, history, civilization, and multiplayer-ready architecture.

## Product invariant
After 30 Worldmind days of accelerated chronology, at least one active eligible process in the player's observed area must produce screenshot-visible, spatially coherent physical change. The change must be attributable to a patch, corridor, frontier, growth edge, or protected-domain process rather than isolated random blocks.

## Architecture
Each 128x128 RegionState gains a lightweight 8x8 sparse spatial field. Each micro-cell represents 16x16 blocks and stores bounded signals for vegetation, forest, farm, settlement, disturbance, route use, reclamation, build presence, and protection. Observation updates only already-loaded surroundings. Abstract simulation ages fields without loading chunks. Materialization converts one process plan into clustered geometry only where Minecraft chunks are already loaded.

## Spatial processes
- FOREST_FRONTIER: spread or thin vegetation across neighboring micro-cells.
- RECLAMATION_PATCH: contiguous nature/weathering progression.
- FARM_SUCCESSION_PATCH: contiguous abandoned agricultural succession.
- ROUTE_CORRIDOR: movement segments accumulate route heat along actual paths.
- SETTLEMENT_GROWTH_EDGE: coherent paths/utility/fortification cues expanding from settlement influence.
- PLACE_DOMAIN: structure-aware reclamation or constructive transformation stays specialized.
- ANOMALY_SITE: rare localized geometry, unchanged in rarity doctrine.

## Intensity
Physical magnitude is derived from elapsed physical age, RegionalDecision intensity, process pressure, and configured mutation budget. A successful change resets only the micro-cells actually affected plus the region's physical materialization clock. A no-op does not reset age.

## Route semantics
Standing still is not travel. Player movement is sampled into line segments; repeated overlapping segments accumulate route heat. Route materialization follows the hot corridor and decays when use stops.

## Context
Materialization chooses biome-appropriate vegetation using local loaded biome/block context. Unknown/modded blocks are preserved conservatively. Worldseals are treated as spatial masks and exact final-block safety checks.

## Diagnostics
V1.1 adds server-authoritative admin/testing commands: /worldmind status, /worldmind inspect, /worldmind advance <days>, /worldmind materialize, /worldmind history, /worldmind debug <on|off>. Commands require operator permission and do not bypass Worldseal safety unless a future explicit admin override is designed.

## Performance
No remote chunk loading, no global per-tick block scan, no remote entities. Observation and materialization operate only in loaded chunks near players. Spatial state is fixed-size 8x8 per known region and simulation cycles remain budgeted.

## Persistence and compatibility
New fields deserialize with safe defaults so HOTFIX2 saves continue to load. No migration may delete existing Worldmind history, plans, places, protections, or civilizations.

## Acceptance
1. 30-day accelerated wilderness test shows coherent frontier/patch change.
2. Repeated A-B travel creates a corridor; stationary presence does not.
3. Abandoned farm succession is contiguous.
4. Village changes originate from settlement influence.
5. Worldseal physically masks processes while surrounding cells evolve.
6. 0-change materialization preserves accumulated age.
7. no forbidden forced-chunk APIs in simulation/materialization.
8. Java 25 + Fabric 26.2 build and all tests pass.
