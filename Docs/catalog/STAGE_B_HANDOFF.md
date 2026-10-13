# Registry Atlas → implementation handoff

## Snapshot and evidence

- Repository HEAD: `f3bd0fcc177743d0c765d811b7c9b48126fc29e6` on `main`.
- Platform: Minecraft 1.20.1, Forge 47.4.16, Java 17.
- Inputs: canonical `Client/ManagedPack`, read-only local `Server` comparison,
  vanilla unpacked 1.20.1 server archive, JEM source and pack data.
- Manifest contains exact SHA-256 and side presence for 534 artifact paths.
- Gameplay precedence: MD3 > Focused > Playability. Engineering ownership and
  locked behavior remain governed by `AGENTS.md`, `AGENTS.logic-locks.md` and
  `Docs/DEVELOPMENT.md`.

## Observed canonical ID counts

| Registry | Statically observed IDs |
| --- | ---: |
| Items | 17,062 |
| Blocks | 12,133 |
| Entity types | 1,593 |
| Effects | 436 |
| Enchantments | 218 |
| Biomes | 264 |
| Structures | 520 |
| Creative tabs | 55 |
| Fluids | 0 (runtime boundary) |
| Block entity types | 0 (runtime boundary) |

The generator indexed 22,984 recipe JSONs. Derived static views currently contain
9,586 item/block placeable joins, 1,593 entity candidates, 42 reviewed food-policy
records, 379 weapon-name candidates, 261 armor-name candidates and 75 tool-name
candidates. The 17,062 item count explains why an unqualified report of ~19.2k
tradeable and ~19.5k unavailable rows cannot describe canonical registered item IDs;
it likely counted JEI presentations/subtypes or another generation. No target count
was manufactured.

## Read paths

- Owner/JAR identity: `Catalog/manifest.json`.
- Per-registry/per-mod records: `Catalog/registries/<registry>/<mod>.json`.
- Foods, mobs, placeables, equipment and trade policy: `Catalog/views/`.
- Recipes and tags: `Catalog/graphs/`.
- Coverage and runtime gaps: `Catalog/coverage.json`, `Catalog/unresolved.json`.
- Quick lookup: `python Catalog/generator/query_atlas.py ...`.
- Regenerate/validate: `Catalog/generator/build_atlas.py` and
  `Catalog/generator/validate_atlas.py`.

## Highest-priority unresolved boundaries

1. A Minecraft/Forge runtime registry bootstrap was not authorized. Static evidence
   cannot prove dynamically registered IDs or prove their absence. Stage B may use
   verified domains now and must keep unknown progression-critical items fail-closed.
2. Stack-dependent max size, food properties/effects, attributes, capabilities and
   NBT variants need runtime or targeted installed-bytecode overlays. Null fields are
   deliberate, not defaults.
3. Living/entity AI roles, health, tame/ride contracts and boss relationships need
   targeted source/bytecode review before gameplay code relies on them.
4. Canonical `Client/ManagedPack` did not provide the user's private villager
   resource-pack texture rules. A compatibility seam may be implemented, but visual
   variants cannot be fabricated or redistributed.
5. The client/server comparison exposes old `jem_economy` 1.0.0 vs 1.2.0 and
   `jem_settlements` 1.0.0 vs 2.0.0 artifact pairs. Stage B source/network changes
   therefore require matching newly built client and server JARs at the later release.
6. BackToBed records remain in the Atlas because the task was forbidden from mutating
   the local server runtime. They are diagnostic evidence from that read-only side,
   not membership in the updated canonical managed client.

## Stage decision

Proceed with Stage B for verified source-owned systems and reviewed catalog overlays.
Do not block `/home`, profession graph migration, food policy sync, settlement journal,
holiday/mount cleanup or Back-to-Bed removal on unrelated third-party unknowns. Keep
unsafe market entries `NONE`; do not infer tradeability from namespace or recipe
distance alone.

Stage A did not deploy, mutate a world, modify the stable launcher profile, publish
AutoModpack or promote the release baseline.

## Implemented Stage B result

The source pass implements durable physical settlement production obligations,
native-derived food policy tooltips, the explicit profession dependency graph,
verified TinyInv-backed Architect hotbar expansion, JEM-owned `/home`, gift-only
holiday cleanup, evidence-only mount tracking, the Market Buyer bulletin and soft
settlement territory guidance. Exact decisions, matching artifacts and the manual
acceptance checklist are recorded in
`Docs/releases/2026-10-13-two-stage-atlas-and-implementation.md`.

Unsupported Architect reach, area placement and 126-slot reserve remain unavailable;
no safe installed native contract was found, so the implementation does not expose
no-op buttons or fabricate inventory authority.
