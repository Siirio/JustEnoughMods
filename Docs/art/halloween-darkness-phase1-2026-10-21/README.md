# Halloween Darkness — Phase 1 Candy King model studies

Phase 1 is a sandbox-only silhouette and massing study for the Candy King. It does not add an entity, registry ID, hitbox, animation, texture, sound, renderer, packet or runtime resource. The editable `.bbmodel` files use Blockbench Generic format so the direction can be changed before a runtime owner is selected.

## Direction brief

The supplied package makes the mouth the encounter's main interaction point and shows the boss as a pumpkin-crowned tree/candy creature. These studies therefore prioritize:

- a huge readable mouth near player height for feeding and candy-vomit staging;
- a heavy rooted body that can sell a ground shockwave;
- a pumpkin crown/head that remains distinct from the mouth;
- broad connected masses and deliberately uneven branch shapes rather than a normal humanoid;
- a silhouette that survives Minecraft viewing distance and a 96 px thumbnail.

No source image pixels were copied into the models. The neutral lavender renders intentionally defer palette, material separation and surface detail to a later Aseprite texture phase.

## C0 alternatives

| Candidate | Shape hypothesis | Phase 1 result |
| --- | --- | --- |
| A — Gullet Oak | Wide, squat wooden idol with four branch arms and a central maw | Mouth reads, but the vertical box body feels too much like an altar and too little like a mobile boss. |
| B — Crowned Stump | Taller royal stump with a raised pumpkin head | Clear hierarchy, but the familiar upright monster silhouette is the least distinctive. |
| C — Crawling Crown | Low four-root crawler with the maw carried between its forelimbs | Strongest encounter silhouette and best base for shockwave staging; selected for one refinement pass. |

## C1 selected refinement

`candy-king-c-crawling-crown-c1.bbmodel` is the current recommended direction. C1 deepens the maw, varies tooth size, shifts the pumpkin crown off-center, adds asymmetric branch hooks, mantle plates, outer claws and back spines, and keeps the weight low over four rooted limbs. The 96 px front silhouette remains recognizable.

This is an agent recommendation, not user approval. All three C0 files remain available and nothing was deleted or overwritten.

## Evidence inspected

Primary user-supplied references from `Halloween_Darkness_Package_v2.zip`:

- `05` — Candy King concept;
- `08` — encounter staging;
- `09` — candy-vomit presentation;
- `10` — shockwave presentation.

Taste Engine form references inspected for Minecraft-scale construction and readable connected mass:

- `REF09` frog/float;
- `REF13` creature heads;
- `REF19` crystal dinosaur;
- `REF20` green dinosaur;
- the approved goblin model preview.

External search results were treated only as unclassified structural proposals, not as user taste or production assets. The [MCModels Pumpkin Pack](https://mcmodels.net/products/10456/pumpkin-pack) and [Ent Pack](https://mcmodels.net/products/11019/ent-pack) helped confirm that pumpkin and tree motifs need a more specific combined silhouette; no geometry, texture or asset was copied.

## Files

`models/` contains editable A/B/C C0 prototypes, the refined C1 prototype, large orthographic studio renders and two 96 px silhouette checks. The review boundary and verdict are recorded in `REVIEW.md`.

## Next phase after direction approval

1. Choose A, B, C1, or request a hybrid.
2. Convert the selected massing study into a runtime-targeted bone hierarchy and define the actual hitbox/scale contract with the owning Forge entity.
3. Author the external pixel texture in Aseprite; do not paint production pixels inside Blockbench.
4. Review neutral, textured and animated turnarounds before any sandbox registration.

No Client, Server, MineRENT, stable profile, managed-pack generation or release baseline is changed by this phase.
