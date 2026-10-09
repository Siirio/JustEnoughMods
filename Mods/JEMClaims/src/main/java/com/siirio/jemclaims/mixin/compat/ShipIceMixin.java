package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.talhanation.smallships.world.entity.ship.abilities.IceBreakable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.talhanation.smallships.world.entity.ship.Ship", remap = false)
public abstract class ShipIceMixin {
    @Unique private static final int JEM_ICE_INTERVAL = 15;
    @Unique private static final double JEM_ICE_EXPANSION = 1.5;
    @Unique private static final double JEM_ICE_MARGIN = 0.75;

    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lcom/talhanation/smallships/world/entity/ship/abilities/IceBreakable;tickIceBreakable()V"))
    private void jemClaims$ice(IceBreakable ability) {
        Entity ship = (Entity) (Object) this;
        if (ship.level() instanceof ServerLevel level && ship.tickCount % JEM_ICE_INTERVAL == 0) {
            AABB bounds = ship.getBoundingBox().inflate(JEM_ICE_EXPANSION);
            BlockPos min = new BlockPos((int) (bounds.minX - JEM_ICE_MARGIN), (int) (bounds.minY - JEM_ICE_MARGIN), (int) (bounds.minZ - JEM_ICE_MARGIN));
            BlockPos max = new BlockPos((int) (bounds.maxX + JEM_ICE_MARGIN), (int) (bounds.maxY + JEM_ICE_MARGIN), (int) (bounds.maxZ + JEM_ICE_MARGIN));
            for (BlockPos target : BlockPos.betweenClosed(min, max)) {
                if (!(level.getBlockState(target).getBlock() instanceof IceBlock)) continue;
                boolean allowed = ship.getControllingPassenger() instanceof ServerPlayer player
                        ? FlanBridge.can(player, level, target, ClaimPermission.BREAK)
                        : FlanBridge.at(level, target) == null;
                if (!allowed) return;
            }
        }
        ability.tickIceBreakable();
    }
}
