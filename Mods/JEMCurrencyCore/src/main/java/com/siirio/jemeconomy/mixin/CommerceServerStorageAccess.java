package com.siirio.jemeconomy.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives the journal Minecraft's existing level.dat writer; never saves world chunks. */
@Mixin(MinecraftServer.class)
public interface CommerceServerStorageAccess {
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess jem$commerceStorage();
}
