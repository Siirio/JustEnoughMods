package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.justenoughmods.achievementguide.integration.travel.TravelThermalObservation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.api.movement.ParagliderPlayerStates;
import tictim.paraglider.impl.movement.ServerPlayerMovement;

@Mixin(value = ServerPlayerMovement.class, remap = false)
public abstract class ParagliderThermalQuestMixin implements TravelThermalObservation {
    @Unique private Vec3 jem$thermalPosition;
    @Unique private int jem$thermalTick;
    @Unique private double jem$beforeThermalY;
    @Unique private boolean jem$beforeThermal;

    @Unique private ServerPlayerMovement jem$movement() { return (ServerPlayerMovement) (Object) this; }
    @Unique private boolean jem$nativeThermal() {
        var movement = jem$movement();
        var actor = movement.player();
        return TravelProof.eligible(actor) && !actor.onGround() && !actor.isPassenger()
                && !actor.getAbilities().flying && !actor.isFallFlying() && !actor.noPhysics
                && movement.state().id().equals(ParagliderPlayerStates.ASCENDING)
                && movement.state().has(ParagliderPlayerStates.Flags.FLAG_PARAGLIDING)
                && movement.state().has(ParagliderPlayerStates.Flags.FLAG_ASCENDING);
    }

    @Inject(method = "applyMovement()V", at = @At("HEAD"))
    private void jem$beforeThermal(CallbackInfo callback) {
        jem$beforeThermal = jem$nativeThermal();
        jem$beforeThermalY = jem$movement().player().getDeltaMovement().y;
        if (!jem$beforeThermal) jem$thermalPosition = null;
    }

    @Inject(method = "applyMovement()V", at = @At("RETURN"))
    private void jem$nativeThermalImpulse(CallbackInfo callback) {
        ServerPlayer actor = jem$movement().player();
        double after = actor.getDeltaMovement().y;
        if (jem$beforeThermal && jem$nativeThermal() && Double.isFinite(after) && Double.isFinite(jem$beforeThermalY)
                && after > 0 && after > jem$beforeThermalY) {
            jem$thermalPosition = actor.position();
            jem$thermalTick = actor.tickCount;
        }
    }

    @Override public void jem$thermalAccepted(Vec3 from) {
        ServerPlayer actor = jem$movement().player();
        Vec3 proof = jem$thermalPosition;
        jem$thermalPosition = null;
        if (proof != null && jem$nativeThermal() && actor.tickCount >= jem$thermalTick && actor.tickCount - jem$thermalTick <= 2
                && TravelProof.finite(from) && TravelProof.finite(actor.position()) && proof.distanceToSqr(from) <= 1.0E-8
                && actor.getY() > from.y && actor.position().distanceToSqr(from) - actor.getDeltaMovement().lengthSqr() <= 100.0) {
            TravelProof.emit(actor, "v3_world_thermal");
        }
    }
}
