package com.siirio.jemserver.smp;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class SmpActions {
    public static void handle(ServerPlayer player, SmpAction request) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpData.get(player.server).edit(() -> request.execute(player));
    }

    static void create(ServerPlayer player, SmpPartyRequest input) {
        var party = Parties.create(player, input);
        SmpNetwork.open(player, "parties", party.id());
        Navigation.accepted(player);
    }

    static void party(ServerPlayer player, SmpAction.PartyCommand request) {
        var data = SmpData.get(player.server);
        var party = data.parties().current(request.id());
        SmpRecords.require(party != null, "unavailable");
        var action = request.kind();
        var event = party.hasEventId() ? data.events().current(party.eventId()) : null;
        if (party.hasEventId()
                && !Set.of(SmpActionKind.CANCEL, SmpActionKind.DISSOLVE, SmpActionKind.LEAVE).contains(action)) {
            SmpRecords.require(com.siirio.jemserver.smp.events.EventRuntime.available(player.server, event), "unavailable");
        }
        if (action == SmpActionKind.START && party.state().equals("ACTIVE")) {
            SmpRecords.owner(player, party);
            return;
        }
        SmpRecords.require(party.revision() == request.revision(), "stale_revision");
        request.apply(player, party);
        for (var id : party.members().keySet()) {
            var member = player.server.getPlayerList().getPlayer(id);
            if (member != null) Navigation.accepted(member);
        }
    }

    static void event(ServerPlayer player, SmpAction.EventCommand request) {
        var event = SmpData.get(player.server).events().current(request.id());
        SmpRecords.require(event != null, "unavailable");
        SmpRecords.require(com.siirio.jemserver.smp.events.EventRuntime.available(player.server, event), "unavailable");
        SmpRecords.require(event.revision() == request.revision(), "stale_revision");
        request.apply(player, event);
    }

    static void navigateParty(ServerPlayer player, PartyState party) {
        var event = party.hasEventId() ? SmpData.get(player.server).events().current(party.eventId()) : null;
        SmpRecords.require(canNavigate(player, party, event), "private");
        Navigation.send(player, "parties", party.id(), party.title(),
                new ResourceLocation(event == null ? party.dimension() : event.dimension()),
                BlockPos.of(event == null ? party.position() : event.position()),
                event != null && event.hasEnds() ? event.ends() : Long.MAX_VALUE);
    }

    static void navigateEvent(ServerPlayer player, EventState event) {
        SmpRecords.require(canNavigate(event), "private");
        Navigation.send(player, "events", event.id(), event.title(), new ResourceLocation(event.dimension()),
                BlockPos.of(event.position()), event.hasEnds() ? event.ends() : Long.MAX_VALUE);
    }

    static void teleportEvent(ServerPlayer player, EventState event) {
        SmpRecords.require(canTeleportEvent(event), "unavailable");
        com.siirio.jemserver.smp.events.EventTravel.request(player, event.id(), event.activity().equals("RESOURCE_RUSH"));
    }

    static void joinEvent(ServerPlayer player, EventState event, boolean solo) {
        SmpRecords.require(EventAdmission.groupActivity(event), "unknown_action");
        EventParties.join(player, event, solo);
    }

    static boolean canNavigate(ServerPlayer player, PartyState party, EventState event) {
        boolean authorized = party.owner().equals(player.getUUID()) || Parties.accepted(party, player.getUUID());
        boolean located = party.hasEventId()
                ? event != null && event.hasDimension() && event.hasPosition()
                : party.hasDimension() && party.hasPosition();
        return authorized && !party.closed() && located;
    }

    static boolean canNavigate(EventState event) {
        return event != null && !event.closed() && event.hasDimension() && event.hasPosition();
    }

    static boolean canTeleportEvent(EventState event) { return canNavigate(event); }

    private SmpActions() {}
}
