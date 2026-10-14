# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0023

**MEDIUM — Eligible skeleton horse has no legitimate native player-tame event into collection**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:44:41.816824+00:00; latest: 2026-10-13T20:30:59.127480+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inspect SkeletonTrapGoal bytecode and collection event admission; actual replay requires a native trap horse and authenticated Survival rider, not summon/creative.

**Expected player experience:** Every listed legitimate native mount can contribute its actual personal acquisition evidence to the x/12 collection.

**Actual behavior and impact:** MountRoster includes minecraft:skeleton_horse, but collection admits only AnimalTameEvent. Exact native SkeletonTrapGoal sets trap horses tamed directly; it does not call player tameWithName/onAnimalTame or set a player owner. Merely mounting the native horse has no collection adapter. Creative/admin spawns are intentionally excluded, so they are not a legitimate workaround.

**Established cause / stated uncertainty:** Five whys: native mount cannot enter collection -> no player AnimalTameEvent -> trap sets tamed state directly -> current adapter accepts only the generic event -> roster eligibility was not paired with every native acquisition/ownership signal.

**Proposed owner correction and native reuse:** Define audited native player ownership/acquisition evidence for trap horses or classify the unsupported personal acquisition honestly; preserve private identity, unique UUID and no-admin-spawn rules.

**Acceptance:** A legitimate naturally obtained horse can count once for its actual verified player; stolen/remounted/shared/admin horses cannot inflate collection; x/12 denominator matches attainable contracts.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:skeleton_horse`

**Source/evidence links:**

- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/collection/MountRoster.java](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/collection/MountRoster.java)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/collection/MountCollectionService.java](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/collection/MountCollectionService.java)
- [build/overnight-qa/SkeletonTrapGoal-bytecode.txt](../../../../../build/overnight-qa/SkeletonTrapGoal-bytecode.txt)
- Canonical lookup for `minecraft:skeleton_horse`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:skeleton_horse`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:skeleton_horse`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
