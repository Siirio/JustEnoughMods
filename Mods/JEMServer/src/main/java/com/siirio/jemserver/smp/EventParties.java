package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class EventParties {
    public static void join(ServerPlayer player, CompoundTag event, boolean solo) {
        var data = SmpData.get(player.server);
        data.edit(() -> joinOwned(player, data.events().bind(event), solo));
    }

    private static void joinOwned(ServerPlayer player, CompoundTag event, boolean solo) {
        SmpRecords.require(EventRuntime.available(player.server, event), "unavailable");
        SmpRecords.require(!SmpData.closed(event), "unavailable");
        if (event.getString("activity").equals("BOSS_RAID") && event.contains("raidArenaId"))
            SmpRecords.require(!com.siirio.jemworldbosstiers.api.RaidArenaApi.participated(player.server, event.getString("raidArenaId"), player.getUUID()), "raid_already_participated");
        var data = SmpData.get(player.server);
        var party = event.hasUUID("party") ? data.parties().current(event.getUUID("party")) : null;
        if (party != null && !SmpData.closed(party) && Parties.accepted(party, player.getUUID())) {
            if (party.getString("state").equals("ACTIVE")) {
                SmpNetwork.open(player, "parties", party.getUUID("id"));
                return;
            }
            if (solo) {
                Parties.makeSolo(player, party);
                event.putBoolean("solo", true);
                
                if (event.getString("state").equals("ACTIVE")) HostedParties.start(player, party);
            }
            SmpNetwork.open(player, "parties", party.getUUID("id"));
            return;
        }
        SmpRecords.require(EventAdmission.staging(event), "combat_locked");
        if (party == null || SmpData.closed(party)) {
            party = Parties.createHosted(player, SmpPartyRequest.hosted(
                    net.minecraft.network.chat.Component.translatable("jem.smp." + event.getString("activity")).getString(), solo));
            party.putBoolean("bloodMoon", event.getString("activity").equals("BLOOD_MOON"));
            party.putBoolean("raid", event.getString("activity").equals("BOSS_RAID"));
            party.putUUID("eventId", event.getUUID("id"));
            if (event.hasUUID("structureId")) {
                party.putUUID("structureId", event.getUUID("structureId"));
                party.putString("structureBoss", event.getString("structureBoss"));
                party.putIntArray("structureBounds", event.getIntArray("structureBounds"));
            }
            if (event.contains("dimension")) {
                party.putString("dimension", event.getString("dimension"));
                party.putLong("position", event.getLong("position"));
            }
            event.putUUID("party", party.getUUID("id"));
            event.putBoolean("solo", solo);
            if (!solo) party.putString("state", "PUBLISHED");
        } else {
            SmpRecords.require(EventAdmission.canJoinExisting(party, solo), "solo");
            Parties.join(player, party);
        }
        
        
        if (solo && event.getString("state").equals("ACTIVE")) HostedParties.start(player, party);
        SmpNetwork.open(player, "parties", party.getUUID("id"));
    }

    public static void start(ServerPlayer host, CompoundTag party, Collection<UUID> accepted) {
        var data = SmpData.get(host.server);
        data.edit(() -> startOwned(host, data.parties().bind(party), accepted));
    }

    private static void startOwned(ServerPlayer host, CompoundTag party, Collection<UUID> accepted) {
        var participants = new EventParticipants(host.getUUID(), List.copyOf(accepted), party.getBoolean("solo"));
        if (!EventAttempts.start(host, party.getUUID("eventId"), participants)) return;
        party.putString("state", "ACTIVE");
        
    }

    private EventParties() {}
}
