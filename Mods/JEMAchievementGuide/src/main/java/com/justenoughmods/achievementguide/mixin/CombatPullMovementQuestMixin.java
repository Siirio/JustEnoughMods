package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatPullProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class CombatPullMovementQuestMixin {
    @Inject(method = "move", at = @At("RETURN"))
    private void jem$moved(MoverType type, Vec3 movement, CallbackInfo callback) {
        Entity target = (Entity)(Object)this;
        if (!(target instanceof ServerPlayer)) CombatPullProof.moved(target);
    }
}
