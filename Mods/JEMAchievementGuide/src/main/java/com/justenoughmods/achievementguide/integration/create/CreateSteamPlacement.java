package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Attribution only for a committed local engine/shaft connection to an already supplied boiler. */
public final class CreateSteamPlacement {
    public static final String RUN = "jemQuestSteamConnectionV1";
    private CreateSteamPlacement() {}

    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer actor) || actor instanceof FakePlayer
                || actor.isSpectator() || actor.getAbilities().instabuild || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos placed = event.getPos();
        String block = String.valueOf(ForgeRegistries.BLOCKS.getKey(level.getBlockState(placed).getBlock()));
        if (block.equals("create:steam_engine")) arm(level.getBlockEntity(placed), actor);
        else if (block.equals("create:shaft") || block.equals("create:powered_shaft")) {
            // The native engine-to-shaft offset is exactly two along its facing.
            for (Direction direction : Direction.values()) {
                BlockPos engine = placed.relative(direction, 2);
                BlockEntity owner = level.getBlockEntity(engine);
                if (owner instanceof SteamEngineBlockEntity
                        && SteamEngineBlock.getShaftPos(level.getBlockState(engine), engine).equals(placed)) arm(owner, actor);
            }
        }
    }

    public static void activated(BlockEntity owner, ServerPlayer actor) {
        arm(owner, actor);
        if (owner.getPersistentData().contains(RUN) && owner instanceof SteamEngineBlockEntity engine) {
            var shaft = engine.getShaft();
            owner.getPersistentData().getCompound(RUN).putBoolean("require_direction_change", true);
            owner.getPersistentData().getCompound(RUN).putInt("prior_direction", shaft == null ? 0 : shaft.movementDirection);
        }
    }

    private static void arm(BlockEntity owner, ServerPlayer actor) {
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || !(owner instanceof SteamEngineBlockEntity engine) || !(owner.getLevel() instanceof ServerLevel level)) return;
        var tank = engine.getTank();
        if (!engine.isValid() || tank == null || tank.boiler.waterSupply <= 0
                || tank.boiler.getEngineEfficiency(tank.getTotalTankSize()) <= 0) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("actor", actor.getUUID());
        run.putLong("deadline", level.getGameTime() + 20);
        owner.getPersistentData().put(RUN, run);
        owner.setChanged();
    }
}
