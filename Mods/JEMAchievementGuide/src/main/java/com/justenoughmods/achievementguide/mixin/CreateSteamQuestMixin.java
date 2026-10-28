package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.justenoughmods.achievementguide.integration.create.CreateSteamPlacement;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity", remap = false)
public abstract class CreateSteamQuestMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$transmittedPower(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        CompoundTag run = owner.getPersistentData().getCompound(CreateSteamPlacement.RUN);
        if (!run.hasUUID("actor")) return;
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity)(Object)this;
        var shaft = engine.getShaft();
        var tank = engine.getTank();
        boolean powered = engine.isValid() && shaft != null && tank != null
                && tank.boiler.waterSupply > 0 && tank.boiler.getEngineEfficiency(tank.getTotalTankSize()) > 0
                && shaft.isPoweredBy(owner.getBlockPos()) && shaft.engineEfficiency > 0
                && shaft.getGeneratedSpeed() != 0 && shaft.getSpeed() != 0
                && (!run.getBoolean("require_direction_change") || shaft.movementDirection != run.getInt("prior_direction"));
        if (powered) CreateRunProofs.emit(level, run.getUUID("actor"), "create_steam_power");
        if (powered || level.getGameTime() > run.getLong("deadline")) {
            owner.getPersistentData().remove(CreateSteamPlacement.RUN);
            owner.setChanged();
        }
    }
}
