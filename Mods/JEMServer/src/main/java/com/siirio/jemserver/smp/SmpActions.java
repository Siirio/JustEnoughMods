package com.siirio.jemserver.smp;


import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpActions {
    public static void handle(ServerPlayer player, SmpAction request) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpData.get(player.server).edit(() -> request.execute(player));
    }

    static void create(ServerPlayer player, SmpPartyRequest input) {
        var party = Parties.create(player, input);
        SmpNetwork.open(player, "parties", party.getUUID("id"));
        Navigation.accepted(player);
    }

    static void party(ServerPlayer player, SmpAction.PartyCommand request) {
        existing(player, request);
    }

    static void event(ServerPlayer player, SmpAction.EventCommand request) {
        existing(player, request);
    }

    private static void existing(ServerPlayer player, SmpAction request) {
        var input = request.input();
        var action = request.kind();
        String table = request.table().key();
        CompoundTag row = request instanceof SmpAction.PartyCommand ? SmpData.get(player.server).parties().current(request.id()) : SmpData.get(player.server).events().current(request.id());
        SmpRecords.require(row != null, "unavailable");
        if ((table.equals("events") || row.hasUUID("eventId"))
                && !Set.of(SmpActionKind.CANCEL, SmpActionKind.DISSOLVE, SmpActionKind.LEAVE).contains(action)) {
            var event = table.equals("events") ? row : SmpData.get(player.server).events().current(row.getUUID("eventId"));
            SmpRecords.require(com.siirio.jemserver.smp.events.EventRuntime.available(player.server, event), "unavailable");
        }
        if (table.equals("parties") && (action == SmpActionKind.START) && row.getString("state").equals("ACTIVE")) {
            SmpRecords.owner(player, row);
            return;
        }
        SmpRecords.require(row.getInt("revision") == request.revision(), "stale_revision");
        switch (action) {
            case NAVIGATE -> {
                var location = row.hasUUID("eventId") ? SmpData.get(player.server).events().current(row.getUUID("eventId")) : row;
                SmpRecords.require(canNavigate(player, table, row, location), "private");
                Navigation.send(
                        player,
                        table,
                        row.getUUID("id"),
                        row.getString("title"),
                        new ResourceLocation(location.getString("dimension")),
                        BlockPos.of(location.getLong("position")),
                        location.contains("ends") ? location.getLong("ends") : Long.MAX_VALUE);
            }
            case TELEPORT_EVENT -> {
                SmpRecords.require(canTeleportEvent(table, row),"unavailable");
                boolean inside=row.getString("activity").equals("RESOURCE_RUSH");
                com.siirio.jemserver.smp.events.EventTravel.request(player,row.getUUID("id"),inside);
            }
            case JOIN_RAID, CREATE_PARTY, SOLO -> {
                if (table.equals("parties")) Parties.action(player, row, action, input);
                else {
                    SmpRecords.require(table.equals("events") && EventAdmission.groupActivity(row), "unknown_action");
                    EventParties.join(player, row, (action == SmpActionKind.SOLO));
                }
            }
            case INVITE -> {
                SmpRecords.require(table.equals("parties"), "unknown_action");
                Parties.action(player, row, action, input);
            }
            default -> {
                if (table.equals("parties")) Parties.action(player, row, action, input);
                else throw new SmpActionFailure("unknown_action");
            }
        }
        if (table.equals("parties"))
            for (UUID id : SmpRecords.memberIds(row)) {
                var member = player.server.getPlayerList().getPlayer(id);
                if (member != null) Navigation.accepted(member);
            }
    }

    static boolean canNavigate(ServerPlayer player, String table, CompoundTag row, CompoundTag location) {
        boolean authorized=table.equals("events")||row.getUUID("owner").equals(player.getUUID())
                ||table.equals("parties")&&Parties.accepted(row,player.getUUID());
        return authorized&&!SmpData.closed(row)&&location!=null&&location.contains("dimension")&&location.contains("position");
    }

    static boolean canTeleportEvent(String table, CompoundTag row) {
        return table.equals("events")&&!SmpData.closed(row)&&row.contains("dimension")&&row.contains("position");
    }

    private SmpActions() {}
}
