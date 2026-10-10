package com.siirio.jemserver.smp;


import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpActions {
    public static void handle(ServerPlayer player, SmpAction request) {
        var input = request.input();
        var action = request.kind();
        String table = request.table().key();
        if ((action == SmpActionKind.CLAIM_REWARDS)) {
            SmpNetwork.open(player, "prizes", null);
            return;
        }
        if (table.equals("prizes") && ((action == SmpActionKind.CLAIM) || (action == SmpActionKind.CLAIM_ALL))) {
            SmpRecords.require((action == SmpActionKind.CLAIM_ALL) || request.id() != null, "invalid_request");
            LegacyRewards.migrate(player);
            SmpRecords.require(com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.claim(player,
                    (action == SmpActionKind.CLAIM_ALL) ? null : request.id()), "inventory_full");
            return;
        }
        if ((action == SmpActionKind.CLAIMS)) {
            com.siirio.jemserver.claims.Claims.openOverview(player);
            return;
        }
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
            com.siirio.jemserver.smp.shops.ShopIndex.action(player, request.id(), action.key());
            return;
        }
        SmpRecords.require(
                Set.of("parties", "events", "profiles").contains(table) && request.id() != null,
                "unknown_action");
        CompoundTag row = SmpData.get(player.server).find(table, request.id());
        SmpRecords.require(row != null, "unavailable");
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
            case TPA -> {
                SmpRecords.require(authorized, "private");
                var target = player.server.getPlayerList().getPlayer(row.getUUID("owner"));
                SmpRecords.require(target != null, "player_offline");
                player.server
                        .getCommands()
                        .performPrefixedCommand(
                                player.createCommandSourceStack(),
                                "tpa " + target.getGameProfile().getName());
            }
            case MESSAGE -> {
                String text = SmpRecords.text(((SmpMessageRequest) input).message(), SmpProtocol.MAX_MESSAGE);
                SmpRecords.require(!text.isBlank(), "invalid_text");
                var target = player.server.getPlayerList().getPlayer(row.getUUID("owner"));
                SmpRecords.require(target != null, "player_offline");
                player.server
                        .getCommands()
                        .performPrefixedCommand(
                                player.createCommandSourceStack(),
                                "msg " + target.getGameProfile().getName() + " " + text);
            }
            case JOIN_RAID, CREATE_PARTY, SOLO -> {
                if (table.equals("parties")) Parties.action(player, row, action, input);
                else {
                    SmpRecords.require(table.equals("events") && Set.of("BOSS_RAID", "BLOOD_MOON").contains(row.getString("activity")), "unknown_action");
                    EventParties.join(player, row, (action == SmpActionKind.SOLO));
                }
            }
            case INVITE -> {
                if (table.equals("parties")) {
                    Parties.action(player, row, action, input);
                    break;
                }
                SmpRecords.require(table.equals("profiles"), "unknown_action");
                var party =
                        SmpData.get(player.server).all("parties").stream()
                                .filter(
                                        p ->
                                                p.getUUID("owner").equals(player.getUUID())
                                                        && p.getString("state").equals("PUBLISHED"))
                                .max(java.util.Comparator.comparingLong(p -> p.getLong("created")))
                                .orElseThrow(
                                        () -> new SmpActionFailure("publish_party_first"));
                Parties.invite(player, party, row.getUUID("id"));
            }
            case VIEW_SHOPS ->
                    SmpNetwork.open(
                            player,
                            new SmpQuery(
                                    "shops",
                                    "search|near",
                                    row.getString("name") + "\u001f",
                                    0,
                                    null));
            case TERRITORY_RIGHTS -> com.siirio.jemserver.claims.Claims.openProfile(player, row.getUUID("id"));
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
