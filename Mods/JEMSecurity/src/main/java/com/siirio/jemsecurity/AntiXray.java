package com.siirio.jemsecurity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class AntiXray {
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));

    private AntiXray() { }

    public static boolean writeFilteredChunk(FriendlyByteBuf buffer, LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ServerLevel level) || !SecurityRuntime.active(level.getServer())
                || !SecurityConfig.ANTI_XRAY.get()) return false;
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection original = sections[index];
            if (!original.maybeHas(AntiXray::hiddenBlock)) {
                original.write(buffer);
                continue;
            }
            LevelChunkSection filtered = new LevelChunkSection(original.getStates().copy(), original.getBiomes());
            int baseY = SectionPos.sectionToBlockCoord(chunk.getMinSection() + index);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = original.getBlockState(x, y, z);
                        if (!hiddenBlock(state)) continue;
                        BlockPos worldPos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y,
                                chunk.getPos().getMinBlockZ() + z);
                        if (!exposed(level, worldPos)) filtered.setBlockState(x, y, z, filler(level, worldPos, state), false);
                    }
                }
            }
            filtered.recalcBlockCounts();
            filtered.write(buffer);
        }
        return true;
    }

    public static boolean hiddenBlock(BlockState state) {
        return state.is(ORES) || state.is(Blocks.ANCIENT_DEBRIS);
    }

    public static boolean exposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!level.hasChunkAt(adjacent)) continue;
            BlockState neighbor = level.getBlockState(adjacent);
            if (neighbor.isAir() || !neighbor.getFluidState().isEmpty() || !neighbor.isSolidRender(level, adjacent)) return true;
        }
        return false;
    }

    public static void revealAdjacent(ServerLevel level, BlockPos changed) {
        if (!SecurityRuntime.active(level.getServer()) || !SecurityConfig.ANTI_XRAY.get()) return;
        for (Direction direction : Direction.values()) {
            BlockPos target = changed.relative(direction);
            if (!level.hasChunkAt(target)) continue;
            BlockState state = level.getBlockState(target);
            if (hiddenBlock(state) && exposed(level, target)) level.sendBlockUpdated(target, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState filler(ServerLevel level, BlockPos pos, BlockState ore) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!level.hasChunkAt(adjacent)) continue;
            BlockState candidate = level.getBlockState(adjacent);
            if (!hiddenBlock(candidate) && candidate.isSolidRender(level, adjacent) && !candidate.hasBlockEntity()) return candidate;
        }
        if (level.dimension() == Level.NETHER || ore.is(Blocks.NETHER_GOLD_ORE)
                || ore.is(Blocks.NETHER_QUARTZ_ORE) || ore.is(Blocks.ANCIENT_DEBRIS)) return Blocks.NETHERRACK.defaultBlockState();
        if (level.dimension() == Level.END) return Blocks.END_STONE.defaultBlockState();
        return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
}
