package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.resident.AmenitySnapshot;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.town.SettlementSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Recomputes only the exact homes and workplaces affected by a block event. */
public final class AmenityScanService {
    public static final int WIDTH = 17, HEIGHT = 11;
    private AmenityScanService() { }

    public static void invalidate(MinecraftServer server, ResourceKey<Level> dimension, BlockPos changed) {
        CityComfortService.invalidate(server, dimension, changed);
        SettlementSavedData data = SettlementSavedData.get(server);
        boolean updated = false;
        for (var town : data.towns()) {
            for (ResidentMood resident : data.residents(town.id())) {
                if (near(resident.home().orElse(null), dimension, changed)) {
                    evaluate(server, resident, resident.home().orElseThrow(), true);
                    updated = true;
                }
                if (near(resident.workplace().orElse(null), dimension, changed)) {
                    evaluate(server, resident, resident.workplace().orElseThrow(), false);
                    updated = true;
                }
            }
        }
        if (updated) data.changed();
    }

    private static boolean near(GlobalPos site, ResourceKey<Level> dimension, BlockPos changed) {
        if (site == null || !site.dimension().equals(dimension)) return false;
        BlockPos pos = site.pos();
        return Math.abs(pos.getX() - changed.getX()) <= WIDTH / 2
                && Math.abs(pos.getY() - changed.getY()) <= HEIGHT / 2
                && Math.abs(pos.getZ() - changed.getZ()) <= WIDTH / 2;
    }

    private static void evaluate(MinecraftServer server, ResidentMood resident, GlobalPos site, boolean home) {
        ServerLevel level = server.getLevel(site.dimension());
        if (level == null) return;
        AmenityEvidence evidence = new AmenityEvidence();
        boolean stale = false;
        for (int y = -HEIGHT / 2; y <= HEIGHT / 2; y++) {
            for (int z = -WIDTH / 2; z <= WIDTH / 2; z++) {
                for (int x = -WIDTH / 2; x <= WIDTH / 2; x++) {
                    BlockPos pos = site.pos().offset(x, y, z);
                    if (!level.hasChunkAt(pos)) {
                        stale = true;
                        continue;
                    }
                    evidence.observe(level, pos, level.getBlockState(pos));
                }
            }
        }
        AmenitySnapshot snapshot = new AmenitySnapshot(evidence.points(home ? 12 : 8),
                evidence.categories(), level.getGameTime(), stale);
        if (home) resident.homeAmenities(snapshot);
        else resident.workAmenities(snapshot);
    }

}
