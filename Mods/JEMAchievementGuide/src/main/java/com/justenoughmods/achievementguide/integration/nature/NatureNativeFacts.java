package com.justenoughmods.achievementguide.integration.nature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.hibiscus.naturespirit.config.NSConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Exact installed native farm predicate, including its native bounded coral-column traversal. */
public final class NatureNativeFacts {
    private static Method columnEnd;
    private NatureNativeFacts() {}
    public static boolean deepslateEnabled() { return NSConfig.deepslateGenerator; }
    public static boolean block(BlockState state, String path) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("natures_spirit:" + path);
    }
    public static boolean validCalcite(ServerLevel level, BlockPos pos, BlockState state) {
        if (!NSConfig.calciteGenerator || !block(state, "calcite_cluster") || !state.hasProperty(AmethystClusterBlock.FACING)) return false;
        BlockPos coral = pos.relative(state.getValue(AmethystClusterBlock.FACING).getOpposite());
        if (level.getChunkSource().getChunkNow(coral.getX() >> 4, coral.getZ() >> 4) == null
                || !(level.getBlockState(coral).getBlock() instanceof CoralBlock)) return false;
        var origin = NatureChunkOrigins.get(level, pos, "Calcite");
        if (!origin.contains("coral", 4) || origin.getLong("coral") != coral.asLong()) return false;
        try {
            if (columnEnd == null) {
                // NatureSpirit adds this exact private native owner method to CoralBlock at Mixin application.
                columnEnd = CoralBlock.class.getDeclaredMethod("naturespirit$findColumnEnd", BlockGetter.class, BlockPos.class);
                columnEnd.setAccessible(true);
            }
            return ((Optional<?>) columnEnd.invoke(null, level, coral)).isPresent();
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("Native calcite column owner failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native calcite column owner changed", failure);
        }
    }
}
