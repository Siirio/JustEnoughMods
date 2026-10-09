package com.siirio.jemcompat.mixin.transport;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon", remap = false)
public abstract class TrottingWagonClydesdaleMixin {
    private static final double CLYDESDALE_MAXIMUM_SPEED_MULTIPLIER = 1.15;
    private static final double CLYDESDALE_ACCELERATION_MULTIPLIER = 1.25;
    private static final ResourceLocation CLYDESDALE = new ResourceLocation("barnyardbuddies", "clydesdale_horse");
    private static final ResourceLocation TRADER_CLYDESDALE = new ResourceLocation("barnyardbuddies", "trader_clydesdale");

    @Shadow @Final private Mob[] horses;
    @Shadow @Final public double speed;
    @Shadow @Final private double acceleration;

    @Redirect(method = "handleAcceleration", at = @At(value = "FIELD",
            target = "Lnet/favouriteless/trotting_wagons/common/entities/base/AbstractWagon;speed:D"))
    private double jemcompat$clydesdaleMaximumSpeed(AbstractWagon wagon) {
        return clydesdaleAttached() ? speed * CLYDESDALE_MAXIMUM_SPEED_MULTIPLIER : speed;
    }

    @Redirect(method = "handleAcceleration", at = @At(value = "FIELD",
            target = "Lnet/favouriteless/trotting_wagons/common/entities/base/AbstractWagon;acceleration:D"))
    private double jemcompat$clydesdaleAcceleration(AbstractWagon wagon) {
        return clydesdaleAttached() ? acceleration / CLYDESDALE_ACCELERATION_MULTIPLIER : acceleration;
    }

    private boolean clydesdaleAttached() {
        for (Mob horse : horses) {
            if (horse == null) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(horse.getType());
            if (CLYDESDALE.equals(id) || TRADER_CLYDESDALE.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
