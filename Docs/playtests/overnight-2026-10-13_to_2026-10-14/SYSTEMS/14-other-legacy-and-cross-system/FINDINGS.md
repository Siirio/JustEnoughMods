# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0003

**HIGH — Atlas selects stale server owner metadata for newer canonical modules**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T18:59:56.871464+00:00; latest: 2026-10-13T20:30:58.636442+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Query behavior jem_economy:shipping_box and inspect items/blocks jem_economy partition against manifest artifact inventory.

**Expected player experience:** Canonical client facts retain their exact current artifact/version; server comparison reports drift separately.

**Actual behavior and impact:** jem_economy:shipping_box cites Economy 1.0.0 and its hash despite provenance from canonical 1.2.0. Canonical and local-server evidence are merged under one owner.

**Established cause / stated uncertainty:** Five whys: row uses old version -> _owner_for resolves shared namespace owner -> inventory keeps first differing artifact and only records later duplicate -> artifact order is filename-sorted across client/server -> canonical identity has no side-priority or per-artifact provenance at row creation.

**Proposed owner correction and native reuse:** Choose canonical enabled client owner explicitly and retain side/version provenance for diagnostic server artifacts. Do not silently make mismatched server authoritative.

**Acceptance:** All jem_economy canonical rows resolve 1.2.0/current canonical hash; server 1.0.0 remains an explicit mismatch.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `jem_economy:shipping_box`, `jem_economy:aether_coin`

**Source/evidence links:**

- [Catalog/generator/build_atlas.py:127](../../../../../Catalog/generator/build_atlas.py)
- [Catalog/generator/build_atlas.py:186](../../../../../Catalog/generator/build_atlas.py)
- [Catalog/registries/items/jem_economy.json](../../../../../Catalog/registries/items/jem_economy.json)
- [Catalog/manifest.json](../../../../../Catalog/manifest.json)
- Canonical lookup for `jem_economy:shipping_box`: [Catalog/registries/items/jem_economy.json](../../../../../Catalog/registries/items/jem_economy.json) (registry identity must be respected).
- Canonical lookup for `jem_economy:shipping_box`: [Catalog/registries/blocks/jem_economy.json](../../../../../Catalog/registries/blocks/jem_economy.json) (registry identity must be respected).
- Canonical lookup for `jem_economy:aether_coin`: [Catalog/registries/items/jem_economy.json](../../../../../Catalog/registries/items/jem_economy.json) (registry identity must be respected).
- Canonical lookup for `jem_economy:aether_coin`: [Catalog/registries/blocks/jem_economy.json](../../../../../Catalog/registries/blocks/jem_economy.json) (registry identity must be respected).

## JEMQA-20261014-0004

**HIGH — Atlas leaks item tags into block/entity records that share a namespaced ID**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T18:59:56.886576+00:00; latest: 2026-10-13T20:30:58.660731+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Query behavior minecraft:wheat; compare block row tags with data tag registry path.

**Expected player experience:** Tag scope reflects the actual Minecraft registry; a wheat crop block is not an item in a bison diet.

**Actual behavior and impact:** minecraft:wheat block row includes alexsmobs:items/bison_breedables and forge:items/wheat alongside block tags. Tag resolution matches bare namespaced IDs across every registry.

**Established cause / stated uncertainty:** Five whys: block row has item memberships -> enrich matches registry_id in all self.tags values -> tag map retains path but matching never filters registry -> same ID can name block and item -> enrichment dropped Minecraft registry identity as part of the membership key.

**Proposed owner correction and native reuse:** Preserve registry dimension while reading and resolving tags, including nested tag expansion only within the same registry.

**Acceptance:** minecraft:wheat blocks contain only block memberships; items contain only item memberships; effects/entities with coincident IDs cannot inherit each other tags.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:wheat`

**Source/evidence links:**

- [Catalog/generator/build_atlas.py:255](../../../../../Catalog/generator/build_atlas.py)
- [Catalog/generator/build_atlas.py:354](../../../../../Catalog/generator/build_atlas.py)
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wheat`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0016

**HIGH — Static Atlas population includes translation/model variants that are not registry IDs**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:34:37.592600+00:00; latest: 2026-10-13T20:30:58.950450+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Run native kubejs dump_registry minecraft:item/block/entity_type; compare exact registry-ID pairs with current Atlas partitions.

**Expected player experience:** Registry coverage denominators represent exact registered objects, with candidates and presentation variants counted separately.

**Actual behavior and impact:** Native initialized server has 13881 items versus 17062 static candidates. Static-only examples alexscaves:alex_meal.desc, alexscaves:cave_book.general and alexscaves:deep_one.reaction_aggressive are tooltip/variant language keys, absent from their runtime registries. Side differences cannot explain these exact examples.

**Established cause / stated uncertainty:** Five whys: registry denominator has nonobjects -> parser derives IDs from every item/block/entity language key and asset model path -> tooltip/variant suffixes are admitted as ResourceLocations -> static evidence does not distinguish definition from reference/presentation -> generator lacks registry-authoritative admission and separate unresolved-candidate accounting.

**Proposed owner correction and native reuse:** Import authorized runtime registry snapshot as registration evidence; retain uncertain static candidates separately and never delete real dot/slash IDs by heuristic alone. Preserve source provenance and version/side distinction.

**Acceptance:** Known tooltip/variant examples disappear from registered-object counts; runtime-only actual IDs are covered with side/version/source; static claims remain explicit and reproducible.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `alexscaves:alex_meal.desc`, `alexscaves:cave_book.general`, `alexscaves:deep_one.reaction_aggressive`

**Source/evidence links:**

- [EVIDENCE/traces/runtime-registry-comparison.json](../../EVIDENCE/traces/runtime-registry-comparison.json)
- [Catalog/generator/build_atlas.py](../../../../../Catalog/generator/build_atlas.py)
- [Catalog/registries/items/alexscaves.json](../../../../../Catalog/registries/items/alexscaves.json)
- Canonical lookup for `alexscaves:alex_meal.desc`: [Catalog/registries/items/alexscaves.json](../../../../../Catalog/registries/items/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:alex_meal.desc`: [Catalog/registries/blocks/alexscaves.json](../../../../../Catalog/registries/blocks/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:alex_meal.desc`: [Catalog/registries/entities/alexscaves.json](../../../../../Catalog/registries/entities/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:cave_book.general`: [Catalog/registries/items/alexscaves.json](../../../../../Catalog/registries/items/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:cave_book.general`: [Catalog/registries/blocks/alexscaves.json](../../../../../Catalog/registries/blocks/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:cave_book.general`: [Catalog/registries/entities/alexscaves.json](../../../../../Catalog/registries/entities/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:deep_one.reaction_aggressive`: [Catalog/registries/items/alexscaves.json](../../../../../Catalog/registries/items/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:deep_one.reaction_aggressive`: [Catalog/registries/blocks/alexscaves.json](../../../../../Catalog/registries/blocks/alexscaves.json) (registry identity must be respected).
- Canonical lookup for `alexscaves:deep_one.reaction_aggressive`: [Catalog/registries/entities/alexscaves.json](../../../../../Catalog/registries/entities/alexscaves.json) (registry identity must be respected).

## JEMQA-20261014-0017

**HIGH — Actual native dynamic objects are absent from the static integration audit universe**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:34:37.598123+00:00; latest: 2026-10-13T20:30:58.970905+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Compare native registry IDs after full mod initialization with static partitions and inspect exact dynamic registration owners.

**Expected player experience:** Pack-wide review can discover dynamic registered objects and choose representative cross-mod integration samples.

**Actual behavior and impact:** Native registry dump finds 359 item IDs, 389 block IDs and 302 entity IDs not in static Atlas. Examples include efn:arc_tachi, immersive_weathering:alexscaves/ancient_leaf_pile, diagonalfences:minecraft/pale_oak_fence and cataclysm:brontes. Actual JEM gameplay exclusion is NOT demonstrated by Atlas absence. Exact Cataclysm 3.31 class inspection identifies brontes as a weapon projectile; it is not presented as a structural boss candidate.

**Established cause / stated uncertainty:** Native dynamic registration is outside existing offline generator coverage, as its docs acknowledge. This becomes a serious audit blind spot if static population is used as the full denominator.

**Proposed owner correction and native reuse:** Extend canonical Atlas ingestion with the authorized runtime snapshot and preserve unresolved properties. Sample dynamic equipment, decoration and bosses against actual JEM class/tag/API rules before recommending gameplay inclusion.

**Acceptance:** Every native registered ID is referenceable with runtime evidence; properties and gameplay inclusion remain unresolved unless proven; no fake 100% manual coverage.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `efn:arc_tachi`, `immersive_weathering:alexscaves/ancient_leaf_pile`, `diagonalfences:minecraft/pale_oak_fence`, `cataclysm:brontes`

**Source/evidence links:**

- [EVIDENCE/traces/runtime-registry-comparison.json](../../EVIDENCE/traces/runtime-registry-comparison.json)
- [Catalog/README.md](../../../../../Catalog/README.md)
- Canonical lookup for `efn:arc_tachi`: [Catalog/registries/items/efn.json](../../../../../Catalog/registries/items/efn.json) (registry identity must be respected).
- Canonical lookup for `immersive_weathering:alexscaves/ancient_leaf_pile`: [Catalog/registries/items/immersive_weathering.json](../../../../../Catalog/registries/items/immersive_weathering.json) (registry identity must be respected).
- Canonical lookup for `immersive_weathering:alexscaves/ancient_leaf_pile`: [Catalog/registries/blocks/immersive_weathering.json](../../../../../Catalog/registries/blocks/immersive_weathering.json) (registry identity must be respected).
- Canonical lookup for `cataclysm:brontes`: [Catalog/registries/items/cataclysm.json](../../../../../Catalog/registries/items/cataclysm.json) (registry identity must be respected).
- Canonical lookup for `cataclysm:brontes`: [Catalog/registries/blocks/cataclysm.json](../../../../../Catalog/registries/blocks/cataclysm.json) (registry identity must be respected).
- Canonical lookup for `cataclysm:brontes`: [Catalog/registries/entities/cataclysm.json](../../../../../Catalog/registries/entities/cataclysm.json) (registry identity must be respected).

## JEMQA-20261014-0022

**HIGH — Roles/Professions retain integrated-server gameplay despite the native singleplayer boundary**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:41:41.789221+00:00; latest: 2026-10-13T20:30:59.099964+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inspect constructors, IdentityPolicy integrated branch and trait/profession handlers; verify a disposable singleplayer world only if runtime permission and matching client are available.

**Expected player experience:** Installed multiplayer-compatible client retains registry/network/presentation without server-owned shared gameplay in native integrated singleplayer, per root instruction.

**Actual behavior and impact:** JemRoles and JemProfessions constructors register command and gameplay listeners unconditionally. IdentityPolicy.resolve treats every integrated ServerPlayer as LOCAL_OWNER; race trait ticks and profession gameplay do not test dedicated-server activity. Owning DEVELOPMENT text separately says Roles/Professions accept integrated owner, so current sources/docs conflict with the root dedicated-only instruction. No singleplayer launch was performed.

**Established cause / stated uncertainty:** Source-confirmed physical/behavioral boundary mismatch. Its historical product authorization is unresolved; do not silently expand or suppress singleplayer gameplay from ambiguous history.

**Proposed owner correction and native reuse:** Reconcile root dedicated-only product contract with the two domain owners. If root instruction governs, bind their common gameplay listeners/commands only on dedicated runtime while retaining client registries/network/screens.

**Acceptance:** A matching installed client joins SMP correctly and integrated singleplayer has no shared-server commands/listeners/gameplay effects; exact race/profession exception, if intended, must be explicit.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [AGENTS.md](../../../../../AGENTS.md)
- [Docs/DEVELOPMENT.md](../../../../DEVELOPMENT.md)
- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/JemRoles.java:20](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/JemRoles.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/JemProfessions.java:20](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/JemProfessions.java)
- [Mods/JEMIdentity/src/main/java/com/siirio/jemidentity/api/IdentityPolicy.java:19](../../../../../Mods/JEMIdentity/src/main/java/com/siirio/jemidentity/api/IdentityPolicy.java)
