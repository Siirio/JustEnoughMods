package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class EventParty {
    public static void join(ServerPlayer player, CompoundTag event, boolean solo) {
        SmpRecords.require(!SmpData.closed(event), "unavailable");
        var data = SmpData.get(player.server);
        var party = event.hasUUID("party") ? data.find("parties", event.getUUID("party")) : null;
        if (party != null && !SmpData.closed(party) && Parties.accepted(party, player.getUUID())) {
            if (party.getString("state").equals("ACTIVE")) {
                SmpNetwork.open(player, "parties", party.getUUID("id"));
                return;
            }
            if (solo) {
                SmpRecords.owner(player, party);
                SmpRecords.require(SmpRecords.members(party).size() == 1, "solo");
                party.putBoolean("solo", true);
                party.remove("invitations");
                SmpRecords.members(party).getCompound(player.getStringUUID()).putBoolean("ready", true);
                event.putBoolean("solo", true);
                data.changed(party);
                data.changed(event);
                if (event.getString("state").equals("ACTIVE")) HostedParties.start(player, party);
            }
            SmpNetwork.open(player, "parties", party.getUUID("id"));
            return;
        }
        SmpRecords.require(!event.getBoolean("combatStarted") && !event.hasUUID("bossEntity"), "combat_locked");
        if (party == null || SmpData.closed(party)) {
            var args = new com.google.gson.JsonObject();
            args.addProperty("activity", "BOSS");
            args.addProperty("title", net.minecraft.network.chat.Component.translatable("jem.smp." + event.getString("activity")).getString());
            args.addProperty("solo", solo);
            party = Parties.createHosted(player, args);
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
            SmpRecords.require(!solo && !party.getBoolean("solo"), "solo");
            Parties.join(player, party);
        }
        data.changed(event);
        data.changed(party);
        if (solo && event.getString("state").equals("ACTIVE")) HostedParties.start(player, party);
        SmpNetwork.open(player, "parties", party.getUUID("id"));
    }

    public static void start(ServerPlayer host, CompoundTag party, Collection<UUID> accepted) {
        var event = SmpData.get(host.server).find("events", party.getUUID("eventId"));
        SmpRecords.require(event != null && event.getString("state").equals("ACTIVE") && EventRegions.near(event, host.serverLevel(), host.blockPosition()), "travel_to_arena");
        if (event.getBoolean("combatStarted")) return;
        var participants = new LinkedHashSet<>(accepted);
        participants.add(host.getUUID());
        var encounter = new EncounterContext(event);
        encounter.initialize(host, participants, party.getBoolean("solo"));
        SmpRecords.require(encounter.prepare(host.server), "no_safe_arrival");
        var level=EventRegions.level(host.server,event);
        encounter.awaitEntry(level);
        event.putBoolean("solo", party.getBoolean("solo"));
        event.putUUID("attemptId",UUID.randomUUID());
        event.putBoolean("combatStarted", true);
        event.putLong("nextWave", System.currentTimeMillis());
        if(level!=null) EventRegions.ejectOutsiders(level,event);
        party.putString("state", "ACTIVE");
        SmpData.get(host.server).changed(party);
        SmpData.get(host.server).changed(event);
    }

    private EventParty() {}
}
