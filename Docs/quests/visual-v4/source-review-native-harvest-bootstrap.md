# Native harvest bootstrap correction

Source frozen pending parent build and ordinary Server restart

Server/logs/latest.log line518 reports WorldPrehistoricQuestHarvestMixin jem$nativeDrops succeeded0/1 while targeting Block.playerDestroy. The debug log shows application of that Mixin and no competing redirect warning for that site. This failure does not require a redirect collision explanation

Exact bytecode evidence comes from both the mapped Forge47.4.10 development jar and Server/libraries/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-server.jar. Block.playerDestroy / m_6240_ invokes Forge-added dropResources(BlockState,Level,BlockPos,BlockEntity,Entity,ItemStack,boolean) at bytecode offset28, after pushing false. The old selector targeted vanilla six-argument dropResources. Generated jem_advancements.refmap.json correctly mapped that wrong overload to m_49881_, which is absent from this call site

WorldPrehistoricQuestHarvestMixin now targets the exact seven-argument Forge method with only that Forge-added invocation remap disabled. Its playerDestroy target still maps normally. The handler forwards every actual argument, including the native boolean, through the unchanged WorldPrehistoricQuestDrops.harvest scope and calls the exact original overload once. No require0, suppression or proof bypass was introduced

There was a separate concrete source conflict: CaveQuestSulfurHarvestMixin and NatureHarvestQuestMixin both redirected the same ServerPlayerGameMode.destroyBlock -> Block.playerDestroy invocation. Actual Server bytecode confirms the native invocation at offset246, Block.m_6240_ with its original six parameters

New vanilla-only NativePlayerHarvestQuestMixin owns that one actual call. Public optional witness interfaces in integration/NativeHarvestWitness.java compose the two observers around it. Existing Cave and Nature Mixins retain their native HEAD snapshots and final RETURN processing, implement their separate witness interface and no longer redirect the call. The dispatcher only sees an interface when that optional Mixin actually merged, so each family remains independent when the other mod is absent and no optional native class is eagerly loaded

Cave retains exact player/world identity, origin block-state equality, actual hazmat, native sulfur block, actual removed block and accepted native item output. Nature retains exact actor/world/position/state matching, actual scoped native drops and final successful destroyBlock result. Original playerDestroy executes once with unchanged arguments

Files changed under Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide

- mixin/WorldPrehistoricQuestHarvestMixin.java
- mixin/CaveQuestSulfurHarvestMixin.java
- mixin/NatureHarvestQuestMixin.java
- New mixin/NativePlayerHarvestQuestMixin.java
- New integration/NativeHarvestWitness.java

Parent central request: build/questbook-v4-native-harvest-bootstrap-registration.json. Add NativePlayerHarvestQuestMixin unconditionally, preserve all three existing optional registrations. No registration removal is required

Read startup logs, exact class bytecode and generated refmap. No build, checker, automated verification, runtime/gameplay test or restart was run by this worker. Parent owns build and normal startup confirmation. Runtime acceptance remains false
