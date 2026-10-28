package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native seated passengers, retained continuously between two actual contact stops. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity", remap = false)
public abstract class CreateElevatorQuestMixin {
    @Shadow private boolean arrived;
    @Unique private final Map<UUID, BlockPos> jem$boardingStops = new HashMap<>();
    @Unique private UUID jem$elevatorEntity;

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$carriedTrip(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        var contraptionEntity = ((PulleyBlockEntity)(Object)this).getAttachedContraption();
        if (contraptionEntity == null || !(contraptionEntity.getContraption() instanceof ElevatorContraption elevator)) {
            jem$boardingStops.clear();
            jem$elevatorEntity = null;
            return;
        }
        Entity vehicle = (Entity)(Object)contraptionEntity;
        if (!vehicle.getUUID().equals(jem$elevatorEntity)) {
            jem$boardingStops.clear();
            jem$elevatorEntity = vehicle.getUUID();
        }
        var aboard = new HashSet<UUID>();
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof ServerPlayer player && player.getVehicle() == vehicle) aboard.add(player.getUUID());
        }
        jem$boardingStops.keySet().retainAll(aboard);
        if (!arrived) return;
        ElevatorColumn column = ElevatorColumn.get(level, elevator.getGlobalColumn());
        if (column == null) return;
        int contactY = (int)Math.floor(vehicle.getY() + .5D) + elevator.getContactYOffset();
        BlockPos stop = column.contactAt(contactY);
        if (!(level.getBlockEntity(stop) instanceof ElevatorContactBlockEntity)) return;
        for (UUID actor : aboard) {
            BlockPos previous = jem$boardingStops.get(actor);
            if (previous != null && !previous.equals(stop)) {
                CreateRunProofs.emit(level, actor, "create_elevator_trip");
            }
            if (jem$boardingStops.size() < 128 || previous != null) jem$boardingStops.put(actor, stop.immutable());
        }
    }
}
