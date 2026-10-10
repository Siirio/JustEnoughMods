package com.siirio.jemclaims;

import com.siirio.jemtwelveeyes.api.CampaignApi;
import net.minecraft.server.MinecraftServer;

final class CampaignClaimZones {
    private CampaignClaimZones() {
    }

    static void observe(BossClaimZones zones, MinecraftServer server) {
        for (var territory : CampaignApi.generatedSites(server)) {
            String dimension = territory.dimension().toString();
            zones.put("site|" + dimension + "|" + territory.entity(), new BossClaimZone(dimension,
                    territory.minX(), territory.minZ(), territory.maxX(), territory.maxZ()));
        }
    }
}
