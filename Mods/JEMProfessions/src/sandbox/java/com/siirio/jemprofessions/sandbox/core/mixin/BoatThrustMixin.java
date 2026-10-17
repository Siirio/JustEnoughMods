package com.siirio.jemprofessions.sandbox.core.mixin;

import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Boat.class)
public abstract class BoatThrustMixin {
    @ModifyConstant(method = "controlBoat", constant = { @Constant(floatValue = .04F), @Constant(floatValue = .005F) })
    private float jem$thrust(float amount) {
        Boat boat = (Boat) (Object) this;
        return (boat.getClass() == Boat.class || boat.getClass() == ChestBoat.class) && boat.isInWater()
                && SandboxProfessionSupport.boatBoostClientEnabled() ? amount * 1.08F : amount;
    }
}
