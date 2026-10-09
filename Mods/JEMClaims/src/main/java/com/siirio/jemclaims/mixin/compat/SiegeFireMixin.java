package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.talhanation.siegeweapons.entities.projectile.AbstractCatapultProjectile", remap = false)
public abstract class SiegeFireMixin {
    @Redirect(method = "igniteArea", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = true), require = 2)
    private boolean jemClaims$ignite(Level level, BlockPos pos, BlockState state, int flags) {
        return (!(level instanceof ServerLevel serverLevel)
                || FlanBridge.environmental(serverLevel, pos, ClaimPermission.FIRESPREAD)) && level.setBlock(pos, state, flags);
    }

    @Redirect(method = "igniteArea", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", remap = true))
    private boolean jemClaims$spread(ServerLevel level, BlockPos pos, BlockState state) {
        return FlanBridge.environmental(level, pos, ClaimPermission.FIRESPREAD) && level.setBlockAndUpdate(pos, state);
    }
}
