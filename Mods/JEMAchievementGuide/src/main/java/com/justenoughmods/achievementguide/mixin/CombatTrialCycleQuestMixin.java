package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatTrialProof;
import java.util.UUID;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.salju.trialstowers.block.TrialSpawnerEntity", remap = false)
public abstract class CombatTrialCycleQuestMixin {
    @Shadow public abstract boolean isActivelySpawning();
    @Unique private boolean jem$wasActive;
    @Inject(method = "setActivity", at = @At("HEAD"))
    private void jem$before(boolean active, CallbackInfo callback) { jem$wasActive = isActivelySpawning(); }
    @Inject(method = "setActivity", at = @At("RETURN"))
    private void jem$after(boolean active, CallbackInfo callback) { CombatTrialProof.activated((BlockEntity)(Object)this, jem$wasActive, isActivelySpawning()); }
}
