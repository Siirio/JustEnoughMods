package com.siirio.jemserver.smp;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Native player-file IO only; receipt schemas remain with their delivery owners. */
final class NativePlayerFiles {
    private NativePlayerFiles() {}

    static CompoundTag read(MinecraftServer server, UUID player) throws IOException {
        var path = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player + ".dat");
        return Files.exists(path) ? NbtIo.readCompressed(path.toFile()) : new CompoundTag();
    }

    static CompoundTag saveAndVerifyInventory(ServerPlayer player, boolean saveIntegratedWorld) throws IOException {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        ((com.siirio.jemserver.mixin.PlayerSaveAccess) player.server.getPlayerList()).jem$save(player);
        if (saveIntegratedWorld && player.server.isSingleplayer()) player.server.saveEverything(false, true, true);
        var path = player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID() + ".dat");
        if (!Files.exists(path)) throw new IOException("Native player file was not persisted");
        CompoundTag saved = read(player.server, player.getUUID());
        if (!expected.getList("Inventory", Tag.TAG_COMPOUND).equals(saved.getList("Inventory", Tag.TAG_COMPOUND)))
            throw new IOException("Native player inventory was not persisted");
        return saved;
    }
}
