package com.siirio.jemcompat.feature.create;

import com.siirio.jemcompat.advancement.AdvancementAwards;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class StationaryDrillWear {
    public static final String WEAR_KEY = "JemDrillWear";
    public static final String REPAIRS_KEY = "JemDrillRepairs";
    public static final int MAX_WEAR = 1000;
    public static final int DULL_WEAR = 500;
    public static final int MAX_REPAIRS = 3;
    public static final float DULL_SPEED_MULTIPLIER = 0.5F;
    private static final int NORMAL_WEAR = 1;
    private static final int OBSIDIAN_WEAR = 500;
    private static final ResourceLocation DRILL_SERVICE = advancement("drill_service");

    private StationaryDrillWear() {
    }

    public static void recordBreak(DrillBlockEntity drill, BlockState state) {
        Level level = drill.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        DrillWearAccess wear = (DrillWearAccess) drill;
        int amount = state.is(Blocks.OBSIDIAN) ? OBSIDIAN_WEAR : NORMAL_WEAR;
        wear.jemcompat$setWear(Math.min(MAX_WEAR, wear.jemcompat$getWear() + amount));
        drill.setChanged();
        if (wear.jemcompat$getWear() >= MAX_WEAR) {
            destroy(drill);
        }
    }

    public static boolean destroyForBedrock(DrillBlockEntity drill, BlockState state) {
        Level level = drill.getLevel();
        if (level == null || level.isClientSide || !state.is(Blocks.BEDROCK)) {
            return false;
        }
        destroy(drill);
        return true;
    }

    public static void awardService(ServerPlayer player) {
        AdvancementAwards.award(player, DRILL_SERVICE);
    }

    private static void destroy(DrillBlockEntity drill) {
        Level level = drill.getLevel();
        if (level == null) {
            return;
        }
        level.playSound(null, drill.getBlockPos(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(drill.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static ResourceLocation advancement(String path) {
        return new ResourceLocation("jem", "engineering/" + path);
    }
}
