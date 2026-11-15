package net.shiroha233.roadweaver.features.path.pathlogic.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Gives an Abridged bridge priority over RoadWeaver's directly placed bridge template. */
public final class AbridgedBridgePriority {
    private static final ResourceLocation ABRIDGED_BRIDGE = new ResourceLocation("abridged", "bridge");
    private static final int CLEARANCE = 20;

    private AbridgedBridgePriority() { }

    public static boolean shouldSuppress(WorldGenLevel world, BlockPos start, BlockPos end) {
        ServerLevel level = world.m_6018_();
        StructureManager manager = level.m_215010_();
        Structure abridged = abridged(manager);
        if (abridged == null) return false;
        int minX = Math.min(start.m_123341_(), end.m_123341_()) - CLEARANCE;
        int maxX = Math.max(start.m_123341_(), end.m_123341_()) + CLEARANCE;
        int minZ = Math.min(start.m_123343_(), end.m_123343_()) - CLEARANCE;
        int maxZ = Math.max(start.m_123343_(), end.m_123343_()) + CLEARANCE;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                for (StructureStart candidate : manager.m_220477_(new ChunkPos(chunkX, chunkZ), value -> value == abridged)) {
                    if (!candidate.m_73603_()) continue;
                    BoundingBox box = candidate.m_73601_();
                    if (box.m_71019_(minX, minZ, maxX, maxZ)) return true;
                }
            }
        }
        return false;
    }

    private static Structure abridged(StructureManager manager) {
        try {
            Object registry = manager.m_220521_().getClass()
                    .getMethod("m_175515_", net.minecraft.resources.ResourceKey.class)
                    .invoke(manager.m_220521_(), Registries.f_256944_);
            Object value = registry.getClass().getMethod("m_7745_", ResourceLocation.class)
                    .invoke(registry, ABRIDGED_BRIDGE);
            return value instanceof Structure structure ? structure : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
