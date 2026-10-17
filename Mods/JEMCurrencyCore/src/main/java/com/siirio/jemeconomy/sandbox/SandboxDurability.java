package com.siirio.jemeconomy.sandbox;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Exact native SavedData encoding, one owner file at a time, with a durable atomic replacement.
 * Unlike SavedData.save(File), an I/O failure aborts the transaction instead of being logged away. */
public final class SandboxDurability {
    private SandboxDurability() {}
    public static void save(MinecraftServer server,SavedData data,String name) {
        if(!SandboxCommerce.active(server))throw new IllegalStateException("Sandbox financial durability required");
        if(!name.equals("puffish_skills")&&!name.matches("jem_[a-z_]+"))throw new IllegalArgumentException("Invalid saved-data owner");
        if(!data.isDirty())return;
        var directory=server.getWorldPath(LevelResource.ROOT).resolve("data");var target=directory.resolve(name+".dat");
        var temporary=directory.resolve(name+".transaction.tmp");
        CompoundTag root=new CompoundTag();root.put("data",data.save(new CompoundTag()));NbtUtils.addCurrentDataVersion(root);
        try{
            Files.createDirectories(directory);NbtIo.writeCompressed(root,temporary.toFile());
            try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            data.setDirty(false);
        }catch(IOException failure){throw new IllegalStateException("Cannot commit "+name+"; transaction remains pending",failure);}
    }
}
