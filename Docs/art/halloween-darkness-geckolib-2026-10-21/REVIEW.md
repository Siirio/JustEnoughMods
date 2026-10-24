# Visual review

Status: `NEEDS_DIRECTION` after C0 plus two bounded automatic correction rounds. This is the best current editable candidate for user inspection, not claimed user approval or runtime readiness.

## Evidence inspected

- Primary positive: `reference-user.png`, opened at original resolution before modelling and reopened against C2.
- Candidate neutral views: `renders/c2-front.png`, `c2-three-quarter.png`, `c2-side.png`, `c2-back.png`.
- Candidate motion poses: `c2-idle.png`, `c2-walk-a.png`, `c2-walk-b.png`, `c2-bite-open.png`.
- Pixel source/export: `textures/candy_king_atlas.aseprite` and `textures/candy_king_atlas.png`.

## Audit

| Axis | Result | Observation |
| --- | --- | --- |
| Brief and noun fidelity | PASS | The tall square-headed pumpkin skeleton, long limb ratio, ribs, rooted extremities, back growth and dominant abdominal candy maw read in front and three-quarter views. |
| Tool and pixel provenance | PASS | Geometry/rig/animation are native Blockbench data. The 128 x 128 texture was painted at final size in layered Aseprite and directly exported. |
| Silhouette and hierarchy | PASS | Pumpkin, ribs and abdominal maw form the three dominant reads; hands and root feet remain subordinate. |
| Geometry, perspective and contacts | FAIL | C2 improves the disconnected C0 branch blocks, but the upper branches remain visibly stepped and less organically hooked than the reference. The mouth frame is also more rectangular in side view than the swollen ring in the reference. A new user-directed revision is required because the automatic C2 limit is spent. |
| Values, color and materials | PASS with limit | Orange pumpkin/glow, dark wood, grey ribs, wine-red flesh and black cavity separate clearly. Material painting is intentionally broad and quieter than the reference's dense bark/shroud detail. |
| Pixel clusters and UV | PASS with limit | The focal pumpkin face is a connected native pixel design and atlas regions remain crisp. Reused limb regions are deliberate, but this study does not yet provide unique wear for every branch and bone face. |
| Motion and spatial flow | PASS for sampled editor poses | Alternating walk contacts, delayed arms/hands, quiet idle and independently opening maw are visible in the sampled poses. Real-time playback timing was not observed in Minecraft. |
| Export and actual use | UNOBSERVED | The `.bbmodel` uses GeckoLib format and named articulated bones, but no runtime JSON/controller/export or Forge integration was requested or exercised. |

## Revision record

- C0: established the upright proportions, rib cage, long arm/leg chains, abdominal maw and editor texture.
- C1: removed the overbuilt face geometry, rebuilt disconnected branches as connected angular paths, enlarged root feet and authored the three clips.
- C2: repainted the pumpkin expression to the sharper reference read, embedded the updated atlas and reduced the branch staircase with controlled segment angles.

No game, build, automated test, validator, linter or static-analysis task ran.
