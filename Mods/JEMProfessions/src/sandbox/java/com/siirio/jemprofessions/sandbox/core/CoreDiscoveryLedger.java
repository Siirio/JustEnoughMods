package com.siirio.jemprofessions.sandbox.core;

import com.mojang.logging.LogUtils;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;

/** Persisted 32x32 chunk bitmaps with a bounded read cache. Eviction never forgets visits. */
final class CoreDiscoveryLedger {
    private static final Map<Path, byte[]> CACHE = new LinkedHashMap<>(64, 0.75F, true);
    private CoreDiscoveryLedger() { }
    static boolean firstVisit(ServerPlayer player, int x, int z) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return false;
        String dimension = player.level().dimension().location().toString();
        String dimensionKey = java.util.UUID.nameUUIDFromBytes(dimension.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        Path file = player.getServer().getWorldPath(LevelResource.ROOT).resolve("data/jem_professions_exploration")
                .resolve(principal.get().accountId().toString()).resolve(dimensionKey).resolve((x >> 5) + "_" + (z >> 5) + ".bin");
        return bit(file, 128, (x & 31) + ((z & 31) << 5), true);
    }
    static boolean firstPlacement(ServerPlayer player, BlockPos pos) {
        var principal = IdentityPolicy.resolve(player); if (principal.isEmpty()) return false;
        return bit(section(player.serverLevel(), "architect/" + principal.get().accountId(), pos), 512, index(pos), true);
    }
    static void placed(ServerLevel level, BlockPos pos) { bit(section(level, "world", pos), 512, index(pos), true); }
    static boolean natural(ServerLevel level, BlockPos pos) { return bit(section(level, "world", pos), 512, index(pos), false); }
    private static int index(BlockPos pos) { return (pos.getX() & 15) | ((pos.getZ() & 15) << 4) | ((pos.getY() & 15) << 8); }
    private static Path section(ServerLevel level, String owner, BlockPos pos) {
        String dimension = java.util.UUID.nameUUIDFromBytes(level.dimension().location().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("data/jem_professions_provenance").resolve(owner).resolve(dimension)
                .resolve((pos.getX() >> 4) + "_" + (pos.getY() >> 4) + "_" + (pos.getZ() >> 4) + ".bin");
    }
    /** Returns whether the bit was previously clear; read failures deny both XP and natural provenance. */
    private static boolean bit(Path file, int length, int index, boolean write) {
        try {
            byte[] bits = CACHE.get(file);
            if (bits == null) {
                bits = Files.exists(file) ? Files.readAllBytes(file) : new byte[length];
                if (bits.length != length) throw new IOException("Invalid profession provenance bitmap: " + file);
                CACHE.put(file, bits); while (CACHE.size() > 64) CACHE.remove(CACHE.keySet().iterator().next());
            }
            if ((bits[index >> 3] & (1 << (index & 7))) != 0) return false;
            if (!write) return true;
            byte[] updated = bits.clone(); updated[index >> 3] |= (byte) (1 << (index & 7));
            Files.createDirectories(file.getParent()); Path pending = file.resolveSibling(file.getFileName() + ".pending");
            Files.write(pending, updated);
            try { Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING); }
            CACHE.put(file, updated); while (CACHE.size() > 64) CACHE.remove(CACHE.keySet().iterator().next());
            return true;
        } catch (IOException error) {
            LogUtils.getLogger().error("Cannot persist/read profession provenance; eligibility denied", error); return false;
        }
    }
    static void clear() { CACHE.clear(); }
}
