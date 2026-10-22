package com.justenoughmods.achievementguide.compat.ftb;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Only distinct observed collection keys; counts/completion/claims remain native FTB data. */
final class BookCollections extends SavedData {
    private final CompoundTag seen;

    private BookCollections(CompoundTag seen) { this.seen = seen; }

    static BookCollections get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BookCollections::new,
                () -> new BookCollections(new CompoundTag()), "jem_quest_collections");
    }

    int observe(UUID team, long task, String key) {
        String owner = team + ":" + Long.toHexString(task);
        CompoundTag keys = seen.getCompound(owner);
        if (!keys.getBoolean(key)) {
            keys.putBoolean(key, true);
            seen.put(owner, keys);
            setDirty();
        }
        return keys.size();
    }

    @Override public CompoundTag save(CompoundTag target) { return seen.copy(); }
}
