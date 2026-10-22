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
        if (!name.equals("jem_settlements") && !name.equals("jem_sandbox_civic") && !name.equals("jem_settlements_emeralds")) throw new IllegalArgumentException("Unknown tax storage owner");
        var directory = server.getWorldPath(LevelResource.ROOT).resolve("data");
        var target = directory.resolve(name + ".dat");
        var temporary = directory.resolve(name + ".tax.tmp");
        CompoundTag root = new CompoundTag(); root.put("data", data.save(new CompoundTag())); NbtUtils.addCurrentDataVersion(root);
        try {
            Files.createDirectories(directory); NbtIo.writeCompressed(root, temporary.toFile());
            try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            if (net.minecraft.Util.getPlatform() == net.minecraft.Util.OS.WINDOWS) {
                // JDK atomic-move ignores REPLACE_EXISTING on Windows. Request one native
                // same-volume replacement, without Java's delete-then-move alternative.
                WindowsReplacement.replace(temporary, target);
            } else {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!root.equals(NbtIo.readCompressed(target.toFile()))) throw new IOException("Daily tax save mismatch: " + name);
            data.setDirty(false);
        } catch (IOException failure) {
            data.setDirty(); throw new IllegalStateException("Daily tax transaction remains pending: " + name, failure);
        }
    }
    private static final class WindowsReplacement {
        private static void replace(java.nio.file.Path source, java.nio.file.Path target) throws IOException {
            var windows = com.sun.jna.platform.win32.Kernel32.INSTANCE;
            int flags = com.sun.jna.platform.win32.WinBase.MOVEFILE_REPLACE_EXISTING
                    | com.sun.jna.platform.win32.WinBase.MOVEFILE_WRITE_THROUGH;
            if (!windows.MoveFileEx(source.toAbsolutePath().toString(), target.toAbsolutePath().toString(),
                    new com.sun.jna.platform.win32.WinDef.DWORD(flags)))
                throw new IOException("Native tax file replacement failed: " + windows.GetLastError());
        }
    }

}
