# Combined source review: Food, Home, Photo and World native observers

This is the assigned bucket of the single overall pre-build source pass. Source inspection and necessary fixes are complete within the assigned ownership. Central registration, build configuration, quest content, blueprints, Atlas and other workers' families were not edited

`source_reviewed = false`, `source_accepted = false`, `runtime_verified = false` remain pending the parent's combined acceptance. No checker, test, fixture, compilation, Gradle task, game, server probe, activation, synchronization or Git action was run

## Material defects fixed

1. Three food transfer Mixins defined private nested records and accessed their private record fields from injected handlers. After Mixin merges those handlers into vanilla classes, the vanilla targets are not Java nestmates of the original nested records. The transfer snapshots now live in a normal helper as public records and the injected handlers use public accessors
2. Food Shift-click provenance allocated certified portions in ascending slot order even when native `moveItemStackTo` traversed in reverse. It also needed the native distinction between merging existing stacks and filling empty slots. Allocation now follows the selected traversal direction within those two native phases. The actual moved and gained quantities must still agree, unknown portions still leave first, and the shared source budget remains finite
3. Shift-crafted prepared meals could lose all recipe proof because native crafting menus pass the emptied original result stack to `ResultSlot.onTake` after moving it. A call-local identity map now captures that exact native result before the move, including initially uncertified outputs. Only the same emptied stack identity can use the snapshot. Actual native recipe selection, ingredient removals and personal output gain remain mandatory. The selected recipe must also be the live recipe-manager object
4. Food and Home admission did not consistently require the live authenticated logical-server actor. Their shared admission helpers now reuse the existing pure `TravelProof.eligible` predicate. Food menu capture and collection also require the current valid menu, and dropped-food emission and prepared-meal settlement recheck admission. This adds no service or gameplay authority
5. `NativeFoodSources` stopped issuing all future batches or planted origins after reaching its permanent 16,384-entry limits. New valid observations now revoke the oldest exact batch UUID or plant key before insertion. Existing batch IDs and all public signatures stay compatible. Missing batch claims are removed from a new output's claim calculation before capacity checks, while live provisional claims remain intact. Revoked IDs immediately fail existing ownership and consume lookups, including other families' live-budget checks. This is bounded conservative observation loss, with no inventory, recipe, reward or gameplay change
6. Photo repository acceptance did not enforce the pending receipt deadline unless another output happened first. The native accepted-image callback now removes expired entries before matching and awarding them, using the existing deadline and no polling

## Exact changed source paths

- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/food/FoodTransferSnapshots.java` added public normal-helper transfer DTOs
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/food/NativeFoodSources.java` bounded revocation and stale-claim pruning
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/food/NativePreparedMealProof.java` exact pre-move result snapshot, canonical recipe identity and actor rechecks
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/food/RemainingFoodActors.java` shared live actor admission
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/food/FoodOutputProof.java` live actor and valid current-menu capture and settlement
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/food/FoodDroppedProof.java` live actor emission guard
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/home/HomeInteractionProof.java` shared live actor admission
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/integration/photo/PhotoImages.java` pending expiry at accepted native upload
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/mixin/RemainingFoodEntitySourceMixin.java` accessible transfer DTO and accessors
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/mixin/RemainingFoodMenuSourceTransferMixin.java` accessible DTO, native transfer order and actual crafting-result capture
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/mixin/RemainingFoodSlotSourceTransferMixin.java` accessible transfer DTO and accessors
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/mixin/RemainingFoodCraftReceiptMixin.java` settle the exact native pre-move result when its original stack was emptied

No new Mixin or optional dependency registration is required for these changes

## Native evidence and retained behavior

The family ledgers and implementation reports were read before source inspection. Native cached sources and installed API bytes were consulted where the outcome, call order or descriptor needed confirmation

- Vanilla `ResultSlot.onTake`, native crafting quick moves and `AbstractContainerMenu.moveItemStackTo` account for the emptied result and transfer order fixes. The installed mapped `AbstractContainerMenu` also has the exact `lambda$doClick$4(ItemStack, Slot, Player, ItemStack)` signature used by the existing cursor transfer observer
- Food batch copy, split, count changes, entity merging, inventory insertion, cursor collection and recipe spending continue to share the same finite batch UUID budgets. Certification remains observational. Native calls are not repeated or replaced with recipes synthesized by the observer
- Installed Farm and Charm `ImplementedInventory` exposes the SRG default methods `m_6836_` and `m_6211_`. The existing compiled inventory adapter also forwards to those methods. The four native interface-default forwards were preserved rather than rewritten from an unproved remapping assumption. The existing artifact is API evidence only, not acceptance of the current source
- Chimes' native event handler commits carving without canceling the event. Its observer requires the actual bamboo-to-carved block transition. Native Immersive Melodies note handling is the server relay itself, and the observer validates the note domain and held instrument in addition to the real actor
- Home placement observations remain deferred until the real item-use return and retained committed state after Forge cancellation or rollback. Copper transport still uses the same assembled golem's actual native chest debit, carried stack and destination gain. Fairy light, doll, register, snow, restoration and signal observers retain their physical native call lineage
- Photo output continues to require the native camera/frame owner or the paid native Lightroom output, real ingredient debit and the original repository's accepted image. Remote camera execution is supplied by the native optional executing player. RGB physical sheets still consume the same actor's prior channel provenance
- The shark consume seam is the actual native `ItemEntity.discard` in `AbstractOceanicShark.m_8119_`. The similarly named tick lambda is only the query predicate, so the existing direct seam is correct
- Scuttler's native stolen item is its main-hand stack. Native growth follows actual food completion and size change, native evolution requires an accepted child spawn, and Shellback completion follows the actual tame-owner mount. Existing native pickup, food debit and same-actor lineage were retained
- Ocean observation retains the exact distance, line of sight, native movement and damage-free interval. Eagle combat contributions are added after positive actual damage before vanilla death settlement, and native combat-history reset clears the contribution lineage
- Abyssal smithing retains the selected live recipe identity, real input debit and native result receipt. Bamboo conversion, pollen use, Niveis drinking and Maelstrom arrival retain native committed outcome and actor checks
- Hunger retains the user-approved zero-food route. The observer requires native `PlayerHandler.isInvulnerableTo` protection inside the same real damage attempt, the exact acquired meal effect, zero food, unchanged health and native rejected damage. The earlier inference that positive-food protection was absent was incorrect. Positive-food protection exists through `MixinPlayer.modifyDamageAmount`, `PlayerHandler`, `LivingEntityHooks` and `AquamiraeCombatRules`. None of those gameplay owners were changed

## Unresolved material issues and acceptance limits

No additional concrete source defect remains open from this assigned pass. Older source provenance can be conservatively revoked at bounded capacity, after which that old source cannot earn proof and the player must perform a fresh native action. This is intentional invalidation rather than a restoration of unknown lineage

Current-source compilation, reobfuscation, optional-mod Mixin application and standalone runtime behavior remain unverified. These are reserved for the parent's one combined acceptance and build stage. No receipt, review flag or runtime flag was promoted by this handoff

## Authorized cross-family findings during bucket C

The parent explicitly authorized the following narrow follow-ups as part of the same overall source pass, without starting another family review

- `mixin/RemainingPitchforkGrowthMixin.java`, `mixin/RemainingVoidseekerQuestMixin.java` and `mixin/SeashellFoodQuestMixin.java`, below the same Guide Java source root, also used private nested DTOs from merged handlers. Their `PitchforkRun`, `VoidMeal` and `SeashellReceipt` now live in the existing normal `integration/food/FoodTransferSnapshots.java` helper with public constructors and accessors. Native growth, teleport and loot-consumer calls and all success conditions remain unchanged
- `mixin/RemainingFoodCraftReceiptMixin.java` and native `mixin/CaveMapResultQuestMixin.java` both redirected the identical `ResultSlot.onTake` ingredient removal. Food now retains the single original removal and passes its exact returned stack to public `CaveResearchProof.CraftDebitWitness`, implemented by the map observer. Food, Photo and Wildlife receipt consumers still receive the original actual debit list, and the map still requires all nine original debits

The source paths in this addendum are relative to `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/`. All acceptance and runtime flags remain false

## Authorized native consumed-effect persistence fix

Package acceptance found that native Maelstrom Omen and Devouring Curse survive ordinary save, restart and logout, while the observer's consumed-food origins existed only as weak runtime references. That lost a required stage despite the original native effect still being active

`integration/worldmisc/WorldMaelstromProof.java` and `integration/worldmisc/WorldHungerGuardProof.java` now write a minimal actor-UUID origin marker into native player ForgeData only after the existing actual paid Niveis Tear or Void Squid consumption and active native effect checks. The effects and markers share native player persistence. The marker alone cannot grant a stage

`integration/worldmisc/WorldNativeLifecycle.java` restores bindings at actual player login. The normal proof lookup can also restore an absent runtime cache. Both require the matching saved actor UUID and corresponding actual active native effect. A missing or inactive effect or a mismatched live effect invalidates the marker. Unknown old state without a consumed-food marker remains ineligible

New `mixin/WorldConsumedEffectRemovalQuestMixin.java` observes the exact mapped Forge 1.20.1 `LivingEntity.onEffectRemoved(MobEffectInstance)` tail. Actual committed removal of the matching native effect revokes only that effect's runtime binding and origin marker. Canceled removal does not reach this seam. Logout and stop clear runtime references while preserving saved origins for effects that remain native-active. Successful actual Maelstrom arrival consumes its origin marker before the native owner removes the effect

No effect, hunger value, teleport progress or gameplay state is set, removed or reconstructed by the observer. The approved zero-food Hunger condition and original destination and damage proofs remain unchanged

Parent registration request: `name = WorldConsumedEffectRemovalQuestMixin`, `qualified_name = com.justenoughmods.achievementguide.mixin.WorldConsumedEffectRemovalQuestMixin`, `dependencies = [aquamirae]`. No compile-only dependency is needed. All four changed or new Java paths are under the same source root used above

This is the same overall source pass, with no checker, fixture, build, game or runtime execution. Source acceptance and runtime flags remain false
