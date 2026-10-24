package com.siirio.mobhome.mixin.shelter.alex;

import com.github.alexthe666.alexsmobs.entity.EntityCosmaw;
import com.siirio.mobhome.feature.shelter.CompanionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve the pickup goal's navigation and rescue logic; intercept only its explicit owner teleport. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCosmaw$AIPickupOwner", remap = false)
public abstract class AlexCosmawPickupTeleportMixin {
    @Group(name = "jemcompat$cosmawOwnerTeleport", min = 1, max = 1)
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityCosmaw;m_6021_(DDD)V"), require = 0)
    private void jemcompat$productionOwnerTeleport(EntityCosmaw cosmaw, double x, double y, double z) { teleport(cosmaw, x, y, z); }

    @Group(name = "jemcompat$cosmawOwnerTeleport", min = 1, max = 1)
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityCosmaw;teleportTo(DDD)V"), require = 0)
    private void jemcompat$mappedOwnerTeleport(EntityCosmaw cosmaw, double x, double y, double z) { teleport(cosmaw, x, y, z); }

    private static void teleport(EntityCosmaw cosmaw, double x, double y, double z) {
        if (!cosmaw.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) cosmaw.teleportTo(x, y, z);
    }
}
