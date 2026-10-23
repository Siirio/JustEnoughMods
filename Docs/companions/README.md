# Shelter-bound companions

Source owner: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/feature/shelter`

Canonical gameplay record: `Catalog/changes/records/pet-shelters.json`

The 2026-10-23 user amendment requires installed mountables, tameables and companions, replacing the previous five-species restriction. Clock/season artwork and shelter models are explicitly excluded from this implementation phase and remain user/Gemini-owned. Missing models still prevent installation of the combined production artifact. Source compilation does not establish player acceptance

## Ownership and discovery

`CompanionAdapters` selects exact installed adapters before native vanilla ownership. Native tame ownership always takes precedence. Companions secondary player ownership and Recruits ownership use verified native signals. Inventories, equipment, variants, commands and serialized Forge capabilities remain native data, without the old blanket capability rejection

Vanilla cats, wolves, parrots, native horse variants, llamas, camel, pig, strider and Allay use their corresponding native contracts. Allay ownership comes from `LIKED_PLAYER` brain memory, including native removal when its item is returned. Its inventory uses native writeInventoryToTag/readInventoryFromTag and captured native equipment drops

For native ownerless rideable animals, the actual controlling player can explicitly claim shelter custody with Bind while riding. Merely riding does not assign custody. The journal and entity data retain the explicit claim, preventing another rider from stealing the binding. This adds neither native taming nor movement AI. Unloaded provisional mounts cannot be claimed remotely. Wild bosses without native ownership or an actual native controlling rider are not enrolled

Discovery observes joins, taming, interactions and already-loaded entities when opening the GUI. It never reads all entity-region files or forces chunks. Native ownership changes invalidate the previous assignment. Loaded corpses cannot acquire a new binding without canonical pending recovery. Temporary spells are not converted into permanent animals

Installed class/registry evidence and precise exceptions live in [Alex evidence](alex-evidence.md) and [pack evidence](pack-evidence.md). Actual local Gemini review and recommendation dispositions live in [Gemini review](GEMINI_REVIEW.md)

The applied source set contains 134 concrete registered parent identities: 14 vanilla, 34 Alex and 86 other installed classes. Living temporary summons are included. Their native active-entity age, secondary player ownership and explosion state remain intact. Iron's native SummonManager owner and earliest expiration queue entry are persisted as an adapter receipt and restored through the same native manager with only the remaining duration. Expired summons stay expired. Individual legacy summons whose native owner or deadline was already lost remain visibly blocked, rather than gaining an invented lifetime. Accepted native disk joins are reconciled after actual admission, while cancelled and stale copies cannot register another native summon

## Homes and GUI

Exactly two items, `jemcompat:small_animal_home` (2 x 2 x 2) and `jemcompat:large_animal_home` (3 x 3 x 3), construct their whole four-way rotated multiblocks. Cell zero owns the sole controller, without a ticker or renderer. Per-cell selection/collision geometry awaits reconciliation with accepted final models

Placement checks loaded empty cells, support, fluids, bounds, entity obstruction and native permissions. Forge multi-placement rollback is preserved. Breaking checks ownership/operator permission and native protections for every cell. Dismantling yields at most one survival item. Destruction removes only the assignment and preserves pending data

Recipes remain `PSP / L L / PPP` and `LRL / L L / PPP`, using matching vanilla wood families through one shaped serializer. No upstream recipe changes

The owner's searchable scrollable GUI uses exactly two pet actions, Bind and Unbind. Packets use menu ID, nonce, revision and stable UUID. The server rechecks actual open menu, dimension, range, intact loaded home, ownership, occupancy and native identity. Temporary readiness reasons are localized EN/RU. House size never affects eligibility

One pet has one home and one home has one pet. Rebinding moves the assignment without evicting occupants. Unbind never kills or teleports a living pet or deletes pending recovery. The compact screen uses native parchment, widgets, font and spawn eggs, without fabricated production art

## Death and persistence

`data/jem_companions.dat` is the single overworld journal. Schema 2 reads schema 1 and adds provisional-rider and explicit-custodian fields. Corrupt identities and unknown schemas fail visibly. Writes use a forced read-back-verified temporary file and atomic same-directory replacement. Failed commits preserve dirty state and the previous in-memory payload

Confirmed death is captured after Forge cancellation, before the dead flag and native equipment/loot processing. Native snapshot and advanced epoch are persisted before suppressing captured drops, XP and wither roses. Narrow adapters cover out-of-band elephant inventory, caribou saddles, recruit inventory/corpses and Luxtructosaurus loot. Bound Hamster drying retains the native death source without emitting another serialized dried-pet block

For bound pets, the Companions native respawn listener is bypassed before totem lookup and queue insertion. Native totem configuration and charges remain untouched. Living unbind restores normal future-death handling. Existing native queued dead pets are not guessed or migrated by proximity

Every restoration attempt saves a fresh epoch before admission. `ShelterPersistence` invokes Minecraft's native entity-section serializer for already-loaded affected chunks, verifies every restored UUID/epoch in the actual payload, waits for real IOWorker writes and flush, then clears the journal durably. This finishes before returning to interaction or entity simulation. There is no AI/movement lock, per-tick save loop or full-world save. Failed I/O removes the attempted live body and retains the recovery payload

After interruption, uncommitted native disk copies are rejected and the journal retries with a fresh epoch. Older copies and duplicate loaded identities are rejected on join. Multipart Gum Worm recovery preserves native child UUIDs, link graph and translated complete-body clearance, saving every restored part before clearing the head journal. Missing naturally unloaded parts remain explicit durable pending data until native joins reconcile them. No forced loads or randomized replacement body

These are source/native-save guarantees, not hardware transactions across arbitrary third-party files. Runtime player acceptance and crash experiments were not performed. Foreign mods storing inventories outside native serialization require their own verified contract

## Safe recovery

Death removal, natural chunk loads, terrain changes and changed player approach positions trigger bounded recovery. It requires an intact loaded home, nearby player within 32 blocks and authenticated online owner for native protection checks. No ingredient, cost or resurrection cooldown

Both sizes share the eight-block horizontal, eight-downward, 32-upward search. The actual interior is preferred when safe. Actual support-shape tops include surfaces above dirt-covered homes. Large creatures are never scaled or forced inside

Candidates check full native body bounds, loaded neighbors before collision queries, entity obstruction, world limits, native hazards and protection. Ground requires full foot support. Aquatic creatures require complete water coverage, amphibious creatures can use water or stable ground, lava mounts can use native lava habitat, and flying creatures can use safe air. Blocked space retains the snapshot and visible reason until a natural retry

Post-recovery owner teleport is paused through native goal/custom teleport seams until the owner's first interaction. Native walking, flight, combat, sitting and mount controls remain intact. The first interaction acknowledges the hold and is consumed, with later interactions native

## Vanilla evidence

Native vanilla registration dimensions were read from the installed Forge47.4.10 mapped EntityType bytecode. These are adult registration references, never size eligibility rules. Recovery always uses the actual deserialized bounds

| Registry ID | Registered width x height | Native authority | Habitat / payload |
| --- | --- | --- | --- |
| `minecraft:cat` | 0.6 x 0.7 | Tame owner UUID | Ground, native equipment, owner teleport hold |
| `minecraft:wolf` | 0.6 x 0.85 | Tame owner UUID | Ground, native equipment, owner teleport hold |
| `minecraft:parrot` | 0.5 x 0.9 | Tame owner UUID | Flying, native variant/equipment, owner teleport hold |
| `minecraft:horse` | 1.3964844 x 1.6 | Native tamed owner | Ground, saddle and armor |
| `minecraft:donkey` | 1.3964844 x 1.5 | Native tamed owner | Ground, native chest/Items and saddle |
| `minecraft:mule` | 1.3964844 x 1.6 | Native tamed owner | Ground, native chest/Items and saddle |
| `minecraft:skeleton_horse` | 1.3964844 x 1.6 | Native owner or explicit actual-rider custody | Ground, native saddle/state |
| `minecraft:zombie_horse` | 1.3964844 x 1.6 | Native owner or explicit actual-rider custody | Ground, native saddle/state |
| `minecraft:llama` | 0.9 x 1.87 | Native tamed owner | Ground, native chest/Items, strength and decoration |
| `minecraft:trader_llama` | 0.9 x 1.87 | Native tamed owner | Ground, native chest/Items and trader state |
| `minecraft:camel` | 1.7 x 2.375 | Native owner or explicit actual-rider custody | Ground, saddle, pose and native state |
| `minecraft:pig` | 0.9 x 0.9 | Explicit actual controlling-rider custody | Ground, native saddle, no new taming |
| `minecraft:strider` | 0.9 x 1.7 | Explicit actual controlling-rider custody | Lava or stable ground, native saddle |
| `minecraft:allay` | 0.35 x 0.6 | Native LIKED_PLAYER memory | Flying, native carried item and inventory |

## Delivery boundary

Food and day/season data/rendering source remain in their existing owners. This amendment extends companion compatibility and completes native persistence. Icons/models are explicitly deferred. No tests, fixtures, game launches, stable-profile changes, MineRENT release or baseline promotion were performed. The combined artifact is withheld from ManagedPack and local Server while production models are absent, rather than installing missing-model gameplay entries
