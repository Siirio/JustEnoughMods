package com.siirio.jemsettlements.payroll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** A tax intent must reach disk before money moves, and money before its acknowledgement. */
public final class TaxDurability {
    private TaxDurability() { }
    public static void save(MinecraftServer server, SavedData data, String name) {
        if (!name.equals("jem_settlements") && !name.equals("jem_economy")) throw new IllegalArgumentException("Unknown tax storage owner");
        var directory = server.getWorldPath(LevelResource.ROOT).resolve("data");
        var target = directory.resolve(name + ".dat");
        var temporary = directory.resolve(name + ".tax.tmp");
        CompoundTag root = new CompoundTag(); root.put("data", data.save(new CompoundTag())); NbtUtils.addCurrentDataVersion(root);
        try {
            Files.createDirectories(directory); NbtIo.writeCompressed(root, temporary.toFile());
            try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            data.setDirty(false);
        } catch (IOException failure) {
            data.setDirty(); throw new IllegalStateException("Daily tax transaction remains pending: " + name, failure);
        }
    }
}
