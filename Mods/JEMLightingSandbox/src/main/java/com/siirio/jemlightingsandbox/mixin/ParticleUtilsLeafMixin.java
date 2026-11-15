package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.CanopyLeaves;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleUtils.class)
public abstract class ParticleUtilsLeafMixin {
    @Unique
    private static final Set<ResourceLocation> JEM_LIGHTING_BACKPORT_LEAVES = Set.of(
            new ResourceLocation("minecraft", "pale_oak_leaves"),
            new ResourceLocation("minecraft", "tinted_leaves"),
            new ResourceLocation("minecraft", "tinted_needles"));

    @Inject(method = "spawnParticleBelow(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/particles/ParticleOptions;)V",
            at = @At("HEAD"), cancellable = true)
    private static void jemLightingSandbox$skipReplacedLeaf(Level level, BlockPos position, RandomSource random,
                                                           ParticleOptions particle, CallbackInfo callback) {
        if (!level.isClientSide()) {
            return;
        }

        var type = particle.getType();
        var id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        if ((type == ParticleTypes.CHERRY_LEAVES || id != null && JEM_LIGHTING_BACKPORT_LEAVES.contains(id))
                && CanopyLeaves.replacesFallingLeaf(level, position, level.getBlockState(position))) {
            callback.cancel();
        }
    }
}
