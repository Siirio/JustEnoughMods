package com.siirio.jemserver.smp;


import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpActions {
    public static void handle(ServerPlayer player, SmpAction request) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.require(request.table() != SmpTable.PROFILES && request.table() != SmpTable.PRIZES
                && request.kind() != SmpActionKind.CLAIM_REWARDS, "unavailable");
        var input = request.input();
        var action = request.kind();
        String table = request.table().key();
        if ((action == SmpActionKind.CREATE)) {
            if (table.equals("parties")) {
                var party = Parties.create(player, (SmpPartyRequest) input);
                SmpNetwork.open(player, "parties", party.getUUID("id"));
            }
            else throw new SmpActionFailure("unknown_action");
            Navigation.accepted(player);
            return;
        }
        if (table.equals("shops")) {
            com.siirio.jemserver.smp.shops.ShopIndex.action(player, request.id(), request.revision(), action.key(), request.request());
            return;
        }
        SmpRecords.require(
                Set.of("parties", "events").contains(table) && request.id() != null,
                "unknown_action");
        CompoundTag row = SmpData.get(player.server).find(table, request.id());
        SmpRecords.require(row != null, "unavailable");
        if ((table.equals("events") || row.hasUUID("eventId"))
                && !Set.of(SmpActionKind.CANCEL, SmpActionKind.DISSOLVE, SmpActionKind.LEAVE).contains(action)) {
            var event = table.equals("events") ? row : SmpData.get(player.server).find("events", row.getUUID("eventId"));
            SmpRecords.require(com.siirio.jemserver.smp.events.EventRuntime.available(player.server, event), "unavailable");
        }
        if (table.equals("parties") && (action == SmpActionKind.START) && row.getString("state").equals("ACTIVE")) {
            SmpRecords.owner(player, row);
            return;
        }
        SmpRecords.require(row.getInt("revision") == request.revision(), "stale_revision");
        boolean authorized =
                table.equals("events")
                        || row.getUUID("owner").equals(player.getUUID())
                        || table.equals("parties") && Parties.accepted(row, player.getUUID());
        switch (action) {
            case NAVIGATE -> {
                var location = row.hasUUID("eventId") ? SmpData.get(player.server).find("events", row.getUUID("eventId")) : row;
                SmpRecords.require(authorized && !SmpData.closed(row) && location != null && location.contains("dimension") && location.contains("position"), "private");
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
                SmpRecords.require(table.equals("events")&&!SmpData.closed(row)&&row.contains("dimension")&&row.contains("position"),"unavailable");
                boolean inside=row.getString("activity").equals("RESOURCE_RUSH");
                com.siirio.jemserver.smp.events.EventTravel.request(player,row.getUUID("id"),inside);
            }
            case JOIN_RAID, CREATE_PARTY, SOLO -> {
                if (table.equals("parties")) Parties.action(player, row, action, input);
                else {
                    SmpRecords.require(table.equals("events") && Set.of("BOSS_RAID", "BLOOD_MOON").contains(row.getString("activity")), "unknown_action");
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

    private SmpActions() {}
}
