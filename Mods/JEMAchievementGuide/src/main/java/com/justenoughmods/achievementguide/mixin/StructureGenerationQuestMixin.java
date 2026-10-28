package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StructureLootOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureStart.class)
public abstract class StructureGenerationQuestMixin {
    @Redirect(method = "placeInChunk", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;postProcess(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/BlockPos;)V"))
    private void jem$pieceOrigin(StructurePiece piece, WorldGenLevel world, StructureManager structures, ChunkGenerator generator,
                                 RandomSource random, BoundingBox bounds, ChunkPos chunk, BlockPos pivot) {
        StructureLootOrigin.place((StructureStart) (Object) this, world,
                () -> piece.postProcess(world, structures, generator, random, bounds, chunk, pivot));
    }

    @Redirect(method = "placeInChunk", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/structure/Structure;afterPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;)V"))
    private void jem$afterPlaceOrigin(Structure structure, WorldGenLevel world, StructureManager structures, ChunkGenerator generator,
                                      RandomSource random, BoundingBox bounds, ChunkPos chunk, PiecesContainer pieces) {
        StructureLootOrigin.place((StructureStart) (Object) this, world,
                () -> structure.afterPlace(world, structures, generator, random, bounds, chunk, pieces));
    }
}
