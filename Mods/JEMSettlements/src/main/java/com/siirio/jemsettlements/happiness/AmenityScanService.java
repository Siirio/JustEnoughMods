package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.resident.AmenitySnapshot;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.town.SettlementSavedData;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;

public final class AmenityScanService {
    public static final int WIDTH = 17, HEIGHT = 11;
    private static final int TOTAL = WIDTH * WIDTH * HEIGHT;
    private static final ArrayDeque<Cursor> QUEUE = new ArrayDeque<>();
    private static final Set<String> QUEUED = new LinkedHashSet<>();
    private AmenityScanService() { }

    public static void invalidate(MinecraftServer server, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos changed) {
        CityComfortService.invalidate(server, dimension, changed);
        SettlementSavedData data = SettlementSavedData.get(server);
        for (var town : data.towns()) for (ResidentMood resident : data.residents(town.id())) {
            if (near(resident.home().orElse(null), dimension, changed) || near(resident.workplace().orElse(null), dimension, changed)) resident.markAmenitiesDirty(server.overworld().getGameTime());
        }
        data.changed();
    }
    private static boolean near(GlobalPos site, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos changed) {
        if (site == null || !site.dimension().equals(dimension)) return false; BlockPos pos = site.pos(); return Math.abs(pos.getX() - changed.getX()) <= 8 && Math.abs(pos.getY() - changed.getY()) <= 5 && Math.abs(pos.getZ() - changed.getZ()) <= 8;
    }
    public static void tick(MinecraftServer server) {
        SettlementSavedData data = SettlementSavedData.get(server); long now = server.overworld().getGameTime(); int admitted = 0;
        for (var town : data.towns()) for (ResidentMood resident : data.residents(town.id())) {
            if (admitted >= 8 || resident.amenityDirtySince() <= 0L || now - resident.amenityDirtySince() < 1_200L) continue;
            if (resident.home().isPresent()) admitted += queue(resident, resident.home().get(), true) ? 1 : 0;
            if (admitted < 8 && resident.workplace().isPresent()) admitted += queue(resident, resident.workplace().get(), false) ? 1 : 0;
        }
        int evaluations = Math.min(8, QUEUE.size());
        for (int i = 0; i < evaluations; i++) { Cursor cursor = QUEUE.pollFirst(); if (cursor == null) break; if (!cursor.step(server, 64)) QUEUE.addLast(cursor); else { QUEUED.remove(cursor.key()); data.changed(); } }
    }
    private static boolean queue(ResidentMood resident, GlobalPos site, boolean home) { Cursor cursor = new Cursor(resident, site, home); if (!QUEUED.add(cursor.key())) return false; QUEUE.addLast(cursor); return true; }

    private static final class Cursor {
        private final ResidentMood resident; private final GlobalPos site; private final boolean home; private int index; private boolean unloaded;
        private final AmenityEvidence evidence = new AmenityEvidence();
        Cursor(ResidentMood resident, GlobalPos site, boolean home) { this.resident = resident; this.site = site; this.home = home; }
        String key() { return resident.villagerId() + ":" + (home ? "home" : "work"); }
        boolean step(MinecraftServer server, int budget) {
            ServerLevel level = server.getLevel(site.dimension()); if (level == null) return finish(server.overworld().getGameTime(), true);
            for (int checked = 0; checked < budget && index < TOTAL; checked++, index++) {
                int horizontal = index % (WIDTH * WIDTH), y = index / (WIDTH * WIDTH) - 5, x = horizontal % WIDTH - 8, z = horizontal / WIDTH - 8;
                BlockPos pos = site.pos().offset(x, y, z); if (!level.hasChunkAt(pos)) { unloaded = true; continue; }
                evidence.observe(level,pos,level.getBlockState(pos));
            }
            return index >= TOTAL && finish(level.getGameTime(), unloaded);
        }
        private boolean finish(long gameTime, boolean stale) {
            if (stale) { AmenitySnapshot prior = home ? resident.homeAmenities() : resident.workAmenities(); AmenitySnapshot snapshot = new AmenitySnapshot(prior.points(), prior.categories(), prior.verifiedGameTime(), true); if (home) resident.homeAmenities(snapshot); else resident.workAmenities(snapshot); return true; }
            int cap = home ? 12 : 8; AmenitySnapshot snapshot = new AmenitySnapshot(evidence.points(cap), evidence.categories(), gameTime, false);
            if (home) resident.homeAmenities(snapshot); else resident.workAmenities(snapshot); return true;
        }
    }
}
