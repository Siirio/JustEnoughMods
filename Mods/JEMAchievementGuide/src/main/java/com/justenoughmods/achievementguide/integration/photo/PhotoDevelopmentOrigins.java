package com.justenoughmods.achievementguide.integration.photo;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Native frame IDs after an actual film recipe debit and actual developed-roll receipt. */
public final class PhotoDevelopmentOrigins extends SavedData {
    private static final int LIMIT = 16384;
    private final Set<String> origins = new LinkedHashSet<>();

    public static PhotoDevelopmentOrigins get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PhotoDevelopmentOrigins::load,
                PhotoDevelopmentOrigins::new, "jem_photo_development_origins_v4");
    }
    private static String key(UUID actor, Frame frame) {
        return frame.identifier().getId().filter(id -> !id.isBlank() && id.length() <= 512)
                .map(id -> actor + "/" + frame.type().name() + "/" + id).orElse("");
    }
    public void developed(UUID actor, Frame frame) {
        String key = key(actor, frame);
        if (!key.isEmpty() && origins.size() < LIMIT && origins.add(key)) setDirty();
    }
    public boolean owns(UUID actor, Frame frame) { return origins.contains(key(actor, frame)); }

    private static PhotoDevelopmentOrigins load(CompoundTag tag) {
        PhotoDevelopmentOrigins data = new PhotoDevelopmentOrigins();
        ListTag entries = tag.getList("frames", 8);
        for (int index = 0; index < Math.min(LIMIT, entries.size()); index++) {
            String key = entries.getString(index);
            if (key.length() <= 600) data.origins.add(key);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (String key : origins) entries.add(StringTag.valueOf(key));
        tag.put("frames", entries);
        return tag;
    }
}
