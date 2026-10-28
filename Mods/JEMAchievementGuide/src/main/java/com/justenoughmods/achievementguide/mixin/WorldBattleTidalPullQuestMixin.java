package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraMovement;
import com.github.L_Ender.cataclysm.entity.projectile.Tidal_Hook_Entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Tidal_Hook_Entity", remap = false)
public abstract class WorldBattleTidalPullQuestMixin {
    @Shadow private boolean isPulling;
    @Unique private Vec3 jem$beforeNativeHook;
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$before(CallbackInfo ci) {
        Entity owner = ((Tidal_Hook_Entity)(Object)this).getOwner(); jem$beforeNativeHook = owner == null ? null : owner.getDeltaMovement();
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$after(CallbackInfo ci) {
        if (jem$beforeNativeHook != null) WorldBattleExtraMovement.pulled((Tidal_Hook_Entity)(Object)this, jem$beforeNativeHook, isPulling);
        jem$beforeNativeHook = null;
    }
}
