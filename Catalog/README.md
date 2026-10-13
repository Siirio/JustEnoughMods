# JEM Registry Atlas

This directory is the reusable, machine-readable inventory for the canonical Forge
1.20.1 pack at the `snapshot_sha` recorded in every envelope. Canonical definitions
live under `registries/`; `views/` and `graphs/` are derived indexes and must not
become competing owners.

## Evidence boundary

The generator reads the exact JARs in `Client/ManagedPack/mods` and `Server/mods`,
the vanilla unpacked server archive, JEM source registrations, recipes, loot, tags,
language files, blockstates, models and worldgen JSON. It does **not** launch Forge.
Therefore `coverage.json` claims complete coverage only for IDs observed in those
static sources. Dynamic registrations, Java-only FoodProperties, ItemStack/NBT
variants, capabilities and entity AI are recorded in `unresolved.json`; null means
“not proved”, never zero or false.

## Regenerate and validate

From the repository root:

```powershell
python Catalog/generator/build_atlas.py
python Catalog/generator/validate_atlas.py
```

These are offline data-generation checks, not gameplay tests, Gradle builds, lint or
Minecraft launches. Output is deterministic apart from `generated_utc`.

## Direct lookup

```powershell
python Catalog/generator/query_atlas.py id minecraft:diamond
python Catalog/generator/query_atlas.py role mountable
python Catalog/generator/query_atlas.py trade NONE
python Catalog/generator/query_atlas.py food-negative
```

Each per-mod file is a stable envelope containing schema version, source snapshot,
generation method, owner metadata and a sorted `records` array. Registry IDs are
lower-case namespaced IDs. A future schema change increments `schema-version.json`;
generators must keep a migration note and must not silently reinterpret old fields.

## Trade and acquisition semantics

`BUY_AND_SELL` permits both system-market directions. `NONE` permits neither, but
does not prohibit player-to-player transfer. Acquisition class is independent of
trade outcome. Unknown progression-critical items fail closed. Ordinary generic
resources do not inherit protection merely because a protected recipe uses them.

## Current limits

The Atlas is immediately useful for exact ID ownership, static recipe/tag/worldgen
queries and reviewed JEM overlays. Do not use an inferred field as runtime truth.
Run an explicitly authorized isolated registry bootstrap before claiming every
dynamic Forge registry ID or every stack-dependent semantic field is resolved.
