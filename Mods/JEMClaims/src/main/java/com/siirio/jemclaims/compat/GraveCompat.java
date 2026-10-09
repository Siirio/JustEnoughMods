package com.siirio.jemclaims.compat;

import de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class GraveCompat {
    private GraveCompat() {}

    public static boolean isOwner(ServerPlayer player, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GraveStoneTileEntity grave
                && grave.getDeath() != null
                && player.getUUID().equals(grave.getDeath().getPlayerUUID());
    }
}
