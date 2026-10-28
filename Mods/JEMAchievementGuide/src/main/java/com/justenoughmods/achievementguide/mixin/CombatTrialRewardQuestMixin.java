package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatTrialProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;
import com.justenoughmods.achievementguide.mixin.CombatTrialStateQuestAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.salju.trialstowers.Trials;
import net.salju.trialstowers.block.TrialSpawnerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = "net.salju.trialstowers.block.TrialSpawnerEntity", remap = false)
public abstract class CombatTrialRewardQuestMixin {
    @Unique private static final ThreadLocal<Integer> jem$beforeMobs = new ThreadLocal<>();
    @Inject(method = "setUpMob", at = @At("HEAD"))
    private static void jem$beforeMob(TrialSpawnerEntity spawner, EntityType<?> type, ServerLevel level, int count, BlockPos pos, ItemStack egg, CallbackInfo callback) { jem$beforeMobs.set(((CombatTrialStateQuestAccess)spawner).jem$mobs().size()); }
    @Inject(method = "setUpMob", at = @At("RETURN"))
    private static void jem$mob(TrialSpawnerEntity spawner, EntityType<?> type, ServerLevel level, int count, BlockPos pos, ItemStack egg, CallbackInfo callback) { Integer before = jem$beforeMobs.get(); jem$beforeMobs.remove(); if (before != null && ((CombatTrialStateQuestAccess)spawner).jem$mobs().size() == before + 1) CombatTrialProof.spawnedLast(spawner); }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/salju/trialstowers/Trials;queueServerWork(ILjava/lang/Runnable;)V"))
    private static void jem$queue(int delay, Runnable original, Level level, BlockPos pos, BlockState state, TrialSpawnerEntity spawner) {
        Trials.queueServerWork(delay, CombatTrialProof.reward(spawner, original));
    }
}
