# Guide cooking and fishing redirect ownership fixes

Source frozen after the startup-3 Tide collision diagnosis. No build, checker, test, startup or Git action was run by this worker.

## Actual failures and fixes

The startup-2 failure in `Server/logs/latest.log` reported JEMServer's `cooking.CampfireCookingMixin.jem$cooked` failing its 0/1 redirect check. Both Guide and JEMServer had replaced the same `CampfireBlockEntity.cookTick` invocation of `Containers.dropItemStack`.

Guide's competing campfire redirect was removed. The already-registered `CombatNativeDropQuestMixin` now observes `Containers.dropItemStack` at HEAD, receiving the actual native output after caller cooking modifiers. The native helper and existing JEMServer owner keep their original call. Existing native selected input, provisional sidecar lineage, accepted entity spawn and actual campfire slot removal still govern final proof.

The startup-3 failure reported JEMServer's `fishing.TideFishingHookMixin.jemCatchExperience` failing its 0/1 redirect check. Guide and JEMServer both replaced the same `TideFishingHook.retrieve` invocation of `Level.addFreshEntity`.

Guide's competing Tide redirect was removed. The existing `RemainingFoodNativeInsertionMixin` now forwards the concrete `ServerLevel.addFreshEntity` RETURN result to `TideCatchProof.item`, only while Tide is installed. The existing exact native retrieve scope, authenticated actor, selected catch kind, contextual hook and native hooked-item object identity remain required. Non-item entities and rejected spawns cannot earn this proof.

JEMServer's original XP suppression remains the sole caller policy. If it suppresses an ExperienceOrb, it never reaches the concrete insertion method. Guide adds no second spawn or XP call. Without JEMServer, the same observer receives Tide's ordinary native spawn result. No new essential dependency or Mixin registration was added.

## Focused remaining overlap inspection

Read-only source inspection covered all four native redirect sites declared by the current JEMServer cooking/fishing Mixin owners.

| Existing JEMServer owner | Native instruction | Guide overlap and result |
| --- | --- | --- |
| `cooking.CampfireCookingMixin` | `CampfireBlockEntity.cookTick` to `Containers.dropItemStack` | Guide redirect removed, normal public-helper observation retained |
| `fishing.TideFishingHookMixin` | `TideFishingHook.retrieve` to `Level.addFreshEntity` | Guide redirect removed, concrete accepted-spawn observation retained |
| `cooking.BakingStationMixin` | `BlankCakeBlock.use` to `Level.addFreshEntity` | No same-instruction Guide redirect. `BakingSurfaceQuestMixin` observes the native `Level.setBlock` operation instead |
| `fishing.TideInformationalBlockScanMixin` | `FishingInfoManager.scanForNearbyInformationalBlocks` to `ServerLevel.getBlockState` | No Guide Mixin targeting this native owner/method |

Other existing JEMServer cooking owners use HEAD/RETURN injections rather than replacement redirects. Tide crate creation and crate loot proof target `LootCrateEntity.create` and `addCrateLoot`, which are different native owners from the existing JEMServer redirects. Their original operations and exact crate proof remain unchanged. FishingReal conversion proof remains unchanged.

## Changed Guide sources

- `mixin/RemainingCampfireGameCookingMixin.java`, removed competing caller redirect
- `mixin/CombatNativeDropQuestMixin.java`, public-helper cooking output observation
- `mixin/RemainingTideCatchQuestMixin.java`, removed competing caller redirect, kept exact native scope
- `mixin/RemainingFoodNativeInsertionMixin.java`, optional Tide accepted-spawn dispatch

Paths above are relative to `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/`. JEMServer source, behavior, registration and configuration were not edited. Runtime success remains unclaimed until the parent's next authorized startup.
