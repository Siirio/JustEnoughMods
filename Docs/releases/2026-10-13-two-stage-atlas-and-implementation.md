# Two-stage Registry Atlas and implementation

Status: source pass recorded, gameplay acceptance **in testing**. No Gradle task, launch, automated test, linter, formatter, static analysis, MineRENT deployment, active profile change, AutoModpack publication or release-baseline promotion was performed.

## Stage A — reproducible evidence

`Catalog/generator/build_atlas.py` rebuilds the atlas from the canonical managed pack, the read-only local server comparison, vanilla 1.20.1 archives, JEM source and pack data. `Catalog/generator/validate_atlas.py` checks schema, partitions, references, hashes and summary counts. The current static snapshot contains 534 artifact paths, 17,062 items, 12,133 blocks, 1,593 entity types, 436 effects, 218 enchantments, 264 biomes, 520 structures, 55 creative tabs and 22,984 recipes.

Runtime-only registrations, stack/NBT-dependent food and attributes, entity AI semantics and absent private villager resource-pack rules remain explicit unresolved boundaries. Zero statically observed fluids and block-entity types is not asserted as a runtime absence. Market policy remains fail-closed for unreviewed entries instead of manufacturing prices from namespace or recipe distance.

## Stage B — implemented owners

- Settlements persist bounded six-day unloaded production obligations. A verified Workers Farmer consumes four real wheat seeds per simulated day and can deliver up to eight wheat per day only into an explicitly linked loaded container. Pending, blocked and quarantined output is never stock.
- Every native edible item can derive a localized settlement value from its real food properties; reviewed overrides win, harmful effects are penalized and the server synchronizes the reviewed policy revision.
- All ten professions use an explicit branch/rank/dependency graph. Unsupported native effects stay visibly unavailable. Architect construction XP is bounded and debounced per real placement.
- TinyInv 1.1.0.1 is the verified native owner of a 13-slot hotbar. A level-20 Architect receives four extra slots through its synchronized attribute and validated direct-slot packets. Expanded reach, area placement and a 126-slot reserve were not invented because no approved native contract exists.
- BackToBed and its compatibility patch were removed. `/home` uses the last valid vanilla bed or charged respawn anchor with identity, recent-damage, cooldown, active-event, claim/safety and collision validation.
- Holiday activities retain only the approved New Year physical gift escrow. Mount records retain real roster/evidence while new milestone rewards and the placeholder quest were removed.
- A persistent native Villager Market Buyer displays two current demand categories and real reviewed buy offers. `/market` is read-only; physical delivery remains owned by Shipping Boxes or supported merchants. Existing discounted `MerchantOffer` cost remains authoritative.
- Settlements persist non-protective square guidance territories. Owners can choose radius 32–256; overlap is rejected, registered villagers are guided by native navigation, and no chunks are forced or entities teleported.

## Matching release artifacts

Activation requires matching newly built client/server artifacts for JEM Economy, Professions, Settlements, Server and Compat, plus the exact canonical TinyInv JAR (`tinyinv-1.20.1-1.1.0.1.jar`, SHA-256 `C014733C81D6363ED078208A29CA5A12C4D924F8B770CFE0C842597C28C2F0BA`). It is the official CurseForge file 5234306 (`https://www.curseforge.com/api/v1/mods/455299/files/5234306/download`); its embedded metadata declares the MIT License and both-side Forge 47 / Minecraft 1.20.1 compatibility. Settlement network revision 3 and Professions network revision 2 require matching clients. BackToBed must be absent on both sides.

## Manual acceptance boundary

An explicitly authorized future `test jem` session should verify migration of existing towns/professions, four-day 16-seed → up-to-32-wheat materialization and retry states, food tooltips, hotbar slots 10–13 across wheel/container/death/reconnect flows, `/home` rejection and safe arrival cases, Market Buyer persistence/caps, and territory navigation without claims or chunk tickets. These checks are intentionally not claimed by this source-only pass.
