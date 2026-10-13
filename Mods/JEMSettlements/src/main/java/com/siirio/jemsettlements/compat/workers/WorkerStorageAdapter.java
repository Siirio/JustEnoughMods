package com.siirio.jemsettlements.compat.workers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public final class WorkerStorageAdapter {
    private WorkerStorageAdapter() {
    }

    public static boolean isPhysicalInventory(ServerLevel level, BlockPos position) {
        var blockEntity = level.getBlockEntity(position);
        return blockEntity != null && blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }
}
