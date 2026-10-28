package com.justenoughmods.achievementguide.integration.photo;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Native printed/captured output waits for the original repository's accepted image. */
public final class PhotoImages {
    private record Key(MinecraftServer server, String id, UUID actor, String action) {}
    private record Pending(ExposureType type, UUID uploader, boolean printed, long retainedUntil) {}
    private static final Map<Key, Pending> PENDING = new ConcurrentHashMap<>();

    public static boolean valid(String id, ExposureType type, boolean printed) {
        if (id == null || id.isBlank() || id.length() > 512) return false;
        var data = ExposureServer.exposureRepository().load(id).getData();
        return data.isPresent() && data.get().getWidth() > 0 && data.get().getHeight() > 0
                && (long)data.get().getWidth() * data.get().getHeight() == data.get().getPixels().length
                && data.get().getTag().type() == type && (!printed || data.get().getTag().wasPrinted());
    }

    public static void output(ServerPlayer actor, String id, ExposureType type, UUID uploader, boolean printed, String action) {
        if (HomeInteractionProof.actor(actor) == null || id == null || id.isBlank() || id.length() > 512) return;
        if (uploader == null && valid(id, type, printed)) {
            QuestActionTrigger.INSTANCE.emit(actor, action);
            return;
        }
        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> entry.getValue().retainedUntil < now);
        if (PENDING.size() < 2048) PENDING.put(new Key(actor.server, id, actor.getUUID(), action),
                new Pending(type, uploader, printed, now + 120_000L));
    }

    public static void received(ServerPlayer uploader, String id) {
        MinecraftServer server = uploader.server;
        Runnable accept = () -> {
            long now = System.currentTimeMillis();
            for (var entry : PENDING.entrySet()) {
                Key key = entry.getKey();
                Pending pending = entry.getValue();
                if (pending.retainedUntil < now) { PENDING.remove(key, pending); continue; }
                if (key.server != server || !key.id.equals(id) || pending.uploader != null && !pending.uploader.equals(uploader.getUUID())) continue;
                if (!valid(id, pending.type, pending.printed)) continue;
                ServerPlayer actor = server.getPlayerList().getPlayer(key.actor);
                if (HomeInteractionProof.actor(actor) != null && PENDING.remove(key, pending)) QuestActionTrigger.INSTANCE.emit(actor, key.action);
            }
        };
        if (server.isSameThread()) accept.run(); else server.execute(accept);
    }
    public static void clear(MinecraftServer server) { PENDING.keySet().removeIf(key -> key.server == server); }
    public static void logout(UUID actor) { PENDING.keySet().removeIf(key -> key.actor.equals(actor)); }
    private PhotoImages() {}
}
