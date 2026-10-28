package com.justenoughmods.achievementguide.integration;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Persisted observation of the actual native structure generation call, never backfilled from location. */
public final class StructureLootOrigin {
    public static final String KEY = "JemQuestGeneratedLootOriginV1";
    private static final ThreadLocal<Origin> CURRENT = new ThreadLocal<>();
    private record Origin(WorldGenLevel world, StructureStart start, String structure) {}
    private StructureLootOrigin() {}

    public static void place(StructureStart start, WorldGenLevel world, Runnable nativePlacement) {
        var id = world.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        Origin previous = CURRENT.get();
        if (!(world instanceof WorldGenRegion) || id == null || StructureLootProof.actionFor(id.toString()) == null) CURRENT.remove();
        else CURRENT.set(new Origin(world, start, id.toString()));
        try { nativePlacement.run(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }

    public static void generated(RandomizableContainerBlockEntity container) {
        Origin origin = CURRENT.get();
        if (origin == null || !origin.start.isValid() || !origin.start.getBoundingBox().isInside(container.getBlockPos())) return;
        CompoundTag proof = new CompoundTag();
        proof.putString("dimension", origin.world.getLevel().dimension().location().toString());
        proof.putString("structure", origin.structure);
        proof.putLong("start_chunk", origin.start.getChunkPos().toLong());
        proof.putLong("position", container.getBlockPos().asLong());
        container.getPersistentData().put(KEY, proof);
    }
}
