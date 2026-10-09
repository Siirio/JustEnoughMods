package com.siirio.jemserver.smp;

import com.google.gson.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpActions {
    public static void handle(ServerPlayer player, SmpNetwork.Action request) {
        JsonObject args = JsonParser.parseString(request.json()).getAsJsonObject();
        String action = request.action();
        String table = request.table();
        if (action.equals("claim_rewards")) {
            SmpNetwork.open(player, "prizes", null);
            return;
        }
        if (table.equals("prizes") && (action.equals("claim") || action.equals("claim_all"))) {
            SmpRecords.require(action.equals("claim_all") || request.id() != null, "invalid_request");
            LegacyRewards.migrate(player);
            SmpRecords.require(com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.claim(player,
                    action.equals("claim_all") ? null : request.id()), "inventory_full");
            return;
        }
        if (action.equals("claims")) {
            com.siirio.jemserver.claims.Claims.openOverview(player);
            return;
        }
        if (action.equals("create")) {
            if (table.equals("parties")) {
                var party = Parties.create(player, args);
                SmpNetwork.open(player, "parties", party.getUUID("id"));
            }
            else throw new IllegalArgumentException("unknown_action");
            Navigation.accepted(player);
            return;
        }
        if (table.equals("shops")) {
            com.siirio.jemserver.smp.shops.ShopIndex.action(player, request.id(), action);
            return;
        }
        SmpRecords.require(
                Set.of("parties", "events", "profiles").contains(table) && request.id() != null,
                "unknown_action");
        CompoundTag row = SmpData.get(player.server).find(table, request.id());
        SmpRecords.require(row != null, "unavailable");
        if (table.equals("parties") && action.equals("start") && row.getString("state").equals("ACTIVE")) {
            SmpRecords.owner(player, row);
            return;
        }
        SmpRecords.require(row.getInt("revision") == request.revision(), "stale_revision");
        boolean authorized =
                table.equals("events")
                        || row.getUUID("owner").equals(player.getUUID())
                        || table.equals("parties") && Parties.accepted(row, player.getUUID());
        switch (action) {
            case "navigate" -> {
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
            case "teleport_event" -> {
                SmpRecords.require(table.equals("events")&&!SmpData.closed(row)&&row.contains("dimension")&&row.contains("position"),"unavailable");
                boolean inside=row.getString("activity").equals("RESOURCE_RUSH");
                com.siirio.jemserver.smp.events.EventTravel.request(player,row.getUUID("id"),inside);
            }
            case "tpa" -> {
                SmpRecords.require(authorized, "private");
                var target = player.server.getPlayerList().getPlayer(row.getUUID("owner"));
                SmpRecords.require(target != null, "player_offline");
                player.server
                        .getCommands()
                        .performPrefixedCommand(
                                player.createCommandSourceStack(),
                                "tpa " + target.getGameProfile().getName());
            }
            case "message" -> {
                String text = SmpRecords.text(args, "message", 256);
                SmpRecords.require(!text.isBlank(), "invalid_text");
                var target = player.server.getPlayerList().getPlayer(row.getUUID("owner"));
                SmpRecords.require(target != null, "player_offline");
                player.server
                        .getCommands()
                        .performPrefixedCommand(
                                player.createCommandSourceStack(),
                                "msg " + target.getGameProfile().getName() + " " + text);
            }
            case "join_raid", "create_party", "solo" -> {
                if (table.equals("parties")) Parties.action(player, row, action, args);
                else {
                    SmpRecords.require(table.equals("events") && Set.of("BOSS_RAID", "BLOOD_MOON").contains(row.getString("activity")), "unknown_action");
                    com.siirio.jemserver.smp.events.EventParty.join(player, row, action.equals("solo"));
                }
            }
            case "invite" -> {
                if (table.equals("parties")) {
                    Parties.action(player, row, action, args);
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
                                        () -> new IllegalArgumentException("publish_party_first"));
                Parties.invite(player, party, row.getUUID("id"));
            }
            case "view_shops" ->
                    SmpNetwork.open(
                            player,
                            new SmpNetwork.Query(
                                    "shops",
                                    "search|near",
                                    row.getString("name") + "\u001f",
                                    0,
                                    null));
            case "territory_rights" -> com.siirio.jemserver.claims.Claims.openProfile(player, row.getUUID("id"));
            default -> {
                if (table.equals("parties")) Parties.action(player, row, action, args);
                else throw new IllegalArgumentException("unknown_action");
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
