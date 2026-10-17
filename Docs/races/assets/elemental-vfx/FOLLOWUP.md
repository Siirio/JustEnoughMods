# Sandbox combat and art correction — 2026-10-17

User direction supersedes the initial handoff: the roots looked like straight
sticks. Keep thick brown bottoms and green leaves, bend/wrap naturally, and give
every branch its own animation. The latest correction explicitly says **do not
make them smoother; make the lower pieces chunkier**. The current model retains
angular Minecraft joints,12 deliberate bends per branch, overlapping contact and
a strong2.1-to0.45 model-unit half-width taper. No smoothed/subdivided tubing is
the current candidate.24 root cubes and12 front/back cutout leaves =168 quads.
The independent tip growth/curl/leaf unfold reaches maturity by0.8s; runtime holds
that pose until release. `roots-native.gif` is actual native viewport evidence.

## Evidence-backed causes and repairs

Each chain follows symptom → wrong state → producer → earlier operation → owner.
All fixes below are **in testing**; source/build/native evidence is not runtime
or user acceptance.

1. Roots fail to prevent attacks → rooted mobs still tick normally → the generated
   canonical root helper only adds Slowness90 → movement debuff never governs
   AI, fuse or teleport state → sandbox root control was missing. `bind` now marks
   hostile targets for90 world ticks. A narrow mapped `ServerLevel.tickNonPassenger`
   injection skips their action/subclass tick, preserving native `baseTick` for
   health, hurt immunity, fire and potion clocks. Forge guards cancel attributed
   attacks, projectile joins, teleports/dimension travel and explosions during
   the bind. No persistent NoAI mutation, heal, invulnerability or target loss
   substitutes for the lock. Expiry is measured by the server world's clock.
2. Stones fail across slopes → placement returns null and removes the wave → the
   floor search only spans+2/-4, and a straight player-height ray rejects rising
   ground → one failed placement terminates every later eruption → sandbox wave
   placement owns the error. Resolve all five placements independently within
   **±10Y of cast height**, nearest exposed collision top first; skip only an
   unsupported placement. Cache those exact accepted positions for model and hit.
   The terrain-following ground strip uses one continuous authored UV field;
   painted fire flanks use continuous UVs too, avoiding tiny repeated wall tiles.
   These flames are presentation, not placed fire blocks or additional damage.
3. Sculk is hidden → decal origin lies inside solid terrain → canonical path points
   are `Vec3.atCenterOf(block)` → the old emission only adds0.02Y → renderer draws
   the flat plane roughly halfway through the block. Resolve actual collision
   tops plus0.04Y on the server, then render the accepted connected surface path.
   This does not alter connected-sculk eligibility or safe traversal destinations.
4. Shield is facing the owner and triggered by the wrong action → the main-skill
   emission owns the shield and uses the old orientation/client offset → an art
   replacement was attached to Life Chorus instead of the canonical low-health
   guardian → missing standalone trait adapter. Life Chorus retains V and gets
   its own gold healing cels. Below40% HP, summon one native Iron Golem, grant
   **30 ticks (1.5s) Resistance V**, and emit the shield toward the nearest threat
   with a180-degree correction. Native full-golem collision checks choose nearby
   supported space. Guardian lasts600 ticks, is invulnerable/player-created and
   yields no drops/XP, as in the local owner. Recovery above40% rearms the threshold;
   one living guardian prevents duplication. Sandbox's unlimited-cast convention
   remains; production's guardian cooldown is not changed.
5. Void mobs attack the old position → their target is an invisible ArmorStand →
   canonical tier2+ explicitly creates a Void Echo at departure and redirects
   nearby enemies to it → sandbox extraction imported that gameplay unchanged →
   the decoy helper, rather than network position sync, owns this behavior.
   Sandbox extraction now omits that decoy. Existing pursuers clear stale walk/look/
   attack memory and navigation, then target the actual teleported player. Only
   previous pursuers/legacy echo targets are refreshed; unrelated mobs are not
   globally aggroed. Canonical production JEMRoles remains unchanged.

## Restored И / physical B abilities

Source evidence: exact local `Server/mods/jem-roles-2.0.0.jar`, canonical
`RaceAbilities`, `RaceTraitRuntime`, `RaceRules`, `LavaBucketUseMixin` and
`TemporaryPathSavedData`. The standalone adapters omit account/cooldown gates,
as previously requested; they retain the following native results:

| Race | И/B | Sneak + И/B |
|---|---|---|
| Ancestors | Information: passive guardian below40%; V heals | Same |
| Void | Hold at least10 ticks, release: pearl at speed1.5; collision revalidates safe arrival within5 blocks, then teleports without vanilla pearl damage |100 ticks of75% gravity reduction |
| Bloom | Toggle3-wide,3-long temporary leaf footholds; only empty dry blocks,800-tick persistent expiry | Same |
| Sculk | Below25 resonance: reveal nearby living entities32 blocks/80 ticks; otherwise consume25 to heal6 and grant400 ticks Absorption II | Same |
| Nether | Preserve the liked fiery three-slash combo | Hold a lava bucket and drink32 ticks; empty bucket and heal5 |

Sculk gets10 resonance per hostile kill, capped100. Actual hold/release packets,
dimension/death/logout/selection cleanup, native temporary-path storage and lava
item-use animation are wired; no invented GUI-only demonstration. Packet protocol3
adds bounded128-point server-authored paths. Release cancels when UI focus is lost.
Changing races does not prematurely erase already accepted root VFX.
Native temporary-path break protection is also retained: tracked leaves cannot
be harvested/replaced before expiry, avoiding removal of a later player leaf at
the same position by the old800-tick expiry entry.

## Art audit and evidence

Reopened I_like183036: directional body/core masses, tapered tongues and broken
release;183623's textured edge hierarchy remains a reference. Brown/green roots
retain the exact model/UV Pair's material separation. Initial new fire-wall cels
were too static; give each tongue its own lean/height rhythm. Thin straight echo
sticks were rejected by the agent; redraw broad broken curved lobes and attached
vein/spark clusters. Reuse the actual owned leaf painting for temporary-path cels,
instead of plain diamond placeholders. No reference pixels were copied.

`author-followup.py` retains nine external layered Aseprite sources, runtime
96px/24-cel strips, moving asset previews and a contact sheet. Ground cut and fire
wall are one connected spatial effect; they remain more regular and less fluid
than183036. Utility effects are smaller supporting cues and do not inherit the
fire combo's approval. Chunky roots are intentionally faceted; attaching them to
very wide/short or very large mobs still requires real game evidence. Strip fitting
on sharp cliffs, final shield facing, AI compatibility and actual utility timing
have not been observed in Minecraft. Neither a successful build nor this critique
certifies final appearance.

Research discovery: relevant winding-root photographs appeared from the
[Taiwan forest collection](https://recreation.forest.gov.tw/Forest/RA?typ_id=0600001)
and [Noosa Landcare](https://noosalandcare.org/plants/argyrodendron-trifoliolatum-white-booyong/).
Direct full-image fetches timed out. They remain discovery proposals, not inspected
full-image evidence, personal-taste labels or sources of artwork. The user's
explicit latest direction and inspected local labelled references govern this work.

## Packaging boundary

Only the standalone `jem sandbox` JAR is replaced, after the user confirmed game
closure. Base classes/assets remain byte-identical; language keys are additive.
The pinned base manifest originally contained only Manifest-Version. The composer
adds the sandbox Mixin config and requires a real nonempty reobfuscated refmap,
because silently discarding the overlay manifest would leave hostile control inert.
Mixins return without gameplay changes on dedicated servers. No game launch,
tests, ordinary Server/Client/ManagedPack changes, AutoModpack generation change,
stable-profile mutation, release-baseline promotion or MineRENT deployment.
