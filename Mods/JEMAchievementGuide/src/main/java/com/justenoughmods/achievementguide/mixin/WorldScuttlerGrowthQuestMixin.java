package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldShellGrowthProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.entity.Scuttler", remap = false)
public abstract class WorldScuttlerGrowthQuestMixin {
    @Unique private WorldShellGrowthProof.Meal jem$actualJellyMeal;
    @Unique private Entity jem$acceptedShellback;
    @Inject(method = "m_8107_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41671_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private void jem$nativeMealStarted(CallbackInfo ci) { jem$actualJellyMeal = WorldShellGrowthProof.eating((Mob)(Object)this); }
    @Inject(method = "m_8107_", at = @At(value = "INVOKE", target = "Ldev/obscuria/aquamirae/common/entity/Scuttler;setSizeVariant(F)V", shift = At.Shift.AFTER))
    private void jem$nativeGrowthCommitted(CallbackInfo ci) {
        WorldShellGrowthProof.grew((Mob)(Object)this, jem$actualJellyMeal); jem$actualJellyMeal = null;
    }
    @Inject(method = "evolveToShellback", at = @At("HEAD"))
    private void jem$beforeNativeEvolution(CallbackInfo ci) { jem$acceptedShellback = null; }
    @Redirect(method = "evolveToShellback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$actualNativeSpawn(Level level, Entity child) {
        boolean accepted = level.addFreshEntity(child);
        if (accepted) jem$acceptedShellback = child;
        return accepted;
    }
    @Inject(method = "evolveToShellback", at = @At("RETURN"))
    private void jem$nativeEvolutionFinished(CallbackInfo ci) {
        WorldShellGrowthProof.evolved((Entity)(Object)this, jem$acceptedShellback); jem$acceptedShellback = null;
    }
}
