# Web-researched mechanics seed: Forge 1.20.1

**Date:** 2026-10-21. **Scope:** first source-backed batch only; not a completed universal encyclopedia. **Gameplay changes:** none. **Build/runtime tests:** none. **Data inputs:** current `Catalog/manifest.json` plus official author/project pages, official/historical wikis and explicitly labelled community guides.

## Included installed mods

| Mod ID | Manifest version | Source type | Main high-value mechanic families |
| --- | --- | --- | --- |
| `alexscaves` | 2.0.2 | community wiki, version caution | cabin → tablet → spelunkery → codex → cave map |
| `unearthed_journey` | 2.2.3 | author description for released version | Temple → Methuselah → Eye → portal, peat, Leptictidium, metallurgy |
| `skarrier_mobs` | 1.1.1 | author descriptions and release notes | combat telegraphs/counters, coatings, Wretch optional integration |
| `exposure` | 1.9.21 | author page and historical wiki mirror | camera/film → develop → print → album, conditional compatibility alerts |
| `irons_spellbooks` | 1.20.1-3.15.6 | developer wiki | scroll → inscription, scroll forge, arcane anvil, weapon imbuement |
| `create` | 6.0.8 | author and historical official wiki | kinetic network, stress, mechanical press, basin compacting |
| `spawn` | 4.0.7-1.20.1 | author page + installed static localization | worldgen, animal interactions, Jade/JEI/datapack integration |
| `block_factorys_bosses` | 2.1.2 | developer and release notes | multiphase arenas, Kraken ship, historical regression note |
| `aquamirae` | 7.1.13 | developer description | Ice Maze exploration, Captain Cornelia |
| `legendary_monsters` | manifest metadata `1.20.1` (file is 2.2.2 MC 1.20.1) | developer & file page | mid/late encounter scope only, exact mappings unresolved |

## FTB Quests quality gate

- Prefer a chain of observable skills over reading-only nodes: **find a temple → defeat the boss → obtain eye → complete portal → visit dimension** (Unearthed Journey); **acquire a tablet → translate → locate biome** (Alex's Caves); **take a real photo → develop → print → make an album** (Exposure).
- Item existence can often be checked by FTB inventory tasks; crafting, defeating a particular mob, visiting a structure, operating a machine, or completing a camera workflow may require **a custom criterion**. Never mark a player-action quest complete just because a GUI was opened or an item was clicked with a checkmark.
- `quest_seed` entries are **design leads**, not already implemented `.snbt` quests; title and task wiring must be localized and reviewed against Forge 1.20.1 and JEM hosted boss rules.

## Compatibility/logic cautions grounded in sources

- Skarrier's Wretch uses an Alex's Caves integration according to the 1.0.8 release; Alex's Caves is present in the pack manifest. Its biome/spawn events still require exact installed bytecode/config verification before using a kill/locate criterion.
- Exposure's author warns about Exordium viewfinder flicker and Visual Overhaul camera-control crashes. **Neither is listed as an enabled mod ID in the snapshot**, so these are conditional research advisories, **not discovered JEM incompatibilities**.
- Skarrier's published Breacher/Bowlder damage and timing values can be changed by JEM progression or future mod releases. Do **not** use an upstream statement as the effective damage in `Catalog/views/power`.
- Bosses'Rise's older 2.0.13 release fixed a phase-two invisible Ashlord problem. Since JEM uses 2.1.2, this is a regression note, **not** an assertion the defect survives.
- Some old community guides contradict the native Alex's Caves wiki regarding tablet loot and progression; this PR uses directly linked higher-quality sources and leaves exact probabilities unresolved.

## Next enrichment priorities (not covered here)

1. Complete installed-version runtime registry capture and remove false ID classifications **only after verification**.
2. Extract exact spawn requirements and AI behavior for Spawn, Skarrier Mobs, Unearthed Journey, Legendary Monsters, Aquamirae, Cataclysm and Bosses'Rise.
3. Integrate authoritative FTB Quest event/criterion IDs and JEM boss and raid lifecycle signals, avoiding duplicate ownership.
4. Expand the web research source layer through each uncovered mod ID from `coverage`. Add sources to source-layer files; add canonical JEM modifications to `changes` only.
5. Generate an explicitly *derived* relationship graph from vetted source and runtime facts (not from copied wiki text alone) and run reachability checks for progression.

This set contains **52 external research observations across 10 mod IDs**; it is not 52 verified runtime mechanics. Counts are measured by the included JSON files, not by the number of statically discovered Atlas objects.

## Repository integration

The original research snapshot remains recorded as
`e5319721b683948860017b19113db7647452a890`; integration does not replace its
provenance with a newer commit. The reader validates the full JSON Schema,
provenance, references and graph before returning records, warns about version
and snapshot drift, and includes embedded dependencies in coverage. Its tooling
dependency is declared in `Catalog/research/requirements.txt`.

The archive's apply commands and research-agent prompt are reference documents.
They do not authorize builds, gameplay changes, launches or deployments. No
external observation is promoted to the canonical registry or effective layer.

Integration validation against the `main` catalog and these changes:

- Research audit with `--check-registry`: 10 mods, 52 observations, 20 edges,
  23 source declarations, zero errors and zero warnings.
- Coverage: 349 distinct installed IDs including embedded dependencies; 339
  remain unresearched. The original 291 count omitted embedded-only IDs.
- `find skarrier_mobs`, `quests exposure`, `graph` and filtered graph queries
  return 9, 5, 20 and 2 records respectively.
- Existing `validate_atlas.py` passes against the committed canonical catalog
  with its referenced sources and locally installed artifacts available.
- All 20 local client/server artifacts belonging to the 10 reviewed mods match
  their catalog SHA256 values, including Exposure 1.9.21 and Skarrier 1.1.1.

The working checkout also contains unrelated, concurrently changing catalog and
gameplay work. It is excluded from the PR; its generated indexes are not rebuilt
by this integration. No Gradle build, automated tests, Minecraft launch, runtime
verification, AutoModpack publication or MineRENT deployment was performed.
