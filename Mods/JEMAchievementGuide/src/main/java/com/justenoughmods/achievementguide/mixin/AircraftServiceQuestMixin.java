package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VehicleEntity.class, remap = false)
public abstract class AircraftServiceQuestMixin {
    @Unique private float jem$repairHealth;
    @Unique private ServerPlayer jem$repairActor;

    @Inject(method = "m_6096_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), remap = false)
    private void jem$beforeRepair(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$repairActor = null;
        var vehicle = (VehicleEntity) (Object) this;
        if (player instanceof ServerPlayer actor && TravelProof.eligible(actor) && vehicle.onGround()
                && vehicle.level() == actor.level() && !vehicle.hasPassenger(actor)
                && BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString().equals("immersive_aircraft:biplane")) {
            jem$repairHealth = vehicle.getHealth();
            if (Float.isFinite(jem$repairHealth) && jem$repairHealth > 0 && jem$repairHealth < 1) jem$repairActor = actor;
        }
    }

    @Inject(method = "m_6096_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"), remap = false)
    private void jem$afterRepair(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var vehicle = (VehicleEntity) (Object) this;
        ServerPlayer actor = jem$repairActor;
        jem$repairActor = null;
        if (actor == player && callback.getReturnValue() == InteractionResult.CONSUME && vehicle.onGround()
                && !vehicle.isRemoved() && Float.isFinite(vehicle.getHealth()) && vehicle.getHealth() > jem$repairHealth) {
            TravelProof.emit(actor, "v3_world_air_service");
        }
    }
}
