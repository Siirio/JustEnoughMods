# Alex companion recovery evidence

This document records inspection of the exact installed binaries for the user's expanded request to support existing installed companions and mountable animals. It supersedes the earlier whitelist-only Alex exclusions in `README.md`, it adds no new species or native taming routes. The native identity and each compatibility behavior remain owned by their original mod, JEM owns only the shelter binding/recovery adapter.

Owner: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/shelter/compat/AlexCompanionAdapter.java` and isolated `mixin/shelter/alex` seams. Core recovery, journal, permissions and distribution remain with the main shelter implementation.

## Evidence and measured scope

- `alexsmobs-1.22.9.jar` SHA256 `16bc5bcc19db9029c24f951346ec71098e6f5b0afef96e61894a2e9407aad37c`
- `alexscaves-2.0.2.jar` SHA256 `131e205ce3eed384d701b336d8a0d4a671108ed56b6eff6014f0e407db2997f0`

Inspected all 129 direct declarations in Alex's Mobs' native entity package and all 57 direct declarations in Alex's Caves' native living-entity package, including helpers and multipart classes. Actual native AMEntityRegistry/ACEntityRegistry registrations, class inheritance, owner routes, controlling-passenger methods, serialization, inventories, death drops and follow goals were read from these jars using javap/CFR. This is binary/source evidence, not a runtime test.

The implemented adapter contains **34 explicit registered parent identities**: 23 Alex's Mobs (20 native tameable classes plus Endergrade, Laviathan and Tusklin rider-custodian mounts) and 11 Alex's Caves (10 concrete native tameable-hierarchy types and the native Gum Worm parent). The Gum Worm's separate segment is translated to its existing parent identity, never another pet. Native-ownerless rides gain a durable JEM recovery custodian only after the real controlling rider explicitly binds, native taming/AI never changes. Native owners always take precedence. A wild Luxtructosaurus is not made tame, owned or mountable merely because its class inherits DinosaurEntity.

Native registration dimensions below are clearance evidence only, never a binding size limit. Age, scale, full multipart geometry and current native bounding boxes still control actual safe placement.

| Registry ID | Native class | Adult registered width x height | Safe habitat | Ownership route | Native owner teleport | Native retained payload / special handling |
| --- | --- | --- | --- | --- | --- | --- |
| `alexsmobs:bald_eagle` | `EntityBaldEagle` | 0.5 x 0.95 | flying | Native tame+owner UUID | Yes, native goal seam | HasCap, EagleCommand, transient launched camera state cleared |
| `alexsmobs:caiman` | `EntityCaiman` | 1.3 x 0.6 | amphibious | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:capuchin_monkey` | `EntityCapuchinMonkey` | 0.65 x 0.75 | ground | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:cosmaw` | `EntityCosmaw` | 1.95 x 1.8 | flying | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:crocodile` | `EntityCrocodile` | 2.15 x 0.75 | amphibious | Native tame+owner UUID | No native owner teleport added | Native variant/command and base held/equipment state |
| `alexsmobs:crow` | `EntityCrow` | 0.45 x 0.45 | flying | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:elephant` | `EntityElephant` | 3.1 x 3.5 | ground | Native tame+owner UUID | No native owner teleport added | Items, Chested, Carpet, Tusked, native chest/equipment death collection + trailing die guard |
| `alexsmobs:endergrade` | `EntityEndergrade` | 0.95 x 0.85 | flying | Current controlling rider -> explicit JEM custodian, native owner wins | No native owner teleport added | Saddle |
| `alexsmobs:flutter` | `EntityFlutter` | 0.5 x 0.7 | flying | Native tame+owner UUID | Yes, native goal seam | Potted + native held/equipment |
| `alexsmobs:gorilla` | `EntityGorilla` | 1.15 x 1.35 | ground | Native tame+owner UUID | No native owner teleport added | Native variant/command and base held/equipment state |
| `alexsmobs:grizzly_bear` | `EntityGrizzlyBear` | 1.6 x 1.8 | ground | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:kangaroo` | `EntityKangaroo` | 1.65 x 1.5 | ground | Native tame+owner UUID | Yes, native goal seam | Items, native equipment slot indices, native serializer/resetKangarooSlots |
| `alexsmobs:komodo_dragon` | `EntityKomodoDragon` | 1.9 x 0.9 | ground | Native tame+owner UUID | Yes, native goal seam | Saddle + native command |
| `alexsmobs:laviathan` | `EntityLaviathan` | 3.3 x 2.4 | amphibious | Current controlling rider -> explicit JEM custodian, native owner wins | No native owner teleport added | HeadGear/BodyGear, Obsidian + multipart parent |
| `alexsmobs:mantis_shrimp` | `EntityMantisShrimp` | 1.25 x 1.2 | aquatic | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:mimic_octopus` | `EntityMimicOctopus` | 0.9 x 0.6 | aquatic | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:mudskipper` | `EntityMudskipper` | 0.7 x 0.44 | amphibious | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:orca` | `EntityOrca` | 3.75 x 1.75 | aquatic | Native tame+owner UUID | No native owner teleport added | Native variant/command and base held/equipment state |
| `alexsmobs:raccoon` | `EntityRaccoon` | 0.8 x 0.9 | ground | Native tame+owner UUID | Yes, native goal seam | Carpet + native held/equipment |
| `alexsmobs:sugar_glider` | `EntitySugarGlider` | 0.8 x 0.45 | flying | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:tarantula_hawk` | `EntityTarantulaHawk` | 1.2 x 0.9 | flying | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexsmobs:tusklin` | `EntityTusklin` | 2.2 x 1.9 | ground | Current controlling rider -> explicit JEM custodian, native owner wins | No native owner teleport added | Saddle, shoe equipment |
| `alexsmobs:warped_toad` | `EntityWarpedToad` | 0.9 x 1.4 | amphibious | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexscaves:atlatitan` | `AtlatitanEntity` | 5.0 x 8.0 | ground | Native tame owner when present, native temporary rider otherwise -> explicit custodian | No native owner teleport added | Native variant/command and base held/equipment state |
| `alexscaves:candicorn` | `CandicornEntity` | 1.7 x 2.25 | ground | Native tame+owner UUID | Yes, native goal seam | Saddle + native command |
| `alexscaves:grottoceratops` | `GrottoceratopsEntity` | 2.3 x 2.5 | ground | Native tame+owner UUID | No native owner teleport added | Native variant/command and base held/equipment state |
| `alexscaves:gum_worm` | `GumWormEntity` | 3.5 x 2.7 | ground | Current controlling rider -> explicit JEM custodian, native owner wins | No native owner teleport added | Complete native stable-UUID segment chain, links/index, native parent only carries RidingSegmentUUID |
| `alexscaves:luxtructosaurus` | `LuxtructosaurusEntity` | 6.0 x 8.5 | ground | Native tame+owner UUID | No native owner teleport added | Native state, separate delayed death-loot buffer guarded |
| `alexscaves:raycat` | `RaycatEntity` | 0.85 x 0.6 | ground | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexscaves:relicheirus` | `RelicheirusEntity` | 2.65 x 5.9 | ground | Native tame+owner UUID | No native owner teleport added | Native variant/command and base held/equipment state |
| `alexscaves:subterranodon` | `SubterranodonEntity` | 1.75 x 1.2 | flying | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexscaves:tremorsaurus` | `TremorsaurusEntity` | 2.5 x 3.85 | ground | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexscaves:tremorzilla` | `TremorzillaEntity` | 4.5 x 11.0 | amphibious | Native tame+owner UUID | Yes, native goal seam | Native variant/command and base held/equipment state |
| `alexscaves:vallumraptor` | `VallumraptorEntity` | 0.8 x 1.5 | ground | Native tame+owner UUID | No native owner teleport added | Native variant/command and base held/equipment state |

## Native serialization and death ownership

Elephant and Kangaroo serialize each native inventory slot into Items, and restore their native containers. Kangaroo also restores its native equipment slot indices and calls resetKangarooSlots, JEM does not reconstruct an independent inventory. Saddles, caps, carpets, pots, held items, commands, variants and owner UUIDs remain in native NBT. The small/large shelter decision never filters these identities by dimensions.

Native dropEquipment for Elephant/Kangaroo and the ordinary held/equipment route run inside native death capture. Elephant additionally loops its inventory after super.die outside the captured collection: AlexElephantDeathMixin stops that trailing loop only for JEM-confirmed captured deaths. Luxtructosaurus populates a deferred deathItems buffer during its native death animation, outside LivingDropsEvent, the isolated populateDeathLootForLuxtructosaurus hook suppresses that buffer only for a JEM-captured owned death. Ordinary wild boss loot/combat remains native. Gum Worm overrides dropAllDeathLoot, but still captureDrops + ForgeHooks.onLivingDrops are present, the common captured-drop cancellation owns that route.

No native resurrection owner was found in these inspected Alex companion classes. This statement does not apply to the separate Companions mod, whose existing native resurrection owner needs its own adapter.

## Habitat and teleport seams

Orca requires water and has a native Moistness reserve. Mantis Shrimp and Mimic Octopus eventually take drying damage even though their native navigation permits temporary land travel, shelter recovery therefore requires an actual water volume. The adapter restores their native environmental reserve using exactly their own water/bucket restoration values (60000 or 2400), not a JEM buff or timer. Mudskipper, Caiman, Crocodile, Warped Toad, Laviathan and Tremorzilla have verified native amphibious navigation. Laviathan itself accepts lava and water, the shelter's amphibious search deliberately selects ground/water that is also native-safe. Fliers retain native flight and require real free collision space.

TameableAIFollowOwner and AnimalFollowOwnerGoal extend vanilla FollowOwnerGoal, the existing vanilla teleport-only hold therefore applies through native inheritance. Independent native goals need exact additional hooks: FlyingAIFollowOwner and TameableAIFollowOwnerWater, CrowAIFollowOwner, MantisShrimp.FollowOwner, WarpedToad.FollowOwner, SubterranodonFollowOwnerGoal and TremorzillaFollowOwnerGoal. Each hook cancels only the private native owner-teleport method. Cosmaw additionally teleports in AIPickupOwner: its one explicit teleport call is intercepted, preserving navigation and the rest of its native rescue logic. A grouped production-SRG/mapped injection requires exactly one matching call route. These markers clear through the core's owner interaction, animals without native owner teleport acquire no new movement lock.

Bald Eagle's serialized Launched/LaunchTime are transient remote falconry presentation/control state, so a restored eagle is not left in a dead remote camera launch. Native HasCap and command survive. No permanent animation or replacement AI is installed.

## Gum Worm: native whole-body identities

The native parent stores only RidingSegmentUUID. The actual 15-19 segment entities are separately persisted, each with UUID, HeadUUID, FrontUUID, BackUUID and Index. Native finalizeSpawn creates a new random-length body, calling it during recovery would silently change body state and leave old persistent parts. JEM never uses that route.

Instead the death snapshot retains every currently loaded native segment as exact native NBT in the parent's existing ForgeData, alongside the old head position. Recovery requires a complete actual chain: index zero fronts the head, each next index has reciprocal native links, the final back link is absent, stable UUIDs are unique, and the count is within the native 15 + nextInt(5) range. There is no invented replacement count. If original parts were unloaded at death, parent and available payload remain durable pending, a later natural old-segment join appends missing native data before rejecting its stale epoch. No source chunk is force-loaded. If missing source parts never naturally load or were already corrupted/lost by native storage, the pending record remains blocked, JEM does not claim to reconstruct missing native data.

After the restored head is accepted, native EntityType deserialization recreates the exact original segment UUIDs, links and state, translated by the verified shelter destination. Every restored segment is stamped with the head's recovery epoch. A temporary exact-object admission set admits only the newly restored body during an active recovery transaction, even a same-epoch saved part from an interrupted write is held out while the head journal is restoring. Later stale native parts are rejected using their saved HeadUUID and the canonical head journal even when the head itself is unloaded. No native known-UUID registry is manually modified: inspected Forge47.4.10 posts the cancellable join before registering the UUID.

The adapter returns the head and every expected separately saved segment to synchronous persistence, any missing exact identity throws. The core must save and acknowledge every loaded affected entity chunk before clearing the pending native payload. If native add/save fails, adapter.rollback discards the reconstructed body and leaves its native snapshot available for retry. Native transient Forge PartEntity children of Laviathan, Sauropod and Tremorzilla do not receive separate persistent entity identities, their parent snapshots preserve full observed collision extents.

The adapter's bounds include the actual full multipart geometry recorded at death and the saved Gum Worm body union. This can require a genuinely broad safe physical area, while either home size remains eligible. The shared safety owner must compensate an extended body's offset below the parent's origin when aligning its lowest safe support surface. No terrain is destroyed to fit a mount.

## Explicit scope distinctions

Straddleboard is a nonliving vehicle/item, not an animal companion, its native vehicle lifecycle is unchanged. Straddler has no native player ownership/mount admission. Underzealot's controlling passenger is a ritual victim, not a player-owned animal mount, it is not converted into a pet. Multipart hit proxies are canonicalized to their parent, not separately bound. Unowned wild species are not assigned fabricated owners. No bosses gain new taming, combat balance or drops through this adapter.

All behavior above was inspected in source/binary form. No tests, fixtures, static-analysis jobs, client launch, gameplay QA or server launch were created or run by this subtask. Compilation, final common wiring and distribution are handled by the owning task. Runtime/player acceptance remains explicit.
