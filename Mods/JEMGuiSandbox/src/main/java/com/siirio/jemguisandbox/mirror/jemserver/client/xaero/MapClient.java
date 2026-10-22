package com.siirio.jemguisandbox.mirror.jemserver.client.xaero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
public final class MapClient {
    public static boolean open(ResourceLocation dimension, BlockPos position) {
        com.siirio.jemguisandbox.Demo.mapPreview(dimension,position); return true;
    }
}
