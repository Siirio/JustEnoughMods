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

/** All four verified native goal constructors have (pet, double, float, float, boolean). */
@Pseudo
@Mixin(targets = {
        "dev.xylonity.companions.common.entity.ai.generic.CompanionFollowOwnerGoal",
        "dev.xylonity.companions.common.entity.ai.generic.CompanionSummonFollowOwnerGoal",
        "tech.alexnijjar.endermanoverhaul.common.entities.pets.BasePetEnderman$EndermanFollowOwnerGoal",
        "com.github.L_Ender.cataclysm.entity.Pet.AI.TameableAIFollowOwnerWater"}, remap = false)
public abstract class PackFollowOwnerTeleportMixin {
    @Unique private Entity jemShelter$pet;
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void jemRememberNativePet(@Coerce Object pet, double speed, float firstDistance, float secondDistance,
                                      boolean leaves, CallbackInfo callback) { jemShelter$pet = (Entity) pet; }
    @Inject(method = {"teleportToOwner", "tryToTeleportNearEntity"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void jemPauseTeleportOnly(CallbackInfo callback) {
        if (jemShelter$pet != null && jemShelter$pet.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) callback.cancel();
    }
}
