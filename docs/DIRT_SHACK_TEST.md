# Worldmind V1 — Dirt Shack Runtime Certification

Build success is not enough to call runtime behavior PASS. Use a disposable test world or a backup.

## Test A — Recognition and evolution

1. Create a new Minecraft 26.2 Fabric world with Worldmind + Fabric API.
2. Build a small dirt/wood shelter.
3. Put a bed, chest/barrel, and crafting table/furnace inside.
4. Spend at least one recognition interval near it (~10 seconds).
5. Travel far enough that the base chunks naturally unload.
6. Remain away for more than five Minecraft days.
7. Return to within ~80 blocks.
8. Observe whether a plausible transformation materializes.
9. Save and reload.
10. Confirm the same transformation does not expand/duplicate merely from reload.

Expected possible outcomes across different worlds/seeds/returns:
- no major change (stasis)
- moss/natural reclamation
- bounded holes/deterioration in safe simple materials
- dirt/cobblestone/wood shelter upgraded toward stone-brick/spruce fortification
- blended change

## Test B — Inventory safety

1. Put valuable throwaway test items in a chest/barrel inside an evolvable shelter.
2. Trigger an evolution.
3. Confirm the inventory block and contents remain intact.

Any Worldmind-caused inventory deletion is an immediate FAIL.

## Test C — Worldseal

1. Craft or `/give` `worldmind:worldseal` in a disposable test world.
2. Place it inside the recognized base.
3. Leave for >5 Minecraft days.
4. Return.
5. Confirm Worldmind does not physically transform the sealed base.
6. Confirm surrounding normal Minecraft activity is not frozen by the marker.

## Test D — Performance boundary

With debug/profiling tools if available:
- verify leaving a recognized base does not keep its chunks loaded solely for Worldmind;
- verify there is no distant rendering requirement;
- verify normal FPS/TPS remains healthy during ordinary play;
- verify offline catch-up does not replay every missed Minecraft tick.

## Certification rule

- CI/build PASS = source compiles/tests/package checks pass.
- Runtime V1 PASS = Dirt Shack Tests A-D pass in-game.
- Do not mark runtime V1 PASS from QUEUED/RUNNING CI or from compilation alone.
