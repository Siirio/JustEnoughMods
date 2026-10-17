# Reference comparison and revision record

Latest revision and actual remaining criticism: [FOLLOWUP.md](<FOLLOWUP.md>).
The user rejected the first straight-stick root silhouette. Later explicit
direction keeps angular bends and strengthens the lower chunks rather than
making smoother tubing. Current native evidence and exports supersede the
84-quad initial wrap; the revised model has168 quads and distinct branch tracks.

## References and taste scope

Exact Pairs: Minecraft-cuboid-entity-models, Minecraft-pixel-uv-and-tiling,
and Forge-1.20.1-jem-race-abilities. Existing I_like183036 supplies tapering
directional masses;183623 supplies stepped textured contours, intentional gaps
and core/edge hierarchy. These are stills, so they do not prove any timing.
User feedback explicitly accepts the preceding fiery slash R5 appearance;
that approval does not extend to these new elemental candidates.

Vanilla grounding: brown woody root material and green foliage rather than
uniform emissive green tubing. Relevant official context:
[mangrove roots and sculk](https://www.minecraft.net/en-us/article/minecraft-snapshot-22w11a),
[root systems](https://www.minecraft.net/ja-jp/article/block-week--rooted-dirt).
These are technical/subject references, not newly inferred user taste or copied
production textures. The exact installed1.20.1 renderer was inspected with javap
before using its actual End portal render type and textures.

## Rejections and repairs

1. Agent rejects the initial green uniform vine material and rectangular leaf
   plates after the user's direct correction. Material identity was wrong and
   plate silhouettes did not communicate leaves. Repaint external root ORA/PNG
   as warm brown bark with lengthwise grooves; paint a separate pointed green
   leaf with asymmetrical shading and vein. Increase lower root thickness and
   reduce it along the climb. Do not add glow to conceal the failure.
2. Native review reveals thin green perimeter artifacts caused by mapping the
   complete leaf texture onto compressed thickness faces. Remove those four
   faces; keep front/back cutout artwork. Re-export actual native posed geometry,
   reducing108 to84 quads. This is a geometry/UV repair, not a screenshot retouch.
3. Initial Bloom sprite study is abandoned after the user's model-wrap correction;
   remove it from source/export/package. It is not retained as an active fallback.
4. Agent revises Sculk's thin cyan-pipe study into broader dark tissue and broken
   attached cyan accents. Current movement travels from the trunk into unequal
   branches, then erodes. It remains visually simpler than I_like183623.
5. User requires each branch to have its own wrapping animation. Replace whole
   coil scaling with12 separately rigged, sequentially growing tips, opposite
   curling/settling, staggered branch timing, six independent leaf-unfold tracks
   and top-first release. Remove visible premature tiny root fragments. Native
   motion contact and25 captured poses show distinct growth fronts around the body.
6. User requests Nether-hearted stones painted like magma: glowing orange top
   fading to very dark bottom. Replace the grey basalt materials with external
   layered thermal painting and deliberately placed cooled crust/fracture lips.
   Map all side faces by the same whole-model vertical ramp; low plates stay
   dark. Actual native eruption capture confirms warm upper masses and near-black
   bases. Fullbright preserves the painted heat without adding world lighting.

## Harsh current audit

- Roots: clear brown/green material separation and visible base-to-tip weight.
  The angular two-coil silhouette still looks constructed from straight sticks;
  it is not convincing organic wrapping in every view. Native/editor evidence
  establishes the model and animation, not combat fit around arbitrary mobs.
  Dynamic AABB scaling is implemented but has not been observed in Minecraft.
  Avoid claiming finished quality solely from this repaint.
- Rift: tapered opening and closing read correctly in cel playback, with a broad
  center rather than a circular marker. Its rim is much sparser than183623's
  layered turbulent edge. The real End shader is source-integrated, but the
  neutral preview cannot establish its game appearance or shader-pack behavior.
- Sculk: distinct from the rejected green rings/links and from the old cuboid
  crawl. It is still a relatively simple branching decal; repeated path patches
  may be noticeable, and floating on uneven terrain is an unresolved runtime
  concern. More cyan particles would not fix those material/spatial limitations.
- Eruptions: three unequal fitted basalt/magma clusters and a timed outward
  sequence satisfy the asset/choreography brief. They remain cuboid-heavy and
  need actual ground-contact, occlusion and camera evidence before visual approval.
- Fire slash: preserve the user-liked R5 bytes and motion. This task changes its
  binding only. No retrospective claim that the new effects share its approval.

Status: sandbox candidates installed for user inspection; neither build success,
MCP use, source completeness nor this audit marks the new art USER_APPROVED.
