package com.justenoughmods.achievementguide.mixin;
import java.util.List;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
@Pseudo
@Mixin(targets = "net.salju.trialstowers.block.TrialSpawnerEntity", remap = false)
public interface CombatTrialStateQuestAccess {
    @Invoker("isActivelySpawning") boolean jem$active();
    @Invoker("getRemainingEnemies") int jem$remaining();
    @Invoker("getMobs") List<UUID> jem$mobs();
}
