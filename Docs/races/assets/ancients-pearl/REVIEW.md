Current dome: [connected native mesh](../guardian-dome/PLAN.md). The independent mosaic has also been rejected and superseded.

# Review — revised sphere ART_CANDIDATE; tail shell USER_REJECTED

LATER USER VERDICT: smooth sphere is also USER_REJECTED as foreign to Minecraft.
Current replacement is [guardian-mosaic](../guardian-mosaic/PLAN.md). This review
and images below are historical evidence; sphere texture/Java are removed from
the active renderer and author recipe. Old sphere preview entry point is retired.

Latest user correction: "the dome is ass"; it must be a solid sphere, not wiggly
tails. The first open-scroll shell is USER_REJECTED. Its exact source, layered
timeline and observed render are preserved in rejected-tails. No active resource
or renderer references guardian_scrolls. Replace it with a fully filled golden
sphere, equal radius on all axes, translucent body and stronger view-dependent rim.
There are no holes, independent ribbons or curled tails. This deliberately uses
the continuous sphere the user requested; a generic anti-primitive rule cannot
override that precise direction.

The corrected neutral-stage projection uses orthonormal camera axes: the previous
preview helper stretched height and would misrepresent a real sphere as an egg.
CPU per-panel alpha still shows more faceting than runtime per-vertex interpolation;
runtime smoothness is NOT OBSERVED. Updated guardian-spatial.png and spatial GIF
show the actual replacement geometry/material under those stated preview limits.

Source: Mods/JEMVisualSandboxR3; requested image 3 afterfire and Ancients remake.
Editable five-layer Aseprite sources and externally authored contour recipe are
beside this file; runtime exports and hashes are listed in assets.json. Export:
`python Docs/races/assets/ancients-pearl/author.py`. Spatial playback:
`python Docs/races/assets/ancients-pearl/preview.py` (existing CPU textured-quad
projection helper; not a Minecraft renderer).

Observed evidence: contact.png at native pixels, guardian-spatial.png and
shields-spatial.png against references/03-void.png and 04-ancestors.png. GIFs
contain actual exported frames; spatial GIF also uses actual R7 mesh quads.
Normal-speed interactive playback and Minecraft appearance are NOT OBSERVED.

- Pearl silhouette has a bright compact neck, three unequal backward tongues,
  detached slivers and lavender/violet value separation. It preserves a native
  teal pearl. Material is sharper and less diffuse than the reference; candidate
  needs actual curved-flight and end-on review. This is not approved final art.
- Initial guardian shell: USER_REJECTED for separated curling tails. The updated
  candidate is a filled continuous golden sphere with a soft body and brighter rim;
  inspect the actual new spatial output. It is not user-approved or game-verified.
- Enlarged existing shield silhouettes remain recognizable and outward-facing;
  .95-block height is nearly twice the earlier .48. They retain the existing kite
  design, not the illustration's circular medallions. No new model approval claimed.
- Decorative ground rune draws are gone. Authored healing front, recipient
  responses, native golem and threat-facing full shield retain their source owners.
- Initial contact-sheet layout overlapped shell and impact rows and distorted
  small impact cells; reopened evidence layout, kept native dimensions and larger
  row spacing. The replacement shows assets without overlap or resampling.

Protected: gameplay healing/knockback/damage/progression/cooldowns, dedicated-server
exclusion, native pearl physics, impact safe-arrival validation, accepted-recipient
events, exact Chorus 100-tick25% reduction and passive30-tick Resistance remain
unchanged. No new packet/protocol/dependency.

Trail history is sampled on client ticks, interpolated at its head, bounded to
16 projectiles /12 samples /2.6 blocks /200 ticks; removal fades within6 ticks.
Level changes and disconnect clear cosmetic state. Waiting for entity spawn is
bounded10 ticks. Impact breakup has its own8-tick envelope.

Build/reobfuscation passed; no tests or gameplay launch. Composed artifact receipt
is delivery.json. No launcher profile is modified: current user rules name
`test jem` for local development, while historical sandbox installer targets a
different profile. The standalone JAR is prepared outside profiles. Ordinary
Server/Client pack generation18 remains unchanged; this is not a pack release.

Next review: throw the pearl along curved paths, collide at nearby walls, check
rapid repeated throws, then inspect Chorus and guardian from first/third person
on bright and dark terrain. These are manual review suggestions, not executed
runtime scenarios or a claim of user acceptance.
