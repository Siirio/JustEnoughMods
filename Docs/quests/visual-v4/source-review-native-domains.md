# Combined source review bucket C: remaining native domains

This is the assigned remaining bucket of the same overall pre-build source pass. It covers the native, domain, Create, filter, travel, workshop, nature, Enderman, factory, creature-care, combat-tools, Caves, Wildlife, MagicLast and owned-event receipts. Existing independently reviewed older adapters were examined at shared interfaces and interactions with the new families, rather than given another per-card review loop

The pass traced the listed receipt source paths into relevant actual helpers and Mixins, with emphasis on common vanilla callsites, final Forge cancellation, native receipt conservation, actor admission, Java access after Mixin relocation, capacity and lifecycle. Exact native sources or existing native API evidence were consulted where needed. No automated checker, test, fixture, compilation, Gradle task, game, runtime probe, activation, synchronization or Git action was run. No subagent was created

All source acceptance and runtime flags remain false pending the parent's combined acceptance. The report does not certify current-source compilation or Mixin application

## Material fixes

### Two incompatible redirects at each of two common native callsites

`ArrowHitQuestMixin` and `CaveQuestVanillaArrowDamageMixin` both redirected `AbstractArrow.onHitEntity -> Entity.hurt`. Only one redirect can own that instruction, so their separate correct-looking native proofs could not coexist reliably. `ArrowHitQuestMixin` now owns the one original damage call and passes its actual accepted result and original health to the normal generic `CaveQuestProof.damageObserved` helper for a marked same-actor volley. The original generic bow and siege proofs remain active. The redundant Cave volley Mixin source was deleted

`CaveMapResultQuestMixin` and `RemainingFoodCraftReceiptMixin` likewise both redirected `ResultSlot.onTake -> CraftingContainer.removeItem`. Food now owns the one original removal. A public normal-helper `CaveResearchProof.CraftDebitWitness` interface forwards that exact returned removal to the map observer. The map retains its native selected recipe, biome and nine actual debit requirements. The existing Food dispatcher and its Photo and Wildlife consumers retain their actual native removed stacks. No ingredient is removed twice

### Shield observers reached success before final Forge cancellation

Skarrier Magmadermis and Wrought observers could emit from inside a native `ShieldBlockEvent` listener before later listeners canceled the block. Forlorn projectile provenance was also activated inside the listener. All three now defer the receipt or activation through the existing `CombatDeferredProof` until `ForgeHooks.onShieldBlock` returns its final event. The event must remain uncanceled with positive finite blocked damage. Native burning, repair, charge and durability effects and actual accepted projectile spawn remain prerequisites. Gameplay calls and native event results are unchanged

The existing `CombatForgeDispatchQuestMixin` now settles this shield-event queue at that exact return. It needs no new registration or upstream dependency

### Quest sidecars contaminated native Create item identity

`CreateCargoTransfers.fingerprint` serialized all root keys returned by `ItemStack.save`, including new Food, Caves and parcel observation sidecars. These keys are outside ordinary native item tags, so physically equivalent ingredients could fail cargo, repacking or factory matching solely because one had legitimate quest provenance. The fingerprint now keeps the native serialized identity fields `id`, `Count`, `tag` and `ForgeCaps`, with normalized count as before

`CreateRepackProof` now separately compares the exact observed parcel UUID set, actor and request between planned and actually consumed fragments. Thus removing sidecars from native item identity does not weaken the original parcel lineage requirement. Payload and native parcel conservation checks remain mandatory

Existing pending fingerprints that encoded observation keys may fail closed after this source change. They are not reconstructed or accepted from unknown lineage. A fresh native operation establishes new proof

### Family actor admission and nested hurt lifetime

CreatureCare, Wildlife, CombatTools and MagicLast now use the existing pure `TravelProof.eligible` predicate for live native actions. Wildlife retains its prior strict Survival-mode condition. CreatureCare and Wildlife emissions also recheck their family admission

Wildlife hurt observation could retain a frame if the actor died or ceased qualifying before return. It now always balances player frames, including rejected actors, and uses a bounded depth with an overflow counter. Nested rejected or overflow calls cannot borrow an outer actor's shield or Emu event. Final proof still uses the same target, damage source and final event state

At the parent's request, `TravelProof.authenticated(ServerPlayer)` became public with identical behavior and the same optional native IdentityPolicy bridge. The parent owns central trigger admission and can reuse that bridge without applying a global alive restriction to canonical accepted death or victory callbacks

### Global physical provenance stores could stop accepting fresh native work

Wildlife tool, setup and worm provenance and CreatureCare mouse and egg metadata had permanent capacity rejection despite stale or abandoned sources remaining recorded. They now conservatively evict the oldest exact metadata entry before admitting a new legitimate observation

Wildlife tool and worm-drop eviction also revoke the exact old shared physical batch UUID through the existing `NativeFoodSources.activate(id, false)` API. Tool origin snapshots are removed when their last batch metadata disappears. CreatureCare state or egg-location eviction invalidates the old lineage rather than reconstructing it. Worm settlement source eviction drops only that old observational source-to-participant link. No item, gameplay state, native encounter entitlement or reward is created or changed

### Caves movement episodes survived unrelated teleport

Caves hook, bounce, flight and possession movement observations could remain pending after a server teleport. The existing connection Mixin now clears those observations at the exact native `teleport(DDDFF, Set)` entry, including same-world corrections and normal cross-world teleports that use that connection path. Accepted native movement remains the completion seam. Clearing cannot award anything

### Remaining private Food DTO access from merged handlers

The broader linkage inspection found the same private nested DTO issue in `RemainingPitchforkGrowthMixin`, `RemainingVoidseekerQuestMixin` and `SeashellFoodQuestMixin`, outside the earlier `RemainingFood*` naming group. The parent authorized these narrow follow-ups. Their DTOs now use public normal-helper accessors in the existing `FoodTransferSnapshots`. Their original native operations and success requirements are unchanged. The Food/Home/Photo/World report contains the corresponding addendum

## Exact changed source paths

All paths below are relative to `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/`

- `integration/CaveResearchProof.java`
- `integration/SkarrierWorkshopProof.java`
- `integration/caves/CaveQuestProof.java`
- `integration/caves/CaveQuestMotion.java`
- `integration/combattools/CombatToolProof.java`
- `integration/create/CreateCargoTransfers.java`
- `integration/create/CreateRepackProof.java`
- `integration/creaturecare/CreatureCareProof.java`
- `integration/creaturecare/CreatureCareLineage.java`
- `integration/food/FoodTransferSnapshots.java`
- `integration/magiclast/MagicLastProof.java`
- `integration/travel/TravelProof.java`
- `integration/wildlife/WildlifeQuestProof.java`
- `integration/wildlife/WildlifeQuestHurt.java`
- `integration/wildlife/WildlifeQuestTools.java`
- `integration/wildlife/WildlifeQuestSetups.java`
- `integration/wildlife/WildlifeQuestWorm.java`
- `mixin/ArrowHitQuestMixin.java`
- `mixin/CaveMapResultQuestMixin.java`
- `mixin/CaveQuestMovementPacketMixin.java`
- `mixin/CaveQuestVanillaArrowDamageMixin.java` deleted
- `mixin/CombatForgeDispatchQuestMixin.java`
- `mixin/RemainingFoodCraftReceiptMixin.java`
- `mixin/RemainingPitchforkGrowthMixin.java`
- `mixin/RemainingVoidseekerQuestMixin.java`
- `mixin/SeashellFoodQuestMixin.java`

## Retained contracts and source observations

- Native optional dependencies remain gated by the existing central plugin. The new shared Cave damage and crafting-debit helpers use builtin types only. Generic arrow and Food crafting hooks are already unconditional registrations. New references do not make ordinary standalone gameplay depend on an upstream optional mod or JEMServer
- Filter comparison remains attached to one native configuration, endpoint and real committed transfer. Its existing independently reviewed comparison and conservation design was preserved
- Travel fuel stays an exact read projection of the native JEMCompat owner, with native engine-operation and pilot lineage. No elapsed-time fuel formula or observer-caused fuel decrement was introduced. Gouge and Nature lifecycle and accepted movement designs were retained
- Factory recipe mode still requires exact configured gauge identity, fulfilled native request vector, conserved parcels, actual unpacking into the selected native production path and genuine native result transfer. Cargo fingerprint correction does not substitute possession or a fabricated recipe
- CreatureCare mouse release still compares the original native captured state and actual accepted spawned entity, eggs retain physical harvest, paid placement and accepted native hatch lineage, and animal work still requires actual inventory or native state conservation
- Wildlife natural output conversion retains the finite shared physical budgets and exact actual pickup gain. Native tool wear retains origin lineage only after actual same-item native durability changes. Worm crafting accepts only canonical participant-linked native receipt material
- MagicLast continues to bind real cast generations to actual native results, invalidate new initiations or native resets, and wait for accepted native damage, healing, effects, relocation or spawn. Lightning exchange intentionally uses a successful native cancellation result, checked at final `EventBus.post` return. It was not incorrectly treated as an ordinary denied event
- Blood and alchemy still require actual native production and payment, output or fluid conservation and actual same-actor receipt. Their native recipes and random outcomes are unchanged
- Enderman valid OR alternatives and actual chest-slot hood behavior were retained. A partial native alternative is not described as a missing whole card. Workshop Invulnerite remains the user-approved native smithing objective
- Structure loot, terminal extraction, network crafting, pending native recipients and holiday gifts remain tied to canonical native owners. The owned holiday event is still optional and isolated from ordinary standalone quest completion
- No original eligible route, reward, identity or text was weakened. The eight parent-approved retirements and the approved Hunger correction remain parent content decisions and were not reopened

## Remaining integration work and acceptance limits

The parent must remove `CaveQuestVanillaArrowDamageMixin` from the central Mixin JSON and OptionalMixins map and remove or replace that deleted path in retained Caves registration and producer receipts. The replacement producer paths are `mixin/ArrowHitQuestMixin.java` and `integration/caves/CaveQuestProof.java`. All remain unaccepted until the parent's combined stage

No other new Mixin registration or compile-only dependency is required by this bucket. Central actor admission remains parent-owned and must consume the now-public authentication bridge as agreed

No further concrete source defect is left open from the inspected mechanisms. Conservative capacity eviction and old fingerprint invalidation can require a fresh legitimate native action. Current-source compilation, reobfuscation, optional-mod Mixin application and actual standalone runtime remain unverified and reserved for the parent's combined acceptance stage
