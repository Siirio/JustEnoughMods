package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class EventParties {
    public static void join(ServerPlayer player, EventState event, boolean solo) {
        var data = SmpData.get(player.server);
        data.edit(() -> joinOwned(player, data.events().bind(event), solo));
    }

    private static void joinOwned(ServerPlayer player, EventState event, boolean solo) {
        SmpRecords.require(EventRuntime.available(player.server, event), "unavailable");
        SmpRecords.require(!SmpData.closed(event) && event.pendingOutcome().isEmpty(), "unavailable");
        if (event.activity().equals("BOSS_RAID") && event.hasRaidArenaId())
            SmpRecords.require(!com.siirio.jemworldbosstiers.api.RaidArenaApi.participated(player.server, event.raidArenaId(), player.getUUID()), "raid_already_participated");
        var data = SmpData.get(player.server);
        var party = event.hasParty() ? data.parties().current(event.party()) : null;
        if (party != null && !SmpData.closed(party) && Parties.accepted(party, player.getUUID())) {
            if (party.state().equals("ACTIVE")) {
                SmpNetwork.open(player, "parties", party.id());
                return;
            }
            if (solo) {
                Parties.makeSolo(player, party);
                event.solo(true);
                
                if (event.state().equals("ACTIVE")) HostedParties.start(player, party);
            }
            SmpNetwork.open(player, "parties", party.id());
            return;
        }
        SmpRecords.require(EventAdmission.staging(event), "combat_locked");
        if (party == null || SmpData.closed(party)) {
            party = Parties.createHosted(player, SmpPartyRequest.hosted(
                    net.minecraft.network.chat.Component.translatable("jem.smp." + event.activity()).getString(), solo));
            party.bloodMoon(event.activity().equals("BLOOD_MOON"));
            party.raid(event.activity().equals("BOSS_RAID"));
            party.eventId(event.id());
            if (event.hasStructureId()) {
                party.structureId(event.structureId());
                party.structureBoss(event.structureBoss());
                party.structureBounds(event.structureBounds());
            }
            if (event.hasDimension()) {
                party.dimension(event.dimension());
                party.position(event.position());
            }
            event.party(party.id());
            event.solo(solo);
            if (!solo) party.state("PUBLISHED");
        } else {
            SmpRecords.require(EventAdmission.canJoinExisting(party, solo), "solo");
            Parties.join(player, party);
        }
        
        
        if (solo && event.state().equals("ACTIVE")) HostedParties.start(player, party);
        SmpNetwork.open(player, "parties", party.id());
    }

    public static void start(ServerPlayer host, PartyState party, Collection<UUID> accepted) {
        var data = SmpData.get(host.server);
        data.edit(() -> startOwned(host, data.parties().bind(party), accepted));
    }

    private static void startOwned(ServerPlayer host, PartyState party, Collection<UUID> accepted) {
        var participants = new EventParticipants(host.getUUID(), List.copyOf(accepted), party.solo());
        SmpRecords.require(EventAttempts.start(host, party.eventId(), participants), "combat_locked");
        party.state("ACTIVE");
        
    }

    private EventParties() {}
}
