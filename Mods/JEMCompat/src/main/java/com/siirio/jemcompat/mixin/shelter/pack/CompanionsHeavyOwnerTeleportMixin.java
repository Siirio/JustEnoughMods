package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.CompanionData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"dev.xylonity.companions.common.entity.ai.antlion.tamable.goal.AntlionAdultFollowOwnerGoal",
        "dev.xylonity.companions.common.entity.ai.teddy.goal.MutatedTeddyFollowOwnerGoal"}, remap = false)
public abstract class CompanionsHeavyOwnerTeleportMixin {
    @Unique private Entity jemShelter$pet;
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void jemRememberNativePet(@Coerce Object pet, double minimum, double start, double speed, float factor,
                                      CallbackInfo callback) { jemShelter$pet = (Entity) pet; }
    @Inject(method = "teleportToOwner", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemPauseTeleportOnly(CallbackInfo callback) {
        if (jemShelter$pet != null && jemShelter$pet.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) callback.cancel();
    }
}
