# Worldmind V1 Design — Corrected Full Authority

## Product identity
Worldmind makes Minecraft a continuously aging living world. Every generated/known region slowly changes over time whether players are nearby or absent. Player structures are one participant in that world, not the center of the simulation. Worldmind is deliberately unknowable in lore: the player can observe consequences and rare clues, but the mod never canonically confirms whether the world itself is conscious.

## V1 north-star experience
A player should be able to revisit any sufficiently old generated region and plausibly think, “This place was different before.” The strongest return-shock case is an abandoned player location that has decayed, been reclaimed, improved, occupied, or transformed, but V1 must also evolve wilderness, routes, farms, villages, infrastructure, ecology, cultures, and history.

## Scope rule
V1 includes the full architecture and playable first implementation of all doctrines locked in planning before the corrected global-evolution pass. The implementation may use abstract simulation and bounded materialization to keep the world performant; no subsystem is allowed to require brute-force ticking of remote entities or chunks.

## Target platform
- Minecraft: Java Edition 26.2
- Fabric Loader: 0.19.3
- Fabric API: 0.156.0+26.2
- Fabric Loom: 1.17-SNAPSHOT
- Java/JDK: 25
- Mod id: `worldmind`
- Version: `1.0.0`

## Hard invariants

### Continuous evolution
Every generated/known region is represented by a compact Regional Evolution Cell. Loaded and unloaded regions both age. Ungenerated terrain has no simulated past before generation. Evolution is pressure-based and multi-speed: tiny pressures accumulate continuously, then bounded physical changes materialize when appropriate.

### Performance
Worldmind must not force-load remote chunks, render remote areas, tick fake remote populations, scan the full world every tick, replay missed Minecraft ticks, or make simulation correctness depend on particles/shaders. Remote evolution is compact state math. Physical changes occur only in normally loaded chunks and under a strict budget.

### Save safety
- No software/data-loss disguised as gameplay loss.
- Container/stateful blocks are protected from unsafe replacement.
- Physical transformations are planned, validated, bounded, idempotent, and commit-tracked.
- Failed materialization defers safely.
- Worldmind state is additive to normal saves.

### Causality
Outcomes come from causal/contextual probability, not arbitrary RNG. Each significant event may record cause links. Randomness only chooses among plausible outcomes.

### Visibility
Worldmind is almost entirely environmental storytelling. No routine meters, event spam, reputation numbers, or quest markers. Rare clues may expose fragments, never complete internals.

### Multiplayer-native architecture
Actors use stable IDs/UUIDs. Events can have multiple contributors. Civilizations can remember players differently. V1 certification may focus on singleplayer gameplay, but data and simulation must not assume one anonymous player.

## Core architecture

### 1. Global Evolution Scheduler
Budgeted event-driven scheduler chooses which Regional Evolution Cells need abstract updates. Priority comes from elapsed time, historical pressure, player significance, unresolved events, and proximity to normally loaded player areas.

### 2. Regional Evolution Cells
Each known region stores compact state for:
- ecology and vegetation pressure
- disturbance and reclamation
- weathering/erosion proxy
- settlement/civilization pressure
- resources and scarcity proxy
- infrastructure/route wear
- threat/conflict pressure
- anomaly pressure
- historical significance
- last abstract evaluation and last physical materialization

Cells must be serializable without storing every block/entity.

### 3. Loaded-region observation
Worldmind samples normally loaded player surroundings at bounded intervals. It records meaningful signals such as repeated travel, clearing, farming, mining, fire/disturbance, settlement features, Worldseals, and player structures. It never performs global per-tick scans.

### 4. Global evolution outcomes
Regional abstract evolution may produce bounded plans such as:
- vegetation spread/thinning/regrowth
- moss/weathering/reclamation
- abandoned farmland succession
- repeated-route trail formation
- minor terrain/shoreline appearance change where safe
- village-lite growth/repair/fortification/decline
- infrastructure aging
- player-build aging or constructive intervention
- historical-place reoccupation
- anomaly manifestation at extreme rarity

### 5. Place/structure intelligence D+
Player and civilization builds are semantic places inside the regional system. Worldmind infers purpose, palette, entrances, roads, rooms, farms, defenses, symmetry/patterns, unfinished intent, expansion direction, ownership/contributors, age, abandonment, and confidence. Low confidence causes restraint. High confidence permits stronger contextual continuation or transformation.

### 6. Abandonment and constructive intervention
After roughly five Minecraft days of meaningful absence, unsealed player places become increasingly eligible for strong transformation. Outcomes can include decay, reclamation, occupation, fortification, repair, or constructive continuation. Long enough plausible history can grow a tiny site into a major settlement/city-scale historical center, but only through time/resources/actors and never as instant magic.

### 7. Civilizations D+
Worldmind supports evolving civilizations as abstract actors with:
- identity/name/symbol lineage
- population and resource pressure
- territory and settlements
- architecture/cultural traits
- governance/political state
- technology/capability profile
- diplomacy/trade/conflict
- migration
- internal factions/interest pressures
- knowledge and imperfect information
- historical memory

Civilizations can emerge, grow, split, merge, migrate, conquer, reform, collapse, and leave successor cultures or ruins. Remote populations are aggregate state; only historically important individuals require persistent identities.

### 8. History, belief, myth, and causal graph
Worldmind records objective history separately from actor knowledge. Significant events may link causes and consequences. Civilizations track observed/reported/recorded/official/mythic beliefs that can diverge from truth. Player actions can become living legends, propaganda, contradictory traditions, or eventually be forgotten.

### 9. Autonomous history + butterfly effects
History continues without the player. Small pressures can cascade into migration, conflict, collapse, settlement formation, cultural divergence, and new political entities. Player actions are powerful catalysts but never the sole source of history.

### 10. Culture, technology, and knowledge
Civilizations can learn, forget, copy, hybridize, and misinterpret. Architecture and capability evolve from resources, environment, inherited tradition, contact, disasters, and player influence. V1 keeps vanilla Minecraft progression intact and adds layered Worldmind knowledge/capability progression rather than replacing vanilla progression.

### 11. Ecology and environment
Ecology behaves as a pressure system: succession, regeneration, predator/prey proxy, water availability proxy, disturbance, civilization pressure, and recovery. The V1 physical layer focuses on safe visible changes rather than full fluid simulation or destructive biome replacement.

### 12. Rare impossible phenomena B+
The world is overwhelmingly causal and grounded, but very rare anomalies may create internally consistent phenomena with unknown in-world origins. They are not grind loops or routine biomes. Civilizations may interpret them differently. Worldmind may intentionally leave origin unresolved.

### 13. Adaptive content C+
Use vanilla content first. Add Worldmind content when the simulation cannot express a needed capability with vanilla alone. New content must have provenance and lineage rather than being random feature inflation. V1's required custom content is the Worldseal family; the architecture permits future generated cultural/ecological content without breaking saves.

### 14. Worldseal / Earned Physical Sovereignty
Protection is physical and earned. A difficult-to-craft Worldseal marks a finite domain that Worldmind may remember and react to historically but may not physically rewrite through decay/reclamation/construction/environmental materialization. Worldseals never freeze surrounding history. Large domains require greater investment; V1 ships one practical tier and a data model that supports connected/higher tiers later without migration.

### 15. Chronology D+
Per-world/server chronology modes:
- PAUSED: no offline aging
- LIVING: strong real-time contribution
- CAPPED_LIVING: default; bounded/compressed catch-up
Offline time becomes abstract historical pressure, never millions of replayed ticks. Long absences use diminishing conversion. Changing chronology affects future time only.

### 16. Layered progression
Vanilla survival progression remains dependable. Worldmind adds knowledge, relationships, archaeology/discovery hooks, civilization capabilities, regional opportunities, and rare phenomena as a second progression layer. V1 must not arbitrarily remove vanilla access.

## V1 physical evolution families
The first release must visibly materialize at least these families under bounded safety rules:
1. Wilderness vegetation/reclamation aging.
2. Disturbed forest recovery or transition.
3. Abandoned farm succession.
4. Repeated-route wear into subtle paths.
5. Village-lite development/decline/fortification cues.
6. Player structure decay/reclamation/constructive upgrade.
7. Historic place reoccupation/repurposing cues.
8. Extremely rare anomaly manifestation hook.

## Regional materialization
A RegionalTransformationPlan has a stable ID, region key, plan family, cause/event IDs, seed, intensity, target tick, mutation budget, and commit status. When a relevant chunk is naturally loaded:
1. revalidate region and Worldseal protection;
2. inspect only currently loaded/local blocks;
3. generate bounded candidate mutations;
4. reject unsafe/stateful/critical targets;
5. apply small deterministic batches;
6. persist commit state so reload cannot duplicate completed work.

## Configuration
V1 exposes safety/engineering controls, not hidden-gameplay dashboards:
- chronology mode
- global abstract simulation budget
- observation budget/radius
- physical mutation budget
- minimum major-abandonment days
- Worldseal radius
- anomaly allowance/intensity
- debug logging
- optional visual-effect intensity independent of simulation

## V1 acceptance suite
1. **Global aging:** several marked natural regions visibly differ after substantial world time.
2. **Loaded aging:** a region changes even while the player remains nearby for a long period.
3. **Unloaded aging:** a known region accumulates abstract history with no forced chunk loading and materializes when naturally revisited.
4. **Disturbed forest:** clearing develops plausible regrowth/transition.
5. **Farm:** abandoned farmland slowly reclaims.
6. **Route:** repeated travel creates persistent wear/path cues.
7. **Village:** vanilla village shows bounded contextual development/decline cues.
8. **Dirt shack:** abandoned unsealed structure can decay, reclaim, improve, or be repurposed after sufficient time.
9. **Constructive shock:** at least one seeded/contextual test produces a clearly improved/fortified player build.
10. **Worldseal:** protected blocks remain physically untouched while surrounding world continues evolving.
11. **Civilization abstract history:** civilization actor state can grow/split/migrate/conflict without remote entity ticks.
12. **Truth vs belief:** actor belief records can diverge from objective event history.
13. **Butterfly chain:** a small pressure can causally produce a multi-event downstream history.
14. **Chronology:** all three offline modes behave as specified and never replay missed ticks.
15. **Idempotency/save safety:** reload/crash-safe plan state does not duplicate committed physical change and does not delete inventories.
16. **Performance:** no full-world per-tick scan, forced remote chunks, or remote rendering/entity simulation is required.
17. **Anomaly rarity:** anomaly scheduling is possible but remains extremely rare and budgeted.

## Non-negotiable interpretation
The Dirt Shack Test is an acceptance case, not the product definition. V1 succeeds only when Worldmind makes the *world itself* feel continuously alive and historically cumulative.
