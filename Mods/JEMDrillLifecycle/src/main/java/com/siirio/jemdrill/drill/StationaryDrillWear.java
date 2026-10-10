package com.siirio.jemdrill.drill;


import static com.siirio.jemdrill.drill.DrillWearPolicy.*;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class StationaryDrillWear {

    private StationaryDrillWear() {
    }

    public static void recordBreak(DrillBlockEntity drill, BlockState state) {
        Level level = drill.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        DrillWearAccess wear = (DrillWearAccess) drill;
        wear.jemcompat$setWear(DrillWearPolicy.afterBreak(wear.jemcompat$getWear(), state));
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

    private static void destroy(DrillBlockEntity drill) {
        Level level = drill.getLevel();
        if (level == null) {
            return;
        }
        level.playSound(null, drill.getBlockPos(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(drill.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
    }

}
