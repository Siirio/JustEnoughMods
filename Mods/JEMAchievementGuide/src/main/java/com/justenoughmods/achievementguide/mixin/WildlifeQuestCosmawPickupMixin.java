package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCosmaw$AIPickupOwner", remap = false)
public abstract class WildlifeQuestCosmawPickupMixin {
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_20329_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$rescuedOwner(LivingEntity owner, Entity vehicle) {
        boolean accepted = owner.startRiding(vehicle); var actor = owner instanceof Player player ? WildlifeQuestProof.real(player) : null;
        if (accepted && actor != null && owner.getVehicle() == vehicle && vehicle instanceof TamableAnimal cosmaw && cosmaw.isTame()
                && actor.getUUID().equals(cosmaw.getOwnerUUID())) WildlifeQuestProof.mark(cosmaw, "cosmaw_native_pickup", actor.getUUID());
        return accepted;
    }
}
