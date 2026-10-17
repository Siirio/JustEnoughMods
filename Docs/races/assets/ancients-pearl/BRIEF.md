# Ancients and pearl afterfire — 2026-10-17

Scope: existing standalone sandbox renderer. The pasted overhaul is context;
this request selects image 3's upper-right pearl afterfire and remakes Ancients
VFX. It does not authorize its standalone architecture, UI or balance proposals.

Reference originals: references/03-void.png and references/04-ancestors.png.
Extract the pearl's compact bright lavender head, unequal violet tongues tapering
backwards and sparse detached fragments. Keep the native teal pearl visible.
For Ancients use gold/ivory, a strong shield silhouette, readable recipient
response, and a continuous protective sphere. No botanical material. User explicitly
confirms no floor runes, shields, pulse and dome. A travelling healing front remains.
The first curling-tail shell was subsequently USER_REJECTED: the requested dome
must be a solid sphere, not separated ribbons or tails.

## Choreography and assets

- Pearl: native entity accepted by server, action 28; sample its actual positions
  once per client tick, at most 12 samples per projectile and 16 projectiles.
  A 96x32 eight-cel layered afterfire material runs from the interpolated head
  backwards over that history, up to 2.6 blocks. No predicted/straight-line beam.
  Camera-facing ribbon width .42 blocks; detached forks are painted in its alpha.
  Stop emission on removal and fade history within 6 ticks; clear on level change.
- Impact: action 29; separate 32x32 eight-cel violet breakup, .7 blocks, 8 ticks.
- Chorus: existing 0–14-tick golden rising curl; tick 7–30 travelling golden pulse
  and softer echo; each actual healed recipient retains its accepted motes.
  Remove all gold_rune draws. Tick 18–118: four outward R7 painted shield meshes,
  each .95 blocks high at radius 1.45 and height 1.05, then gold dissolution.
  Exact existing 100-tick damage amount*.75 remains server-owned.
- Guardian: action 35 already marks successful golem creation. The same player
  anchor gets a brief continuous golden sphere: 128x64, eight cels, two ticks per cel,
  30-tick envelope matching existing emergency Resistance.
  Surface is fully filled gold with restrained glints, faint body and brighter rim;
  no missing panels or tails. 32x16 spatial panels, equal-axis radius1.45, center
  1.35 above the player. No new gameplay state. Existing full-size
  threat-facing shield and golem formation remain.

Layers: shadow fringe, colored body, bright flow, ivory core, detached pieces.
Nearest pixels, RGBA alpha, no blur or copied reference pixels. Aseprite sources,
runtime PNG strips and neutral dark/light moving playback accompany the candidate.
No gameplay launch, automated test or runtime visual acceptance is implied.

Production choices (dimensions, bounds and art poses) are Codex decisions. User
approval of the reference motif does not approve these produced assets.
