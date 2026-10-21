# Phase 1 visual review

## Review boundary

This review covers neutral geometry and silhouette only. The active exact Taste Engine pair is `Minecraft-cuboid-entity-models`; the target is a generic editable Blockbench study, not a runtime export. Texture, UV quality, rigging, animation, hitboxes, renderer behavior, particles, performance and in-game scale are unobserved and are not marked as passed.

## C0 comparison

- **A — Gullet Oak:** the maw is immediate from the front and the lateral span feels boss-sized. The torso remains a tall rectangular idol, the pumpkin is too box-like and the branch arrangement reads as four repeated arms.
- **B — Crowned Stump:** the crown/head/body hierarchy is easy to parse. Its upright stance and narrow trunk produce the most generic humanoid-tree outline, and the mouth feels attached to the torso rather than built into the creature's locomotion.
- **C — Crawling Crown:** the low four-root stance creates the clearest encounter function, gives the mouth a player-facing height and supports a radial shockwave. C0 was too symmetric, with simple branch ends and a shallow maw.

Decision: refine C once and preserve A/B as alternatives.

## C1 inspection

Observed views: front, right, back, top and three-quarter at 768 px, plus front and three-quarter silhouettes at 96 px.

| Axis | Result | Evidence |
| --- | --- | --- |
| Primary read | Pass for Phase 1 | The open maw dominates the front view; the pumpkin crown is secondary. |
| Silhouette | Pass for Phase 1 | Low crawler stance, wide rooted limbs and branch hooks remain identifiable at 96 px from the front. |
| Geometry/perspective | Pass for direction selection | Large masses are connected and leg depth reads in three-quarter view; the side view is still only a blockout and needs runtime-scale correction later. |
| Deliberate contrast | Partial | Size and negative-space contrast are present, but material/value/color contrast is intentionally absent. |
| Distinctiveness | Pass for comparison | Crooked crown, uneven teeth and asymmetric branches separate C1 from an ordinary tree golem. |
| Texture/UV/material | Unobserved | No production texture or UV review in Phase 1. |
| Motion/rig/secondary motion | Unobserved | No bone hierarchy or animation contract in Phase 1. |
| Runtime/performance/accessibility | Unobserved | No Forge export, entity integration or in-game inspection in Phase 1. |

## Evidence matrix

| Required evidence | State |
| --- | --- |
| Neutral front/side/back/three-quarter/top | Observed for C1 |
| Small-scale silhouette | Observed at 96 px |
| Textured turnaround | Not applicable to this neutral phase |
| Animation extremes | Unobserved |
| In-game distance, lighting and hitbox | Unobserved |
| User taste decision | Pending |

## Verdict

**NEEDS_DIRECTION.** C1 is the agent-recommended Phase 1 geometry candidate and is ready for the user's selection, but it is not user-approved and it is not a production-ready model. No second automatic correction round was spent: the remaining high-impact decision is which silhouette to carry forward.
