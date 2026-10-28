# Overall source review bucket B

Source frozen for parent acceptance and the single overall production build

Scope: integration/battle, battlearmor, battlefinal, companions, petutilities, prehistoric and spawncare, plus their corresponding Mixins. Includes final frozen Epic/Nightfall/Damage/Relics, BattleFinal and root Cataclysm weapon additions. Existing registrations and family receipts retain false source/runtime flags until parent acceptance

No tests, checker, automated verification, build, runtime, game, Git or subagent was run. Native source and installed class/API inspection were used. No central, book, Atlas, compiler, migration, recipe or gameplay ownership files were changed

## Material fixes

All paths below are relative to Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide

- integration/companions/CompanionQuestHolyCraft.java and mixin/CompanionQuestHolyCraftMixin.java now observe the existing public NativePreparedMealProof begin/crafted/end paid native crafting receipt. They certify the actual new unstackable Holy armor output after a real inventory/cursor gain. Shift transfer no longer depends on a depleted ResultSlot onTake stack. Original recipe object, native output and consumed input receipt remain authoritative. Existing identical preexisting armor cannot borrow the output proof
- integration/companions/CompanionQuestSpell.java now binds created native projectiles to the actual native use scope, requires its normal committed return and real registered world output, and preserves the installed books' successful native PASS result. Native blood weapons retain their actual consuming result. Delayed owner-created MagicRay/StoneSpike projectiles carry the committed use receipt into their original scheduled world insertion. Actor identity requires the canonical same-world online player
- mixin/CompanionQuestAcceptedSpawnMixin.java, PetUtilityQuestSpawnMixin.java and WorldPrehistoricQuestAcceptedSpawnMixin.java require actual accepted registered nonremoved world entities. Child projectile lineage requires actual registered output
- integration/prehistoric/WorldPrehistoricQuestDart.java and new mixin/WorldPrehistoricQuestDartDamageMixin.java now require the exact dart's accepted real positive damage, its real owner and the accepted poison in the same actual impact. An unrelated nested health loss cannot stand in for rejected dart damage
- integration/prehistoric/WorldPrehistoricQuestDrops.java, WorldPrehistoricQuestAsync.java and mixin/WorldPrehistoricQuestPickupMixin.java bind actual native drops to their exact saved output/count before pickup attribution. Altered or partially removed unknown output fails closed
- integration/spawncare/SpawnCareQuestContainers.java retires the old own pigment physical-source batch when the actual native cosmetic recipe consumes that filled item. Repeated recosmetics no longer orphan active batches in the shared bounded source store
- integration/battle/BattleQuestNightfall.java requires the same actual weapon stack throughout an active native attempt. A second same-ID weapon cannot complete the first weapon's paid attempt
- integration/battle/BattleQuestDamage.java and integration/battlefinal/BattleFinalQuestLife.java reject nonfinite vitality observations
- mixin/BattleFinalQuestLifeBeginMixin.java starts bounded optional family hurt frames before native cancellable HEAD handlers. Final priority500 RETURN observers in BattleQuestDamageMixin, CompanionQuestSpellDamageMixin, WorldBattleArmorQuestHurtMixin and WorldPrehistoricQuestDartDamageMixin balance every nested call, including early native cancellation. Normal helper DTOs own cross-injection data. No private Mixin nested DTO is accessed from a merged target
- integration/battlearmor/WorldBattleArmorQuestDamage.java owns balanced nullable hurt frames. CompanionQuestSpell and WorldPrehistoricQuestDart own equivalent bounded normal helper frames. integration/battlefinal/BattleFinalQuestLifecycle.java clears their transient contexts under exact optional-mod guards on server stop
- mixin/WorldBattleExtraDamageQuestMixin.java uses a bounded nested health snapshot stack instead of one overwritten scalar
- mixin/BattleQuestAcceptedMoveMixin.java, WorldBattleArmorQuestAcceptedMovementMixin.java and PetUtilityQuestVehiclePacketMixin.java begin packet observation after native PacketUtils.ensureRunningOnSameThread. The first two require the native lastGoodZ accepted movement commit and no pending correction. Actual physical surface/flight/backstep conditions remain required. Netty handoff does not establish an ability receipt

## Native resume persistence

Source evidence: build/battle-schools-native/cataclysm/root-extra/source/com/github/L_Ender/cataclysm/capabilities/ChargeCapability.java and RenderRushCapability.java. Both serialize/deserialize their actual active flag and timer. Therefore the old transient-only WorldBattleExtraMovement.RIDES could lose the already paid native release after restart while its native owner resumed

Fixed integration/battle/WorldBattleExtraMovement.java and WorldBattleExtraLifecycle.java, plus mixin/WorldBattleExtraMovementQuestMixin.java. Added integration/battle/WorldBattleExtraStackCarrier.java, mixin/WorldBattleReleaseStackQuestMixin.java and WorldBattleReleaseCapabilityQuestMixin.java

The minimal player receipt stores native kind, actual original release force observation, actor/source UUID, native item ID and dimension. A separate external ItemStack serialization sidecar carries the physical source token outside ordinary item tags. It does not consume the shared physical portion budget or interfere with WorldBattleEquipmentOrigins' persisted mode correspondence

Restore requires the same canonical survival player, actual held count1 physical token, same item/kind/dimension and the actual native active capability with a positive native timer. The bounded runtime map holds at most256 actors. Native capability tick clears ended casts. Logout suspends only the runtime reference, dimension/clone and confirmed live interruption clear the receipt. Initial login synchronization cannot erase a still-native-active resumed release before observation can restore it. Actual accepted movement or actual native impact still provides completion. No timer, velocity, player position, cooldown or native capability is restored or written

Native TidalHook owns an actual stack identity relation that itself breaks on reload, so that native job receives no invented resume behavior. Already earned Soul/Tidal/Ceraunus mode correspondence remains the existing persistent physical-source WorldBattleEquipmentOrigins state

## Exact Skull Mixin 0.8.5 contract

Inspected exact cached org.spongepowered:mixin:0.8.5 jar at C:/Users/user/.gradle/caches/modules-2/files-2.1/org.spongepowered/mixin/0.8.5/9d1c0c3a304ae6697ecd477218fa61b850bf57fc/mixin-0.8.5.jar. Selected transformer source cache: build/overall-review-b-native/mixin-source/org/spongepowered/asm/mixin/transformer

MixinPreProcessorStandard.conform delegates injector renaming to MethodMapper. attachUniqueMethod returns without renaming when the helper has no target collision and is not synthetic. Private/protected unique methods are renamed for collisions or synthetic handling, rather than unconditionally

The exact installed Cataclysm LivingEntityMixin helper dev1_21_1$handleStunEffectLogic has descriptor (MobEffectInstance,Entity,CallbackInfoReturnable)V and flags0x0002 ACC_PRIVATE with no ACC_SYNTHETIC. It is an explicit noninjector helper. Source cache is build/battle-schools-native/cataclysm/root-mixins/source/com/github/L_Ender/cataclysm/mixin/LivingEntityMixin.java. javap inspection of the cached original class confirmed those flags. No matching helper declaration is supplied by the owned target sources, only Guide injection selectors

Thus the installed noncolliding helper name survives under the inspected transformer behavior. WorldBattleArmorQuestSkullNativeMixin at priority900 remains valid on this exact native contract after Cataclysm priority1000 merge. Dynamic is documentary, not the justification. Proof still requires actual native Stun rejection, real new cooldown and the final public addEffect false/no-Stun result. Runtime Mixin application remains pending the parent build/runtime gate

## Remaining family conclusions

Reviewed committed owner/result gates, native crafting/smithing output identity, delayed entity/drop ownership, physical split/copy/drop lineage, preview lifetime, optional lifecycle access and actual installed descriptors across the assigned frozen families. Native algorithms, costs, cooldowns, accepted target policy, equipment semantics and recipes were preserved. Root BattleQuestProof and BattleFinalQuestProof both use TravelProof.eligible for the real survival actor. New source-only helper references preserve those public signatures

Existing paid smith output and WorldBattleEquipmentOrigins public certified/has/confirmed APIs were reused without modifying their physical-source store or ordinary item tags. Armor source retains all8 native smith transforms through the shared parent implementation, actual Ignitium leggings/boots/Elytra flight, actual Cursium backstep/dodge and all3 assigned defensive Curios routes. Revival was not introduced as a condition

No new whole-card impossibility or unsupported advertised route was introduced by this source pass. Previously recorded unsupported routes remain explicit and false, including native client-only Phantom Ascent. Retired rows and user-approved native condition corrections remain governed by their existing receipts. No runtime coverage is claimed

## Parent integration

Normalized additions and existing HolyCraft target metadata correction: build/questbook-v4-overall-review-b-registration.json

Three new Mixins require central registration: WorldPrehistoricQuestDartDamageMixin, WorldBattleReleaseStackQuestMixin and WorldBattleReleaseCapabilityQuestMixin. No new installed compile dependency beyond the already requested exact Cataclysm3.31 and lionfishapi3.0 jars

Source changes are frozen. Parent acceptance, central reconciliation, one overall production build and future runtime evidence remain pending
