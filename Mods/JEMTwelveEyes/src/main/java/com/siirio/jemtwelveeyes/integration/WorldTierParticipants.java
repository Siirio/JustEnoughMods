package com.siirio.jemtwelveeyes.integration;

import com.siirio.jemworldbosstiers.api.WorldTierApi;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

public final class WorldTierParticipants {
    private WorldTierParticipants() {
    }

    public static Set<UUID> forEntity(LivingEntity entity) {
        if (!com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(entity).isEmpty()) {
            return new LinkedHashSet<>(com.siirio.jemworldbosstiers.api.HostedEncounterApi.eligibleParticipants(entity));
        }
        return WorldTierApi.encounter(entity)
                .<Set<UUID>>map(encounter -> new LinkedHashSet<>(encounter.participants()))
                .orElseGet(LinkedHashSet::new);
    }
}
