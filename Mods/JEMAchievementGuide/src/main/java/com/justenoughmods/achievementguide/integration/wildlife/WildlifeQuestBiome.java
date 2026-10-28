package com.justenoughmods.achievementguide.integration.wildlife;
import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
public final class WildlifeQuestBiome {
    private WildlifeQuestBiome() {}
    public static List<Holder<Biome>> snapshot(EntityMungus entity) {
        if (entity.level().isClientSide) return List.of(); var chunk = entity.level().getChunkAt(entity.blockPosition());
        int index = chunk.getSectionIndex(entity.getBlockY()); if (index < 0 || index >= chunk.getSectionsCount()) return List.of();
        var values = chunk.getSection(index).getBiomes(); List<Holder<Biome>> result = new ArrayList<>(64);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) result.add(values.get(x, y, z)); return result;
    }
}
