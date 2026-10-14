# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0009

**HIGH — Quarantined settlement production is retried as if ordinary pending work**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:07:22.429114+00:00; latest: 2026-10-13T20:30:58.806135+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inspect partial-insert branch and next-tick skip predicate; use disposable physical storage scenario with a supported modded inventory, never production saves.

**Expected player experience:** Ambiguous storage mutation stays quarantined until explicit reconciliation, preserving real physical items.

**Actual behavior and impact:** deliverPending skips only DELIVERED. QUARANTINED obligations re-enter capacity/extraction/insertion on later ticks. If a handler partially accepts output then returns a remainder, input is rolled back but accepted output is not removed; subsequent retry can materialize the full obligation again. Native chest no-concurrency happy path may not trigger it; modded/reentrant handlers and crash boundaries need reproduction.

**Established cause / stated uncertainty:** Source-confirmed non-terminal quarantine and missing partial-output rollback; actual item duplication is a hypothesis until runtime replay. Root owner: SettlementSimulationService physical commit protocol.

**Proposed owner correction and native reuse:** Stop automatic execution of quarantined obligations and add durable per-slot mutation/commit recovery against supported native inventory contracts. Do not patch with blind refunds.

**Acceptance:** Partial acceptance cannot leave both accepted output and restored full input; ambiguous operations never auto-retry, restart preserves quarantine and operator-visible receipt.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:wheat_seeds`, `minecraft:wheat`

**Source/evidence links:**

- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/SettlementSimulationService.java:83](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/SettlementSimulationService.java)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/ProductionObligation.java](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/ProductionObligation.java)
- Canonical lookup for `minecraft:wheat_seeds`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat_seeds`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat_seeds`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0010

**MEDIUM — Settlement manual acceptance still tells operators background production is disabled**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:07:22.446393+00:00; latest: 2026-10-13T20:30:58.828966+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Compare acceptance doc with current activation and deliverPending implementation.

**Expected player experience:** Current acceptance matches latest source ownership and physical storage behavior.

**Actual behavior and impact:** PHASE2_MANUAL_ACCEPTANCE cases 8/10/15 expect zero Workers output. New SettlementSimulationService reserves Farmer wheat after verified work-area evidence and attempts linked physical input/output delivery. Docs/DEVELOPMENT.md describes the newer obligation path.

**Established cause / stated uncertainty:** Earlier disabled implementation acceptance was retained after newer source enabled evidence-backed Farmer obligations. The newer owning development doc/source wins; historical acceptance is stale.

**Proposed owner correction and native reuse:** Update exact acceptance cases for supported Farmer, missing evidence, input deficit, output-full/unloaded/removed storage, six-day cap and quarantine.

**Acceptance:** Operator follows one current scenario and can prove real input/output and journal behavior; no contradictory disabled-output expectation.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `workers:farmer`, `minecraft:wheat`

**Source/evidence links:**

- [Docs/settlements/PHASE2_MANUAL_ACCEPTANCE.md:12](../../../../settlements/PHASE2_MANUAL_ACCEPTANCE.md)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/SettlementSimulationService.java](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/SettlementSimulationService.java)
- [Docs/DEVELOPMENT.md](../../../../DEVELOPMENT.md)
- Canonical lookup for `workers:farmer`: [Catalog/registries/items/workers.json](../../../../../Catalog/registries/items/workers.json) (registry identity must be respected).
- Canonical lookup for `workers:farmer`: [Catalog/registries/entities/workers.json](../../../../../Catalog/registries/entities/workers.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0030

**HIGH — Loaded dead workforce entities can overwrite the canonical death mark as living**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T20:18:17.910901+00:00; latest: 2026-10-13T20:30:59.289075+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Register a legitimate Farmer, cause actual death near a settlement observation tick while the native dying entity remains loaded, inspect the roster before/after observation, then inspect later inactive production. Do not simulate an authenticated player or alter the original world.

**Expected player experience:** Actual worker death remains dead in the canonical roster, even while a native corpse/death-animation entity is still loaded.

**Actual behavior and impact:** SettlementRuntime correctly marks Workers dead: exact hierarchy is AbstractWorkerEntity -> AbstractChunkLoaderEntity -> BowmanEntity -> AbstractRecruitEntity. But observeLoadedWorkers checks only entity != null, then calls worker.seen; seen unconditionally sets living=true. The original dying worker still present at the 200-tick observation can therefore undo markDead before removal. Later production/specialization uses the saved living flag. Actual timed death replay is NOT_TESTED. Exact native die overrides delegate upward and normal LivingEntity.tickDeath retains the original entity until deathTime >=20. A separately spawned recruit corpse has another UUID and is not this lookup.

**Established cause / stated uncertainty:** Five whys: dead roster can become living -> seen overwrites living flag -> loaded observation does not check entity.isAlive -> presence was treated as authoritative life evidence -> workforce lifecycle conflated loaded/observed with living, overriding its correct death producer.

**Proposed owner correction and native reuse:** Admit living evidence only from an actually alive native entity; preserve explicit death versus unload and do not revive records from mere UUID presence. Review native corpse/removal timing before final replay.

**Acceptance:** Death marks survive subsequent loaded observations and restart; dead workers cannot produce or count as active jobs; unloaded living workers retain only approved evidence.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `workers:farmer`

**Source/evidence links:**

- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/runtime/SettlementRuntime.java:84](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/runtime/SettlementRuntime.java)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/workforce/WorkforceRecord.java:49](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/workforce/WorkforceRecord.java)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/SettlementSimulationService.java:46](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/simulation/SettlementSimulationService.java)
- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/payroll/PayrollService.java:26](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/payroll/PayrollService.java)
- Canonical lookup for `workers:farmer`: [Catalog/registries/items/workers.json](../../../../../Catalog/registries/items/workers.json) (registry identity must be respected).
- Canonical lookup for `workers:farmer`: [Catalog/registries/entities/workers.json](../../../../../Catalog/registries/entities/workers.json) (registry identity must be respected).
