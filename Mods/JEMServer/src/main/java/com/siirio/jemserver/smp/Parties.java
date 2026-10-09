package com.siirio.jemserver.smp;

import com.google.gson.JsonObject;
import com.siirio.jemworldbosstiers.api.WorldTierApi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class Parties {
    public static final Set<String> ACTIVITIES =
            Set.of("BOSS", "CUSTOM");

    public static CompoundTag create(ServerPlayer player, JsonObject args) {
        return create(player,args,true);
    }

    public static CompoundTag createHosted(ServerPlayer player,JsonObject args) {
        return create(player,args,false);
    }

    private static CompoundTag create(ServerPlayer player,JsonObject args,boolean posting) {
        if(posting) {
            SmpRecords.require(SmpData.get(player.server).all("parties").stream()
                    .noneMatch(row -> !SmpData.closed(row) && accepted(row, player.getUUID())), "already_in_party");
            SmpRecords.posting(player, "parties");
        }
        String activity = SmpRecords.text(args, "activity", 24).toUpperCase(Locale.ROOT),
                title = SmpRecords.text(args, "title", 80);
        SmpRecords.require(ACTIVITIES.contains(activity) && !title.isBlank(), "invalid_party");
        int limit = SmpRecords.number(args, "slots", 0, 0, Integer.MAX_VALUE);
        String description = SmpRecords.text(args, "description", SmpConfig.MAX_TEXT.get());
        String language = SmpRecords.text(args, "language", 24);
        long scheduled = SmpRecords.time(args, "scheduled");
        var data = SmpData.get(player.server);
        var row = data.create("parties", player.getUUID());
        row.putString("title", title);
        row.putString("activity", activity);
        row.putString("description", description);
        row.putString("language", language);
        row.putLong("scheduled", scheduled);
        row.putInt("limit", limit);
        row.putString("name", player.getGameProfile().getName());
        row.putBoolean(
                "approval",
                args.has("approval")
                        && args.get("approval").getAsString().equalsIgnoreCase("true"));
        row.putBoolean(
                "solo",
                args.has("solo") && args.get("solo").getAsString().equalsIgnoreCase("true"));
        row.putString("state", "DRAFT");
        SmpRecords.locate(row, player);
        join(player, row, !posting);
        Profiles.count(player.server, player.getUUID(), "partiesHosted");
        data.changed(row);
        data.prune("parties");
        return row;
    }

    public static void join(ServerPlayer player, CompoundTag row) {
        join(player, row, false);
    }

    private static void join(ServerPlayer player, CompoundTag row, boolean hosted) {
        if (SmpRecords.member(row, player.getUUID())) return;
        SmpRecords.require(
                !SmpData.closed(row) && !row.getString("state").equals("ACTIVE"), "combat_locked");
        boolean owner = row.getUUID("owner").equals(player.getUUID());
        SmpRecords.require(owner || row.getString("state").equals("PUBLISHED"), "not_published");
        SmpRecords.require(owner || !row.getBoolean("solo"), "solo");
        if(!hosted) SmpRecords.require(SmpData.get(player.server).all("parties").stream()
                .noneMatch(other -> other != row && !SmpData.closed(other) && accepted(other, player.getUUID())), "already_in_party");
        var members = SmpRecords.members(row);
        SmpRecords.require(
                row.getInt("limit") == 0 || members.size() < row.getInt("limit"), "full");
        var member = new CompoundTag();
        member.putString("name", player.getGameProfile().getName());
        member.putLong("joinedAt", System.currentTimeMillis());
        member.putBoolean("accepted", owner || !row.getBoolean("approval"));
        member.putBoolean("ready", owner && row.getBoolean("solo"));
        row.getCompound("invitations").remove(player.getStringUUID());
        members.put(player.getUUID().toString(), member);
        SmpData.get(player.server).changed(row);
        Profiles.count(player.server, player.getUUID(), "partiesJoined");
    }

    public static boolean accepted(CompoundTag row, UUID player) {
        return SmpRecords.member(row, player)
                && row.getCompound("members").getCompound(player.toString()).getBoolean("accepted");
    }

    public static void action(
            ServerPlayer player, CompoundTag row, String action, JsonObject args) {
        SmpRecords.require(!SmpData.closed(row), "closed");
        var members = SmpRecords.members(row);
        switch (action) {
            case "solo", "multiplayer" -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(
                        row.getString("state").equals("DRAFT") && members.size() == 1,
                        "unavailable");
                row.putBoolean("solo", action.equals("solo"));
            }
            case "invite" -> invite(player, row, UUID.fromString(SmpRecords.text(args, "player", 36)));
            case "join" -> join(player, row);
            case "arrive" -> {
                SmpRecords.require(accepted(row, player.getUUID()), "not_member");
                HostedParties.arrive(player, row);
            }
            case "leave" -> {
                SmpRecords.require(
                        !row.getUUID("owner").equals(player.getUUID())
                                && !row.getString("state").equals("ACTIVE"),
                        "transfer_host_first");
                members.remove(player.getUUID().toString());
            }
            case "ready" -> {
                SmpRecords.require(!row.getString("state").equals("ACTIVE") && accepted(row, player.getUUID()), "combat_locked");
                var member = members.getCompound(player.getUUID().toString());
                member.putBoolean("ready", !member.getBoolean("ready"));
            }
            case "publish" -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(
                        !row.getBoolean("solo") && row.getString("state").equals("DRAFT"),
                        "unavailable");
                row.putString("state", "PUBLISHED");
                String target = "/smp parties " + row.getUUID("id");
                player.server
                        .getPlayerList()
                        .broadcastSystemMessage(
                                Component.literal(
                                                "["
                                                        + row.getString("activity")
                                                        + "] "
                                                        + player.getGameProfile().getName()
                                                        + ": "
                                                        + row.getString("title")
                                                        + " Р’В· "
                                                        + (row.getInt("limit") == 0
                                                                ? "\u221e"
                                                                : row.getInt("limit")))
                                        .withStyle(net.minecraft.ChatFormatting.GOLD)
                                        .append(Component.literal(" "))
                                        .append(
                                                Component.translatable("jem.smp.join")
                                                        .withStyle(
                                                                style ->
                                                                        style.withClickEvent(
                                                                                new ClickEvent(
                                                                                        ClickEvent
                                                                                                .Action
                                                                                                .RUN_COMMAND,
                                                                                        target
                                                                                                + " join"))))
                                        .append(Component.literal(" "))
                                        .append(
                                                Component.translatable("jem.smp.view")
                                                        .withStyle(
                                                                style ->
                                                                        style.withClickEvent(
                                                                                new ClickEvent(
                                                                                        ClickEvent
                                                                                                .Action
                                                                                                .RUN_COMMAND,
                                                                                        target)))),
                                false);
            }
            case "approve", "remove", "transfer" -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(!row.getString("state").equals("ACTIVE"), "combat_locked");
                UUID target = UUID.fromString(SmpRecords.text(args, "player", 36));
                SmpRecords.require(members.contains(target.toString()), "unknown_player");
                if (action.equals("approve")) {
                    var invited = player.server.getPlayerList().getPlayer(target);
                    SmpRecords.require(invited != null, "player_offline");
                    members.getCompound(target.toString()).putBoolean("accepted", true);
                    members.getCompound(target.toString()).putBoolean("ready", false);
                    SmpNetwork.open(invited, "parties", row.getUUID("id"));
                }
                else if (action.equals("remove")) {
                    SmpRecords.require(!target.equals(player.getUUID()), "transfer_host_first");
                    members.remove(target.toString());
                } else {
                    SmpRecords.require(accepted(row, target), "not_member");
                    row.putUUID("owner", target);
                    row.putString("name", members.getCompound(target.toString()).getString("name"));
                }
            }
            case "cancel", "dissolve" -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(!row.getString("state").equals("ACTIVE"), "combat_locked");
                HostedParties.release(player.server, row);
                row.putString("state", "CANCELLED");
            }
            case "start" -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(row.getString("activity").equals("BOSS"), "unavailable");
                HostedParties.start(player, row);
            }
            default -> throw new IllegalArgumentException("unknown_action");
        }
        SmpData.get(player.server).changed(row);
    }

    public static CompoundTag view(ServerPlayer player, CompoundTag row) {
        var copy = row.copy();
        copy.remove("invitations");
        int present = 0, ready = 0;
        var members = copy.getCompound("members");
        for (String key : members.getAllKeys()) {
            var member = members.getCompound(key);
            var online = player.server.getPlayerList().getPlayer(UUID.fromString(key));
            boolean simulated=member.getBoolean("simulated");
            member.putBoolean("online", online != null || simulated);
            boolean here =
                    member.getBoolean("accepted")
                            && (simulated || online != null && SmpRecords.present(row, online));
            member.putBoolean("present", here);
            UUID memberId=UUID.fromString(key);
            boolean leader=row.getUUID("owner").equals(player.getUUID());
            member.putBoolean("canApprove",leader&&!row.getString("state").equals("ACTIVE")&&!member.getBoolean("accepted"));
            member.putBoolean("canRemove",leader&&!row.getString("state").equals("ACTIVE")&&!memberId.equals(player.getUUID()));
            member.putBoolean("canTransfer",leader&&!row.getString("state").equals("ACTIVE")&&member.getBoolean("accepted")&&!memberId.equals(player.getUUID()));
            if (here) present++;
            if (online == null && !simulated) member.putBoolean("ready", false);
            if (member.getBoolean("ready") && member.getBoolean("accepted")) ready++;
        }
        copy.putInt("worldTier", WorldTierApi.currentTier(player.server));
        if (row.hasUUID("bossEntity")) {
            var level =
                    player.server.getLevel(
                            net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DIMENSION,
                                    new net.minecraft.resources.ResourceLocation(
                                            row.getString("dimension"))));
            if (level != null
                    && level.getEntity(row.getUUID("bossEntity")) instanceof LivingEntity boss)
                for (var status :
                        com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss)) {
                    var member = members.getCompound(status.playerId().toString());
                    member.putString("combatState", status.state());
                    member.putBoolean("reviveAvailable", status.reviveAvailable());
                    member.putLong("remainingTicks", status.remainingTicks());
                }
        }
        if (row.getString("activity").equals("BOSS") && !row.getString("state").equals("ACTIVE")) {
            var scaling = com.siirio.jemworldbosstiers.encounter.HostedScaling.calculate(Math.max(1, present), false, 1, 1);
            copy.putInt("partyBonusPercent", (int) Math.round((scaling.health() - 1) * 100));
        }
        copy.putInt("present", present);
        copy.putInt("readyCount", ready);
        copy.putBoolean("leader", row.getUUID("owner").equals(player.getUUID()));
        String reason = HostedParties.startReason(player, row);
        copy.putBoolean("canStart", reason.isEmpty());
        copy.putString("startReason", reason);
        copy.putBoolean("closeLobby", row.getString("state").equals("ACTIVE") || SmpData.closed(row));
        if (SmpData.closed(row)) copy.putString("lobbyMessage", "party_dissolved");
        var candidates = new net.minecraft.nbt.ListTag();
        if (copy.getBoolean("leader") && !row.getBoolean("solo") && !row.getString("state").equals("ACTIVE") && !SmpData.closed(row)) {
            for (var target : player.server.getPlayerList().getPlayers()) {
                if (SmpRecords.member(row, target.getUUID()) || target.isSpectator()
                        || SmpData.get(player.server).all("parties").stream().anyMatch(other -> !SmpData.closed(other) && accepted(other, target.getUUID()))) continue;
                var candidate = new CompoundTag();
                candidate.putUUID("id", target.getUUID());
                candidate.putString("name", target.getGameProfile().getName());
                candidate.putBoolean("pending", row.getCompound("invitations").contains(target.getStringUUID()));
                candidates.add(candidate);
            }
        }
        copy.put("inviteCandidates", candidates);
        return copy;
    }

    public static void invite(ServerPlayer player, CompoundTag row, UUID targetId) {
        SmpRecords.owner(player, row);
        SmpRecords.require(!row.getBoolean("solo") && !SmpData.closed(row) && !row.getString("state").equals("ACTIVE"), "combat_locked");
        var target = player.server.getPlayerList().getPlayer(targetId);
        SmpRecords.require(target != null && !target.isSpectator(), "player_offline");
        SmpRecords.require(!SmpRecords.member(row, targetId) && SmpData.get(player.server).all("parties").stream()
                .noneMatch(other -> !SmpData.closed(other) && accepted(other, targetId)), "already_in_party");
        var invitations = row.getCompound("invitations");
        if (invitations.contains(targetId.toString())) return;
        SmpRecords.require(row.getInt("limit") == 0 || SmpRecords.members(row).size() < row.getInt("limit"), "full");
        invitations.putLong(targetId.toString(), System.currentTimeMillis());
        row.put("invitations", invitations);
        row.putString("state", "PUBLISHED");
        SmpData.get(player.server).changed(row);
        Notifications.send(player.server, targetId, "party_invitation", "parties " + row.getUUID("id"));
    }

    public static void resetReady(ServerPlayer player) {
        var data = SmpData.get(player.server);
        for (var row : data.all("parties")) {
            if (SmpData.closed(row) || row.getString("state").equals("ACTIVE") || !SmpRecords.member(row, player.getUUID())) continue;
            SmpRecords.members(row).getCompound(player.getStringUUID()).putBoolean("ready", false);
            data.changed(row);
        }
    }

    private Parties() {}
}
