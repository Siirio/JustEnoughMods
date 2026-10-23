# Shelter-bound companion recovery

Owner: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/shelter`.
Client projection: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/shelter/ShelterScreen.java`.
Gameplay assignments/history: `Catalog/changes/records/pet-shelters.json`.

The user requested these ordinary singleplayer/dedicated gameplay systems in
`JEM_Food_PetShelters_SeasonalHUD_FINAL_v2.md` on 2026-10-23. The implementation
adds no animal species, combat bonuses, training, progression or resurrection cost.
Source was reviewed; no game acceptance is claimed. Production model integration
awaits the user's real Gemini-authored `.bbmodel` files. Geometry below is the
source contract and must match the eventual accepted models before delivery.

## Placement and ownership

`jemcompat:small_animal_home` occupies 2 x 2 x 2 cells;
`jemcompat:large_animal_home` occupies 3 x 3 x 3. Each is one placeable BlockItem.
The controller is local cell zero. Other cells have no block entity. Neither has
a ticker or renderer. Cell indexing is `x + size*z + size*size*y`; local +Z is
the open front, facing SOUTH before rotation. Local +X grows towards the facing
direction's counterclockwise neighbor. Four rotations use the same clipped voxel
boxes in `ShelterGeometry` for collision and selection.

Placement requires already-loaded empty, fluid-free cells, supporting solid-top
ground under the footprint, world bounds, native item-use permission and no
entity obstruction. Forge's real ItemStack multi-block placement transaction
captures all cells placed by `setPlacedBy` and can roll them all back when the
native MultiPlaceEvent is denied. There is no synthetic placement permission
click. Pistons cannot move individual cells. Homes use ordinary wooden blast
resistance, and explosion removal follows the same pending-data preservation path. Breaking a cell requires ownership
(or native operator permission level 2) and uncancelled native BreakEvent checks
for every affected cell, using a reentrancy guard. Dismantling yields at most one
survival item and no creative item; controller removal retains pending pet data.

The two shaped recipes are `PSP / L L / PPP` and `LRL / L L / PPP`, respectively.
P means planks; L logs; S wooden slab; R wooden stairs. The one shared shaped
serializer enforces the same vanilla wood family in all nonempty cells. It uses
native tags and one JSON per shape, supports normal/stripped logs and wood,
and never modifies an upstream recipe. The recipe book unlocks both upon
obtaining a log; JEI receives ordinary shaped CraftingRecipe objects. The visual
wood design is independent of ingredient family.

## Binding GUI and discovery

Only the placer opens their home. The compact 248 x 166 screen reuses the real
vanilla `minecraft:textures/gui/demo_background.png` parchment region, native
font, native buttons, and native spawn-egg icons. There is no fabricated GUI
PNG. Three visible rows scroll across the server's complete known-pet list;
search filters localized species/custom name. There are exactly two pet actions,
Bind and Unbind, with contextual help. EN/RU strings live in JEMCompat's existing
`assets/jemcompat/lang/en_us.json` and `ru_ru.json`. This source reuse does not
constitute runtime visual acceptance.

Entity joins, native tame notifications, ordinary interactions and loaded-only
discovery on opening a home populate one overworld owner-to-pet index. Discovery
does not load chunks or read every saved entity. Unseen unloaded pets remain
unknown; the empty screen and row tooltip explain this. Observed native ownership
changes invalidate an old home assignment. Known records are rechecked when the
native entity is actually loaded. A loaded unsupported entity can be listed,
but cannot acquire recovery merely by inheriting TamableAnimal.

The menu owns an immutable home position, dimension and random session nonce.
The action packet contains container ID, nonce, server revision, stable pet UUID
and Bind/Unbind. The server checks the actual open menu, range, loaded controller,
complete structure, owner, pet identity, native ownership where loaded, supported
serialization and occupancy. Replaying a previous revision fails. Native menu
button packets were deliberately not used for row IDs: their button ID is one
signed byte and cannot represent hundreds of pets safely.

One pet has one home; one home has one pet. Rebinding mutates that single
assignment atomically on the server thread. An occupied home never evicts another
pet. Unbind and home destruction remove only the assignment; a dead pet's native
snapshot survives and can be assigned elsewhere. Binding never compares an
animal's size against the house, doorway or interior clearance.

## Verified native scope

The five concrete classes below are explicitly admitted; arbitrary subclasses
are not. Native dimensions below describe ordinary adult registration only.
Recovery checks the actual restored bounding box, including native age/scale.

| Registry ID | Native owner route | Owner teleport | Inventory / equipment | Adult width x height | Status / habitat |
| --- | --- | --- | --- | --- | --- |
| `minecraft:cat` | Cat / TamableAnimal tame + owner UUID | Native FollowOwnerGoal | Native entity/equipment serialization | 0.6 x 0.7 | Supported ground animal, conditional on no unknown serialized Forge capabilities |
| `minecraft:wolf` | Wolf / TamableAnimal tame + owner UUID | Native FollowOwnerGoal | Native entity/equipment serialization | 0.6 x 0.85 | Supported ground animal, same capability restriction |
| `minecraft:horse` | Horse / AbstractHorse isTamed + owner UUID | None added | Native SaddleItem and Horse ArmorItem serialization | 1.3964844 x 1.6 | Supported ground mount, same capability restriction |
| `minecraft:donkey` | Donkey / AbstractHorse isTamed + owner UUID | None added | Native chest flag, Items and saddle; native serializer covers JEM's existing expanded inventory | 1.3964844 x 1.5 | Supported ground mount, same capability restriction |
| `minecraft:mule` | Mule / AbstractHorse isTamed + owner UUID | None added | Native chest flag, Items and saddle | 1.3964844 x 1.6 | Supported ground mount, same capability restriction |

All five bind to **either** home. A horse in the small home always proceeds to
external/above candidates when the actual box cannot fit inside. Native data is
serialized before equipment death drops can mutate it. Passengers and leash
references are excluded; passengers are never cloned. Unknown nonempty ForgeCaps
are deliberately unsupported because their inventory/drop semantics have not
been verified. A supported class is not proof that every third-party capability
attachment is supported.

Installed-mod evidence was inspected directly: Minecraft Forge 1.20.1 / 47.4.10
mapped bytecode; `companions-forge-1.20.1-1.3.3.jar`;
`alexsmobs-1.22.9.jar`; the existing JEM native taming catalog. No third-party
recovery adapter is enabled in this change. Measured implemented recovery coverage
is **5 concrete vanilla entity types**, not the whole installed pack.

| Observed existing content | Status | Concrete limitation |
| --- | --- | --- |
| `minecraft:parrot` and other unlisted vanilla mounts | Read-only/unavailable | No audited native flying/perch or corresponding species/inventory recovery adapter; no terrestrial assumption is made |
| Companions 1.3.3, including native `companions:antlion` | Read-only/unavailable | CompanionEntity already owns `respawnTotemPos`, dimension, native removal behavior and RespawnTotemBlockEntity savedEntities/pending queue. Copying the native entity would create competing resurrection owners; no safe transfer adapter is implemented |
| Alex's Mobs 1.22.9 `alexsmobs:elephant` | Read-only/unavailable | Native elephantInventory, own serialization and equipment behavior require an exact inventory/drop adapter |
| Alex's Mobs 1.22.9 `alexsmobs:cosmaw`, `alexsmobs:flutter`, `alexsmobs:mantis_shrimp`, `alexsmobs:bald_eagle` | Read-only/unavailable | Native flying/aquatic habitat and owner teleport behavior are not verified by the ground-only shelter adapter |
| Other entries in JEM's existing taming catalog (`alexsmobs:*`, `alexscaves:*`, `primal:*`, `spawn:hamster`) | Read-only/unavailable | The catalog proves a known taming hint, not native owner/death/inventory/teleport safety. No individual recovery adapter has been verified; dimensions are deliberately unresolved |

This table documents the supported subset and real unresolved seams. It is not
a complete native entity audit or a statement that unresearched animals cannot
be supported later. In particular, no tall/wide third-party creature is rejected
because of geometry: the current blocker is absent verified recovery ownership.

## Death, storage and exactly-once limits

`data/jem_companions.dat` in the overworld SavedData storage is the sole journal.
Version 1 stores owner, stable UUID, species, optional name, last verified
dimension/position, one home assignment, epoch, blocked reason and optional
native recovery snapshot. No legacy shelter schema existed; unknown future or
malformed identities fail visibly rather than erase stored native data.

The confirmed-death seam runs after ForgeHooks.onLivingDeath and all death-event
cancellation, immediately before LivingEntity's `dead=true`, before native equipment/loot
handling. It increments the stable epoch, records the native snapshot and saves
the journal before loot can run. LivingDropsEvent is cleared/cancelled and native
XP is zeroed only for that captured death. The separate native wither-rose
byproduct is also suppressed; it is created outside LivingDropsEvent. No currency,
equipment or conventional loot is minted by recovery.

Before adding the restored entity, a `restoring` intent is saved while retaining
the snapshot. AddFreshEntity and a cancellable join are never treated as durable
storage. The EntityStorage seam waits for IOWorker.store's successful actual
RegionFileStorage write containing the exact UUID and epoch before clearing the
snapshot. Coalesced/superseded chunk-write futures are rejected by a per-chunk
sequence. A stale old native chunk copy with an earlier epoch is rejected on
actual entity join. A failed write leaves the journal intact. No full-world
force-save or forced chunk load is used.

**Conservative crash limitation:** if a process crashes after the restoring
intent but before a confirmed native write and the matching entity does not
naturally reload, the snapshot remains durable, marked as awaiting native
reconciliation. The code does not guess whether that identity moved into another
unloaded chunk and does not spawn a second copy. There is currently no automatic
repair or admin repair UI for that ambiguous state. This preserves data and
prevents duplication, but does not claim automatic recovery from every possible
cross-file crash window. Filesystem/hardware corruption is outside native save
guarantees. Third-party loot emitted outside the inspected native hooks requires
an exact adapter and is not claimed safe by inheritance.

Journal writes override native SavedData's error-swallowing writer: compressed
NBT goes to a same-directory temporary file, is forced to disk and read back,
then atomically replaces the one canonical file. Failures propagate without
clearing dirty state; a failed death intent rolls back its in-memory mutation
before any captured-drop suppression. The loader likewise refuses corruption
instead of letting the generic native loader silently construct an empty index.

Recovered cats/wolves retain native following/pathfinding; only
FollowOwnerGoal.teleportToOwner is paused using a persistent marker. Their owner's
first normal interaction clears the marker and consumes that interaction without
also feeding/toggling sitting. Normal owner actions resume on subsequent clicks.
Ground mounts have no invented follow, movement or teleport lock.

## Safe anchored recovery

Death removal, naturally loaded chunks, player approach and relevant native block
edits coalesce into event-driven recovery work. Player approach samples once per
second only after a block-position/dimension change. No whole-world pet scan,
per-home ticker, forced loading or resurrection cooldown exists. The anchor must
be currently loaded and a player must be within 32 blocks. Offline owners are
conservatively deferred because the native spawn-protection permission context
requires an authenticated ServerPlayer; another player cannot impersonate them.

The actual restored entity first tries the center of the authored interior
floor. Both home sizes then use the same bounded search: up to eight blocks
horizontally from the footprint, eight blocks down and 32 up. Loaded columns
contribute their actual support-shape top surfaces, including partial blocks,
roof steps and dirt/stone above a buried home. Candidates sort deterministically
by nearby distance band, favoring projection above the footprint within a band,
then exact distance and coordinates. Burial above the roof is never structural
invalidation. Empty door space is never a binding prerequisite.

Every candidate checks world bounds, all touched chunks before collision calls,
the complete native AABB, entity obstruction, fluids, vanilla damaging blocks,
native path hazard categories and supporting collision at a nine-point feet
grid. Full space must be safe; no squeezing, scaling or terrain destruction
occurs. A blocked search retains the snapshot and its binding with a visible
reason until another natural approach/load/edit. This ground-animal predicate is
not presented as safe for aquatic, flying or exotic modded species.

Actual placement/use/break requests retain native Forge protection events.
No third-party claim mod is installed in the inspected ManagedPack; JEMClaims
has no active source implementation. Recovery respects native spawn protection
and fails closed if Flan, FTB Chunks or Open Parties and Claims is subsequently
installed without its own verified animal-spawn permission adapter. It never
posts synthetic click events that could execute another mod's block actions.

## Remaining acceptance and delivery

The requested horse/small-home, external spawn and buried-home surface paths are
implemented in source without a size gate. They have not been exercised in a
game. The user rejected automated tests; none were created or run. No client,
disposable server, QA run or MineRENT release was launched by this subtask.
Final model assets, source/geometry reconciliation, GUI appearance acceptance,
production compilation/synchronization and native AutoModpack publication are
reported by the owning implementation task, not inferred from this document.
