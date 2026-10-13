# JEM Registry Atlas

This directory is the reusable, machine-readable inventory for the canonical Forge
1.20.1 pack at the `snapshot_sha` recorded in every envelope. Canonical definitions
live under `registries/`; `views/` and `graphs/` are derived indexes and must not
become competing owners.

The permanent behavior model is:

```text
Original Registry -> Applied JEM Changes -> Effective Gameplay Behavior
Catalog/registries    Catalog/changes        Catalog/effective
```

`changes/records/` is the single version-controlled owner for JEM modifications,
their selectors, before/after behavior, cause, rationale, history and declared
indirect effects. `effective/index.json` is a compact generated index; it never
copies or overwrites original registry facts. A tag, namespace, category, source
set or whole registry is recorded once, with exceptions, rather than expanded into
redundant per-ID records. Exact IDs remain appropriate when their behavior differs.

## Mandatory maintenance workflow

Every gameplay task must load `.agents/skills/jem-registry-keeper/SKILL.md` and use
this order:

1. Query the current base record and all applied rules.
2. Run the working-tree impact scan before implementation.
3. Change the canonical owning source.
4. Add or update a `changes/records/*.json` record, including same-task history and
   indirect systems. Use `SELF` for the current commit; queries resolve it from Git
   history after commit. Unknown historical values stay explicit `null`/unresolved.
5. Rebuild only the compact effective index, scan coverage again, and validate.

```powershell
python Catalog/generator/query_atlas.py behavior cataclysm:the_incinerator
python Catalog/generator/maintain_atlas.py scan
python Catalog/generator/maintain_atlas.py rebuild
python Catalog/generator/maintain_atlas.py audit
python Catalog/generator/maintain_atlas.py scan
python Catalog/generator/validate_atlas.py
```

The scan reads `changes/source-watch.json`. A relevant changed Java, Mixin, KubeJS,
datapack, recipe, loot, config, JAR or dependency path is covered only when a change
record that names it is modified in the same diff. Runtime `Server/` files are not
canonical inputs and are intentionally ignored.

Canonical client JARs may be Git-ignored, so the scan also hashes
`Client/ManagedPack/mods/*.jar` against `manifest.json`. Added, removed or changed
artifacts make the scan fail with `full_regeneration_required`; run the existing full
generator, then review/update affected change records and effective behavior.

`effective/baseline-coverage.json` inventories every currently tracked canonical
source matched by that watch policy. Historical files without a reviewed semantic
record are listed as unresolved; this is deliberate and prevents the initial Atlas
from pretending that unreviewed existing code has been understood. Future modified
files cannot remain in that state because the same-diff scan fails uncovered work.

## Change records and precedence

The canonical contract is `changes/schema.json`. Selectors support exact IDs,
namespaces, registries (`registry:blocks:*` is represented as a `registry` selector),
tags, reviewed categories and source-backed ID sets. Exceptions use the same shape.
Rules apply in ascending numeric priority, then ascending selector specificity, so
the later/more-specific modifier wins. Equal-order modifiers of the same property
remain visible conflicts; they are never silently overwritten. `overrides` records
an intentional relationship but does not delete either rule or its history. A rule
may list reusable parent change IDs in `inherits`; parents apply before the matching
child, inheritance cycles or missing parents fail validation, and overlapping broad
plus exact selectors remain independently visible.

Source-backed selectors preserve different per-ID values in their existing owning
file. For example, all reviewed economy policies are linked once to the canonical
JEM Economy policy JSON, while a query emits the matched member values and direct
recipe consequences for the requested item.

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
python Catalog/generator/query_atlas.py behavior minecraft:diamond
python Catalog/generator/query_atlas.py impact minecraft:diamond
python Catalog/generator/query_atlas.py role mountable
python Catalog/generator/query_atlas.py trade NONE
python Catalog/generator/query_atlas.py food-negative
```

Each per-mod file is a stable envelope containing schema version, source snapshot,
generation method, owner metadata and a sorted `records` array. Registry IDs are
lower-case namespaced IDs. A future schema change increments `schema-version.json`;
generators must keep a migration note and must not silently reinterpret old fields.

`behavior` answers what modifies an object, how, why, from which source/history and
commit, in which precedence order, and which conflicts remain. `impact` gives the
compact dependency view, including recipes that consume or produce an item and the
declared affected systems. These lookups read existing partitions and never require
a complete Atlas regeneration.

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
