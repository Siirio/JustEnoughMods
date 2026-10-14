# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0007

**MEDIUM — Profession tree hover repeats the node title instead of effect description**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T19:07:22.394960+00:00; latest: 2026-10-13T21:30:19.589599+00:00. Recheck: REPRODUCED: representative Architect node tooltip.

**Mode/environment:** Authenticated dedicated-server Survival, fresh clone-only native account; no administrative XP/gear grants. Time set day after first death.

**Attempt/reproduction:** Inspect a/u constructors, ProfessionNode and hover render; verify with a real miner tree once client parity is ready.

**Expected player experience:** Hover explains the effect and how to invoke it, especially for locked or unfamiliar skills.

**Actual behavior and impact:** ProfessionCatalog helpers accept an effect parameter but discard it and set effectKey to jem.professions.node.<id>. ProfessionTreeScreen renders effectKey, so the hover is the same label already drawn on the card. Actual L1 Architect hover shows the same “Призрачный строитель: плоскость3×3” title again, rather than explaining its effect. Only this representative node tooltip has been observed.

**Established cause / stated uncertainty:** Five whys: hover repeats title -> effectKey is node title key -> helper ignores its effect input -> catalog dropped separation of effect description and name -> data-to-presentation contract is incomplete.

**Proposed owner correction and native reuse:** Use a maintained localized effect key per node, including activation, range, cooldown, dependencies and truthful unsupported status.

**Acceptance:** Hover at ordinary GUI scale adds meaningful actionable details in RU/EN without clipping.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionTreeScreen.java:61](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionTreeScreen.java)
- [EVIDENCE/screenshots/actual/013-architect-tree-overflow-tooltip.png](../../EVIDENCE/screenshots/actual/013-architect-tree-overflow-tooltip.png)

## JEMQA-20261014-0008

**DESIGN_OPPORTUNITY — Most profession branches are declared unavailable even at level 50**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:07:22.413144+00:00; latest: 2026-10-13T20:30:58.776407+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inspect all ten explicit ProfessionCatalog graphs; gameplay verification must attempt unavailable branches and inspect actual messaging.

**Expected player experience:** Level progression has useful new actions and understandable capability growth.

**Actual behavior and impact:** Catalog intentionally uses UNSUPPORTED_NATIVE_API for every Farmer, Engineer, Merchant, Mayor, Warrior, Explorer and Mage node; Miner has six available nodes, Fisher one and Architect one. This is missing functionality, not an observed failure of implemented abilities. Actual Farmer L1 tree after genuine harvestXP6 shows all six nodes unavailable; screenshot038. This is missing perk implementation, not failed permission/unlock.

**Established cause / stated uncertainty:** Source-confirmed deliberate fail-closed availability, per earlier engineering choices. No safe API has been demonstrated for these branches; lack of implementation must not be mistaken for functional RPG progression.

**Proposed owner correction and native reuse:** Choose one meaningful native action per empty profession before marketing complete profession progression; make unsupported milestones truthful and avoid reward grind for unusable powers.

**Acceptance:** At least one legitimate Survival action and visible outcome per offered profession, with source-linked native contracts and fresh-player discovery evidence.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java)
- [Docs/DEVELOPMENT.md](../../../../DEVELOPMENT.md)
- [EVIDENCE/screenshots/actual/038-farmer-tree-all-six-unavailable.png](../../EVIDENCE/screenshots/actual/038-farmer-tree-all-six-unavailable.png)

## JEMQA-20261014-0014

**MEDIUM — Mage cast XP bypasses the ordinary profession daily/creative anti-farm guards**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:25:57.304764+00:00; latest: 2026-10-13T20:30:58.908481+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inspect spellCast and ProfessionApi.credit/ProfessionRecord.credit; disposable repro casts one cheap repeatable spell at each cooldown and compares XP/counters, including Creative and Survival.

**Expected player experience:** Mage progress derives from legitimate relevant Survival casts with auditable bounded repeat earning.

**Actual behavior and impact:** IronsProfessionAdapter credits every successful spell event directly with max(1,spellLevel), generating a new audit UUID per game tick. It has no daily reserve, creative/spectator/FakePlayer guard or bounded repeat source. Other profession gameplay paths use daily budgets. Actual level-50 farm is not replayed.

**Established cause / stated uncertainty:** Five whys: repeated casting can continue awarding -> every game tick creates distinct accepted audit -> adapter calls unbounded credit directly -> generic API validates identity and duplicate receipt only -> native spell adapter omitted anti-farm admission owner contract.

**Proposed owner correction and native reuse:** Bound legitimate cast credit in the owning adapter/domain, differentiate actual cast costs/sources and preserve native spell cooldowns; exclude administrative/Creative activity where required.

**Acceptance:** Legitimate varied Survival casts advance Mage; repeated trivial/admin casts cannot bypass its explicitly authored budgets and every award has a durable bounded receipt.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `namespace:irons_spellbooks`

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/compat/irons/IronsProfessionAdapter.java:20](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/compat/irons/IronsProfessionAdapter.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/api/ProfessionApi.java:21](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/api/ProfessionApi.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionRecord.java:42](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionRecord.java)

## JEMQA-20261014-0018

**HIGH — Human +5% profession trait grants +100% on ordinary one-XP construction actions**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:37:38.622237+00:00; latest: 2026-10-13T20:30:58.990459+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Choose Human then compare many ordinary base-1 placements with another race; inspect accepted base/bonus receipts and daily total in a disposable Survival scenario.

**Expected player experience:** The advertised +5% accepted profession progress stays approximately 5% across small repeated awards, within the daily cap.

**Actual behavior and impact:** reserveProfessionBonus computes max(1,floor(baseDelta*0.05)). A base-1 Architect placement gets +1, doubling its XP; base-2 Farmer/Explorer awards gain +50%; base-3 Miner gains +33.3%, until the 250 daily bonus cap. These are exact arithmetic consequences of the implemented source, not a replayed level progression.

**Established cause / stated uncertainty:** Five whys: small actions exceed 5% -> every accepted event receives at least one whole XP -> max(1, floor(...)) rounds each sub-one fraction up -> source stores no fractional carry -> percentage trait owner implemented per-event minimum rather than cumulative percent accounting.

**Proposed owner correction and native reuse:** Use persisted fractional/cumulative accounting at the verified race owner, preserve the authored +5% and 250 cap and avoid per-action rounding exploits.

**Acceptance:** Twenty eligible base-1 awards yield about one bonus, not twenty; same total base XP split across actions produces equivalent bonus; restart/replay cannot duplicate residue.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/race/RaceService.java:63](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/race/RaceService.java)
- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/race/RaceRecord.java:38](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/race/RaceRecord.java)
- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/compat/professions/ProfessionRaceBridge.java:15](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/compat/professions/ProfessionRaceBridge.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java:57](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java)

## JEMQA-20261014-0019

**HIGH — Merchant and Mayor tracks have no production XP producer**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:37:38.633453+00:00; latest: 2026-10-13T20:30:59.010686+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Search all canonical Mods/*/src/main/java for ProfessionId.MERCHANT/MAYOR and every ProfessionApi.credit consumer, then attempt legitimate trades/town actions after client parity.

**Expected player experience:** Each offered profession progresses through relevant legitimate actions such as funded trade or town work.

**Actual behavior and impact:** Production source search finds MERCHANT/MAYOR only in the node catalog/enum. Economy, Settlements and Server have no ProfessionProgressRequest/credit path for these tracks; only the administrative grantxp path can directly assign them XP. Missing functionality is distinct from implemented behavior failing.

**Established cause / stated uncertainty:** Five whys: normal players cannot advance tracks -> relevant trade/town actions emit no profession credit -> only the generic service and nodes exist -> domain owners have no audited native bridge -> feature integration did not connect offered tracks to legitimate work producers.

**Proposed owner correction and native reuse:** Connect meaningful validated funded-trade and native town-work results through existing Profession API with appropriate exact-once/anti-farm admissions. Do not award from GUI opens or untrusted client requests.

**Acceptance:** Real accepted Survival work advances each track; rejected/no-op/replayed trades and idle menu clicks award none; visible progress and unlocks are observable.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java)
- `Mods/JEMEconomy/src/main/java` (exact reference; local/raw or source version may require lookup)
- `Mods/JEMSettlements/src/main/java` (exact reference; local/raw or source version may require lookup)

## JEMQA-20261014-0020

**MEDIUM — Farmer harvest XP omits ordinary melon and pumpkin harvests**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:37:38.654562+00:00; latest: 2026-10-13T20:30:59.035584+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Grow and break native melon/pumpkin in Survival; compare mature wheat. Inspect exact block classes and the only Farmer credit producer.

**Expected player experience:** Comparable legitimate crop harvest actions participate in the same profession without requiring one vanilla inheritance shape.

**Actual behavior and impact:** blockBroken awards Farmer only when block instanceof CropBlock and max age; ordinary minecraft:melon and minecraft:pumpkin are not CropBlock, so their harvest takes no Farmer path. This is source-confirmed predicate exclusion, not an actual played harvest result.

**Established cause / stated uncertainty:** Five whys: these harvests earn zero -> crop predicate excludes their actual classes -> credit uses CropBlock inheritance only -> native stem-fruit/berry and addon harvesting signals are unadapted -> broad crop-work domain was reduced to one implementation class.

**Proposed owner correction and native reuse:** Audit actual native harvest signals for stem-fruit/berries and installed crop mods, include genuine harvesting evidence with anti-replant/exploit admission; avoid hand-picked ID-only patch.

**Acceptance:** Mature ordinary and audited addon harvests earn equivalent relevant XP; artificial placed fruit/repeated reset does not provide unbounded farms.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:melon`, `minecraft:pumpkin`, `minecraft:wheat`

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java:77](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java)
- [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json)
- Canonical lookup for `minecraft:melon`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:melon`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:melon`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:pumpkin`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:pumpkin`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:pumpkin`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0025

**HIGH — Explorer has a permanent level ceiling and loses future XP for exploration beyond a daily cap**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:45:29.316166+00:00; latest: 2026-10-13T20:30:59.172467+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inspect playerTick ordering, ProfessionRecord.claimDiscovery/reserveDaily and exact level-cost formula; Survival replay explores past a daily cap, revisits those chunks next day and inspects zero-award receipts.

**Expected player experience:** Legitimate exploration can reach offered level 50 and daily budgets defer earning without silently erasing an entire career.

**Actual behavior and impact:** PlayerTick awards 2 XP only for a new chunk discovery. The shared discovery set permanently rejects additions at 8192 keys, giving at most 16384 base Explorer XP before other-track keys reduce it. ProfessionFormula needs 23744 for level 50; the base ceiling is level 41. Discovery is persisted before daily allowance, so chunks entered after the 200/day cap consume keys for zero XP. The Human rounding defect is a separate unintended bonus, not a valid progression design.

**Established cause / stated uncertainty:** Five whys: offered late levels are unreachable/lose progress -> unique earning opportunities are exhausted -> bounded shared set freezes all future discoveries and records unpaid ones -> the bound and daily cap were implemented independently -> profession earning lifetime and level-cost contract were never reconciled.

**Proposed owner correction and native reuse:** Design a bounded authoritative exploration ledger/earning contract that preserves distinct work, sustainable long-term progression and explicit deferred/capped feedback. Do not simply delete old discovery marks and enable repeat farms.

**Acceptance:** Non-Human legitimate exploration can reach level 50; daily caps do not permanently destroy unexplained future progress; persistence remains bounded and repeated/replayed movement cannot farm XP.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java:154](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionRecord.java:32](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionRecord.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionFormula.java:10](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionFormula.java)

## JEMQA-20261014-0035

**HIGH — Race-selection cards overlap Russian traits and footer crosses selection controls**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T23:36:57.017901+00:00; latest: 2026-10-13T23:36:57.017914+00:00. Recheck: NOT_FIXED.

**Mode/environment:** Actual authenticated NON-OP Survival client

**Attempt/reproduction:** Non-op /jem role at GUI4,1707x960logical viewport, Russian locale; inspect before choice and after Human confirmation.

**Expected player experience:** All three race benefits and irreversible/free-switch conditions are fully readable before ordinary player choice.

**Actual behavior and impact:** Every card overlaps wrapped benefit paragraphs; Human last line spills through card bottom. Footer intersects selection buttons. Native confirmation is clear and ordinary Human choice succeeds; choice mechanics are not broken.

**Established cause / stated uncertainty:** RaceSelectionScreen render places each wrapped trait at fixed24pixel increment without measuring wrapped height; card112height and independent buttonY/footertop126 collide.

**Proposed owner correction and native reuse:** Keep established cream/copper/brown styling; measure real wrapped heights, flow each paragraph, reserve footer/button space and add bounded scrolling or alternate card layout where viewport cannot fit.

**Acceptance:** All traits and footer readable and hitboxes distinct at inspected viewport plus smaller native GUI; native choice, nonce and switch rules unchanged.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/RaceSelectionScreen.java:27](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/RaceSelectionScreen.java)
- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/RaceSelectionScreen.java:65](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/client/RaceSelectionScreen.java)
- [EVIDENCE/screenshots/actual/032-race-screen-overlapping-russian-benefits.png](../../EVIDENCE/screenshots/actual/032-race-screen-overlapping-russian-benefits.png)
- [EVIDENCE/screenshots/actual/034-human-race-selected-native-status.png](../../EVIDENCE/screenshots/actual/034-human-race-selected-native-status.png)
