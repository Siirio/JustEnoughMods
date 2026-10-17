# Race VFX reference prompts — current standalone sandbox

2026-10-17. Mechanics below come from Mods/JEMVisualSandboxR3's source and explicit
user decisions. This describes the standalone integrated owner, not the production
pack's progression/cooldowns. Raw damage values are before applicable native
mitigation;2 damage =1 heart. Current implementation is not live-game acceptance.

Use this document to generate **inspiration/reference boards**, then bring them
back for actual editable art, animation and renderer integration. Text inside an
AI picture does not change the ability. Chorus has its supplied golden reference;
R5 fire slashes have a scoped positive user judgment. Neither needs another
reference request just to continue.

## Copy this preamble before any selected prompt

> Design a visual-development reference board for one Minecraft Java1.20.1 combat
> ability described below. Use deliberate crisp pixel clusters, familiar Minecraft
> material scale, strong major silhouettes and readable anticipation/contact/release.
> Show the effect against a vanilla-scale player and ordinary unshaded terrain.
> Include a short sequence in a fixed third-person view, one first-person visibility
> view, and isolated views of required sprite/model assets. Include a top view for
> ground effects. Make every stage visibly related to the same material and action.
> Keep enough empty space to see the player, enemies and ground. Do not hide weak
> shapes behind bloom, particle noise, repeated circles or a concept-only cinematic
> camera. This is reference art, not a claim that a sprite sheet or Minecraft mod is
> already implemented. Do not invent extra attacks, numeric bonuses, text UI or lore.

## 1. Ancestors — Life Chorus / V

**Mechanic:** one healing front spreads from the cast position out to16 blocks.
Players heal6; animals/villagers/recruits/workers heal fully; native golems also
respond. Harmful effects are removed and the existing full-health absorption boon
is retained. Four miniature existing shields orbit the caster, faces **outward**,
and multiply incoming damage by.75 for5s. The same player-origin front knocks hostile
mobs outward once as it reaches them; it does no new damage. Native knockback
resistance applies. A creature responds when actually reached,
not because it happens to be in a client-side screenshot.

**Timeline:**0–.35s awakening; .35–.90s strong ground pulse; roughly.65–1.5s individual
responses; .90–5.90s shields; brief gold-fragment release. Server phases stay fixed
even if an inspiration image prints other numbers.

**Current direction:** supplied gold storyboard: curled rising gold ribbon,
ornate ground rune with pointed nodes, ivory/gold diamond motes, subtle secondary
wave, miniature painted shields and dissolving fragments. This supersedes the
intermediate green-leaf Chorus candidate. It does not recolor Bloom.

**Reference prompt:**

> Life Chorus: ancient guardian healing nearby players, a horse, farm animals and
> an Iron Golem. Show a small warm-gold curl gathering at the feet, unfolding into
> a tapered rising spiral around the caster. A finely articulated gold ground rune
> opens and sends one low expanding healing front outward, then a quieter echo.
> The front also pushes hostile mobs away once on contact, while friendly
> creatures receive healing rather than knockback. Each creature answers individually with ascending ivory/gold diamond blossoms
> and cross glints, not three orange rods. Four miniature versions of the existing
> painted gold shield orbit slowly at waist height, fronts pointing away from the
> caster; end by breaking into a few drifting gold fragments. Show awakening,
> pulse, recipient responses, sustained protection and release as five coherent
> phases. Separate top-down rune, near/far spiral strands, recipient motes, small
> orbital cue and crumble poses. Controlled light, crisp stepped lips, amber depth,
> pale-gold body and small ivory cores. Preserve the player's view. No green leaf
> substitute for this newly selected gold ritual, giant fire explosion or random
> straight bars. The supplied storyboard is inspiration; keep four shields/5s.

**Assets needed now:** new rune reveal/hold/release cels; rising swirl cels;
per-recipient gold motes; shield dissolve; restrained orbit/secondary cue. Reuse
the existing native R7 shield model/painting at miniature scale. In production.

## 2. Ancestors — automatic guardian below40% health

**Mechanic:** crossing below40% health emits one full-size threat-facing shield,
grants1.5s Resistance V and creates a player-owned native Iron Golem for30s.
Its successful entry starts one outward wave at the **player**, using the same16-block/.6s expansion and native knockback as V; each hostile is knocked once as reached. It is not centered on the golem
and not a repeating aura. Native knockback resistance applies.
No duplicate living guardian; threshold rearms after recovery. This is separate
from Life Chorus's25% ward and does not activate just because V was pressed.

> Show a sudden protective intervention when an injured guardian is threatened:
> one convincing painted gold shield opens between player and attacker, front
> facing the attacker, with a compact brace/impact pose. A native Iron Golem forms
> beside the player through rising gold fragments and settles onto its actual feet.
> At that entry moment show a compact gold pressure wave expanding from the
> **player** to nearby hostile mobs, knocking each outward once, then clearing.
> The golem has its own separate formation cue. No repeating shockwave aura.
> Communicate immediate protection, not another expanding healing spell. Show the
> full-size shield from both sides and the golem formation/release cue. Preserve
> the existing shield design and native golem, no shield fan or unfamiliar statue.

**Needed:** compact interception/formation/release accents around reused models.

## 3. Void Walkers — Rift Ambush / V

**Mechanic:** aim at a living target within32 blocks. Brief opening pause; at.70s
the server rechecks a safe destination behind the target, teleports and performs
the existing attack. Pursuers retarget the actual player; no departure decoy.

> A diagonal cut opens in space near the caster: thin at the two tips and broad
> at the center, comparable in major scale to the last fiery slash. Its interior
> reveals Minecraft's End-portal void, framed by authored torn purple/ivory edges.
> Show anticipation, the incision opening, a held readable aperture, disappearance,
> a matching exit cut behind the target and the cut closing from its tips. Keep
> the central void dark and deep, with restrained purple fracture fragments.
> Show front/side/first-person views. No purple ground circle, plain rectangular
> plane, leaf motif, or fake star texture replacing the native End portal interior.

**Needed:** coherent aperture masks, border motion, closing remnants and compact
exit contact. Existing native portal shader remains the interior source.

## 4. Void Walkers — charged teleport / И

**Mechanic:** hold at least.5s, release a small pearl-like projectile. Server
resolves safe arrival at impact. While aiming, show an approximate purple distance
and a small impact marker; motion, spread/collision and floor search can shift it.

> Show a readable, unobtrusive purple destination estimate while a player aims:
> a small stepped corner/diamond marker near the predicted impact point, plus a
> compact purple distance label below the crosshair. Show a small projectile after
> release, a tapered wake and a tiny arrival cut closing on impact. Separate aiming,
> flight, impact/safe arrival, and no-safe-destination presentation. Never put a
> giant glowing orb or circle in front of the camera; destination feedback belongs
> at the destination. Preserve the view of the enemy and the path.

**Needed:** refined destination marker, small flight/wake and arrival/contact cels.

## 5. Void Walkers — gravity denial / sneak+И

**Mechanic:**75% less gravity for5s. This changes falling behavior, not a teleport
or a new flight/dash power.

> Show gravity easing around the player using sparse upward violet fragments and
> a low, interrupted counterflow near the boots. Present activation, a restrained
> sustained state while falling and a settling release. Distinguish it from the
> portal's torn slit silhouette. No giant sphere, constant vertical rods or opaque
> body aura. Keep feet and ledges visible in first person.

**Needed:** small activation/held/release pixel material, not a recolored portal.

## 6. Born in Bloom — binding roots / V

**Mechanic:** hostile mobs in the10-block query area take6 once. Base has a brief
fitted wrap. Three sequential upgrades: I locks hostile actions for4.5s; II adds
3 magic damage each second at1/2/3/4s; III applies Poison I once at the start.
Normal native health/status clocks continue during control. Root art adapts to
the entity's current dimensions. Already-fired attacks must not be mistaken for
an animation event; actual suppression belongs to the server.

> Show woody roots climbing and wrapping around a hostile creature, adapting to
> a small zombie and a much larger creature. Keep angular Minecraft bends and
> overlapping fitted joints; lower sections are chunky, brown and weight-bearing.
> Leaves must be pointed, recognizable green leaves with purposeful veins. Each
> root branch has its own staggered emergence, inward wrap, tightening hold and
> withdrawal. Show baseline quick strike/release, longer action-lock hold, a small
> once-per-second constriction pulse and a single initial poison accent as readable
> upgrades to one effect. No straight sticks, smooth spaghetti, green wood, square
> plates masquerading as leaves, generic neon rings or repeatedly sprayed poison.

**Needed:** keep current native angular root geometry/independent tracks; refine
tightening/pulse/poison cues without replacing the user's selected lower chunkiness.

## 7. Born in Bloom — Living Path / И toggle

**Mechanic:** native temporary leaf footholds form ahead/below the moving player,
expire after40s and cannot be harvested while temporary. It is traversal support,
not a damage wave or an entity root lock.

> Show small leaf clusters and woody tips answering beneath the next step, followed
> by familiar solid Minecraft leaf footholds. Convey where it is safe to step and
> which section is about to return to nature. Activation should be compact; held
> motion should follow the player's path rather than draw a giant magic circle.
> Show a gap crossing, a turn and a clean expiry, preserving visible block support.

**Needed:** underfoot emergence, direction hint and expiry cues around native leaves.

## 8. Infected by Sculk — radial sonic strike / V

**Mechanic:** replaces sculk traversal. An8-block horizontal wave expands over
ticks7–18 and hits each hostile mob once for8 sonic damage/4 hearts, vertical±3.
Works without standing on sculk. Native sonic sound accompanies the strike.

> Show the player gathering a short, dense sculk pressure pulse at the chest/feet,
> then releasing one circular sonic front along the surrounding ground. Paint
> stepped cyan/ivory pressure lobes, dark teal depth and broken resonant tissue
> accents. The leading edge must have mass and clear direction; secondary echoes
> are visibly weaker. Each enemy contact gets a compact pressure fracture, never
> another full-damage wave. Show wind-up, eight-direction propagation, enemy contact
> and pressure decay from top, side and first-person views. Do not depict travel,
> teleporting, buried decals, a uniform thin blue circle or a global gold recolor.

**Needed:** strong radial pressure body, surface-safe breakup, contact/release cels.

## 9. Infected by Sculk — reveal / И with low resonance

**Mechanic:** reveals nearby entities within32 blocks for4s. This is information,
not an8-damage sonic attack. Hostile kills add10 resonance, capped100.

> Show a quiet sculk listening pulse spreading outward, briefly answering at each
> revealed creature with a restrained cyan outline/resonance mote. Distinguish
> gathering information from a forceful pressure strike. Include an enemy behind
> a wall and an ordinary visible enemy; preserve their positions and the player's
> field of view. No repeated bright circles across every mob or opaque screen tint.

**Needed:** subtle scan onset, individual reveal response and end cue.

## 10. Infected by Sculk — absorb resonance / И with at least25

**Mechanic:** spends25 resonance, heals6 and grants20s Absorption II. This is the
same contextual utility key; do not invent an extra button or new progression.

> Show gathered sculk motes folding inward toward the player's chest, a compact
> resonant core opening then settling into a restrained cyan protective pulse.
> Converging motion communicates spending stored resonance. It must look different
> from Ancestors' outward gold healing wave and the sculk attack's violent pressure.
> Show intake, conversion and quiet sustain/end, with clear first-person visibility.

**Needed:** intake/convert/absorption accents and a clean end, not plain cyan rods.

## 11. Nether-hearteds — earth/magma eruptions / V

**Mechanic:** five growths rise outward at0/.2/.4/.6/.8s, each resolving an exposed
floor independently within±10Y. Three Blockbench forms alternate. Contacts follow
.25s after each emergence:10 fire damage,6s burning and an upward push. A continuous
ground fissure and fire flanks connect supported terrain, with gaps at no floor.

> Show a committed volcanic rupture starting at the player and advancing forward
> as five successive terrain eruptions, not a simultaneous radial bush. The three
> native stone forms have bright saturated orange tops and a whole-height ramp to
> almost black bottoms on all side faces. Paint one connected branching hot ground
> cut beside/under them and shaped fire flanks, with compressed anticipation, upward
> impact mass, brief hold and cooling/collapse. Show the wave climbing and descending
> uneven terrain within ten blocks of the cast elevation. Avoid floating growths,
> same-Y-only placement, pale beige magma, generic vanilla fire as the finished art
> and five unrelated orange circles. Include a side thermal-material study.

**Needed:** connected ground body, shaped fire fronts, emergence/contact/cooling cues
around the existing native magma models. Preserve the thermal painting direction.

## 12. Nether-hearteds — three escalating fiery slashes / И

**Mechanic:** three successive directional hits with collision-checked forward
pulls. Start0/.50/1.10s; contact.20/.70/1.35s; release by1.90s. Each hit retains10
fire damage/6s burning. Approved art escalates compact rising hook→broader return
with wake→large descending cleave/crown. Asset scales2.4/3.6/5.0 are deliberately
different. Visual escalation is not a promise of three different damage values.

> If a new supporting reference is needed, show a readable three-hit fiery combo
> using three distinct continuous fire bodies: a compact rising hook, a broader
> return sweep with a trailing wake, then a massive descending cleave and release.
> Give the fire pale-hot narrow cores, saturated orange body and red ember fringe,
> preserving crisp pixel flow and articulated surviving tongues at the end. Player
> steps forward with each wind-up, never through walls or off ledges. The third hit
> must read stronger in mass, reach, wake and release duration. Do not cut little
> slash-shaped holes inside the fire or deliver three same-power crescent variants.
> Existing R5 artwork is liked: preserve it rather than request a replacement.

**Needed:** no replacement reference/art requested now; retain approved assets and
choreography. Only extend context if the user selects a new scope.

## 13. Nether-hearteds — ordinary lava drinking / right-click

**Mechanic:** with a lava bucket, normal right-click starts the native drinking
animation for1.6s; completion leaves an empty bucket and heals5. Sneak-right-click
retains native pouring. It is not an И combo or a new explosive combat effect.

> Show only a tiny warm intake/ember accent around a lava bucket during familiar
> Minecraft drinking, followed by an empty bucket and a compact restorative warmth.
> Keep hand, item and view readable; no fire wall, attack circle, full-body flame
> eruption or extra button prompt. A single restrained material/motion study is
> enough; native item animation remains the main readable action.

**Needed:** native item use is sufficient now; any custom accent is optional and
must not turn ordinary drinking into another special combo.

## Delivery format when returning references

Send the image and name the power. Useful feedback: which body/edge/phase looks
right, which looks wrong, and whether palette or composition is the main target.
Codex then records the direction, authors native assets/timing, compares the moving
candidate and installs only within authorized scope. A storyboard's rendered
Minecraft scene is concept evidence, not proof of a running mod.

Further race upgrades remain [proposals](assets/life-chorus/PROGRESSION.md), not
requirements an image AI should silently add to these current abilities.
