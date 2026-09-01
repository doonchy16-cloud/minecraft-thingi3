# Worldmind V1.1 Acceptance

## Global visible-aging test
1. Back up or create a disposable test world.
2. Capture a before screenshot of a forest/plain transition from a reproducible position.
3. Remain/explore for 10+ seconds.
4. `/worldmind inspect` must show a known region and non-zero spatial signal(s).
5. `/worldmind advance 30`
6. `/worldmind materialize`
7. Capture an after screenshot.
8. PASS requires a recognizable coherent patch/frontier/corridor change, not isolated random pixels.

## Route test
Walk the same A-B route repeatedly. Stationary observation must not produce route heat. `/worldmind inspect` should show route pressure near the traveled corridor. After advance/materialize, path wear should align with the route.

## Farm test
Observe a farm, stop maintaining it, advance sufficient Worldmind time, and materialize. Succession must appear contiguously rather than as scattered independent farmland replacements.

## Worldseal test
Place Worldseal near a protected structure. Advance/materialize. Protected blocks remain untouched while eligible evolution can occur outside the protected domain.

## Safety/performance gates
- Java 25 + Fabric 26.2 compile/test PASS.
- No `getChunk(`, `setChunkForced`, `addRegionTicket`, or `forceLoad` in simulation/materialization source.
- Production JAR contains command, spatial engine, Worldseal assets, and recipe.
- Existing V1/HOTFIX2 state JSON remains readable through lazy defaults.
