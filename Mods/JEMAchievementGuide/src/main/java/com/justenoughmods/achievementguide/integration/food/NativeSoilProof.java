package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Scope around the original native water predicate, never a second sprinkler range scan. */
public final class NativeSoilProof {
    public static final String PLACER = "JemQuestSprinklerPlacerV4";
    public static final class Water {
        final LevelReader reader;
        final BlockPos farmland;
        boolean nativeRange;
        BlockPos selected;
        BlockState state;
        public Water(LevelReader reader, BlockPos farmland) { this.reader = reader; this.farmland = farmland.immutable(); }
    }
    private static final ThreadLocal<Water> PREDICATE = new ThreadLocal<>();
    private NativeSoilProof() {}
    public static void begin(LevelReader reader, BlockPos pos) { PREDICATE.set(new Water(reader, pos)); }
    public static void nativeRange() { Water run = PREDICATE.get(); if (run != null) run.nativeRange = true; }
    public static void selected(Level reader, BlockPos pos, BlockState state) {
        Water run = PREDICATE.get();
        if (run != null && run.reader == reader && run.nativeRange) {
            run.selected = pos.immutable();
            run.state = state;
        }
    }
    public static UUID end(boolean accepted) {
        Water run = PREDICATE.get();
        PREDICATE.remove();
        if (!accepted || run == null || !run.nativeRange || run.selected == null || !(run.reader instanceof ServerLevel level)
                || !String.valueOf(ForgeRegistries.BLOCKS.getKey(run.state.getBlock())).equals("farm_and_charm:water_sprinkler")
                || !level.hasChunkAt(run.selected)) return null;
        BlockEntity sprinkler = level.getBlockEntity(run.selected);
        return sprinkler != null && sprinkler.getBlockState().is(run.state.getBlock()) && sprinkler.getPersistentData().hasUUID(PLACER)
                ? sprinkler.getPersistentData().getUUID(PLACER) : null;
    }
    public static void hydrated(ServerLevel level, BlockPos pos, BlockState before, UUID placer) {
        if (placer == null || !before.hasProperty(FarmBlock.MOISTURE)) return;
        BlockState after = level.getBlockState(pos);
        if (!after.is(before.getBlock()) || !after.hasProperty(FarmBlock.MOISTURE)
                || after.getValue(FarmBlock.MOISTURE) <= before.getValue(FarmBlock.MOISTURE)) return;
        var actor = level.getServer().getPlayerList().getPlayer(placer);
        if (RemainingFoodActors.real(actor) != null && actor.level() == level) QuestActionTrigger.INSTANCE.emit(actor, "food_living_soil_watered");
    }
}
