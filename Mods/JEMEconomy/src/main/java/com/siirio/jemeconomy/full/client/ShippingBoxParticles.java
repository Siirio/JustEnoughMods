package com.siirio.jemeconomy.full.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

/** Native shape sampling and oak material, with one particle per three native samples. */
public final class ShippingBoxParticles implements IClientBlockExtensions {
    @Override public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine engine) {
        if (!(level instanceof ClientLevel client)) return true;
        int[] sample = {0};
        state.getShape(level, pos).forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double dx = Math.min(1.0D, maxX - minX), dy = Math.min(1.0D, maxY - minY), dz = Math.min(1.0D, maxZ - minZ);
            int nx = Math.max(2, Mth.ceil(dx / .25D)), ny = Math.max(2, Mth.ceil(dy / .25D)), nz = Math.max(2, Mth.ceil(dz / .25D));
            for (int x = 0; x < nx; x++) for (int y = 0; y < ny; y++) for (int z = 0; z < nz; z++) {
                if (++sample[0] % 3 != 0) continue;
                double fx = (x + .5D) / nx, fy = (y + .5D) / ny, fz = (z + .5D) / nz;
                engine.add(new TerrainParticle(client, pos.getX() + minX + fx * dx, pos.getY() + minY + fy * dy,
                        pos.getZ() + minZ + fz * dz, fx - .5D, fy - .5D, fz - .5D, Blocks.OAK_PLANKS.defaultBlockState(), pos));
            }
        });
        return true;
    }
}
