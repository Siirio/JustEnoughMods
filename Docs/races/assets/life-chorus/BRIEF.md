# Life Chorus — current golden reference production

Superseded presentation, 2026-10-17: the user explicitly confirms shields, pulse
and dome with no floor rune. Current [Ancients/pearl brief](../ancients-pearl/BRIEF.md)
removes all decorative rune draws, enlarges the existing outward shields and adds
a brief continuous guardian sphere. Gameplay and existing healing front remain as below.
Earlier installation receipt describes the previous installed sandbox, not the
new prepared artifact.

2026-10-17. Standalone **jem sandbox** only. This supersedes the intermediate
botanical Chorus study and earlier elemental-vfx/FOLLOWUP for revised abilities.
Current mechanics/prompt catalogue: ../../VFX_REFERENCE_BRIEFS.md.

The user supplied references/user-chorus-direction.png and explicitly requested
its inspiration for Chorus. Extract gold rising swirl, ornate ground rune, bright
healing motes, restrained secondary wave and shield dissolution. The printed
three-shield/different-duration captions do not override four outward-facing
shields and5s25% protection. The image's rendered game panels are concept evidence.

## Current choreography

- 0–.35s: small gold curl gathers and opens a local rune. The swirl's upper ribbon
  releases through.70s, overlapping the advancing healing pulse.
- .35–.90s: an ornate gold front expands from the player's cast position to16 blocks.
  It heals eligible allies and knocks each hostile Mob outward once when reached.
  A quieter gold echo follows. Terrain pieces sample exposed collision tops.
- Recipients: actual accepted healing events produce their own rising ivory/gold
  diamond motes, scaled to the subject; no generic orange rods or client guesses.
- .90–5.90s: four miniature actual R7 shield meshes orbit slowly, painted faces
  outward after the explicit180degree correction. Damage amount*.75 for100 ticks;
  no stacking. A restrained local rune connects the protection to its healing origin.
- End: shield alpha fades, then surviving gold motes drift outward/upward and clear.

## Shared outward wave — latest explicit correction

A successful native guardian golem spawn **triggers the wave at the player**.
The golem is not its center and does not emit a repeating aura. Both successful
entry and V call LifeChorusAbility.startOutwardWave; one shared state/update/repel
path expands16 blocks in12ticks and applies native.9-strength knockback once per
hostile, preserving knockback resistance. Golem entry starts immediately; V starts
with its healing pulse at tick7. These16/.6s/.9 settings are Codex choices for the
shared mechanic. Neither wave adds damage. Formation motes on the golem are separate.

The below40% passive still creates one native golem for600ticks and gives30ticks
Resistance V. Healing contract retains playerheal6, full friendly animal/villager/
recruit/worker/golem healing, cleanse and full-health absorption. Hostile mobs are
repelled rather than healed. Exact current other race behavior is in the catalogue.

## Production contract and evidence

Built-in image_gen generated original production sheets for gold_rune, gold_swirl
gold_motes and the separate open-center gold_front. The tool does not expose a model version. These are new purpose-made
sprite cels, not crops from the user's reference. Keep the ornate rune local (up to3.2 blocks) and use the separate open-center
front for the16-block expansion; enlarging the filled ornament obscured terrain.
Retain the original RGBA sheets
in production-sheets. gold-author.py extracts16 native384x256 cels losslessly,
aligns the rune's persistent center with integer offsets, creates editable single-
paint-layer Aseprite timelines and exports384x4096 PNG strips. No scaling,
pixelation filter, alpha thresholding or reference-pixel reuse in runtime exports.
Native3:2 aspect is preserved in the renderer; small source RGB haze with zero
alpha is invisible. Native animation frame mapping is explicit in the renderer.

life-chorus.gif plays these exact exported cels plus the actual textured R7 mesh on
a neutral stage; camera zooms during awakening/orbit. This is **not game footage**.
gold-contact.png and gold-assets.json show frames/contracts/hashes. green-study.gif
and green-study-contact.png retain the superseded agent candidate; it was not
user-approved. Prior orange-bar art remains USER_REJECTED in its labelled archive.

Build/install receipt: installation.json. REVIEW.md records remaining weaknesses.
No tests or game launch. Ordinary Server, clean Client, stable, MineRENT and native
AutoModpack generation18 remain unchanged. The existing sources.zip receives new
native production sheets/timelines alongside the unchanged R7 native shield source.
