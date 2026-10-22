package com.siirio.jemserver.smp;

import com.siirio.jemworldbosstiers.api.WorldTierApi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class Parties {
    public CompoundTag bind(CompoundTag record) {
        if (record == null) return null;
        SmpRecords.require(record.hasUUID("id"), "unavailable");
        CompoundTag current = current(record.getUUID("id"));
        SmpRecords.require(current != null, "unavailable");
        return current;
    }
    private final SmpData storage;
    Parties(SmpData storage) { this.storage = storage; }
    public net.minecraft.nbt.CompoundTag snapshot(UUID id) { return storage.snapshot("parties", id); }
    public java.util.List<net.minecraft.nbt.CompoundTag> snapshots() { return storage.snapshots("parties"); }
    public CompoundTag current(UUID id) { return storage.current("parties", id); }
    public java.util.Collection<net.minecraft.nbt.CompoundTag> currentRows() { return storage.currentRows("parties"); }
    public CompoundTag create(UUID owner) { return storage.create("parties", owner); }
    public void prune() { storage.prune("parties", row -> com.siirio.jemserver.smp.events.StructureStaging.hasPendingRaidExit(row)); }

    public static final Set<String> ACTIVITIES =
            Set.of("BOSS", "CUSTOM");

    public static CompoundTag create(ServerPlayer player, SmpPartyRequest args) {
        return SmpData.get(player.server).edit(() -> createOwned(player, args, true));
    }

    public static CompoundTag createHosted(ServerPlayer player, SmpPartyRequest args) {
        return SmpData.get(player.server).edit(() -> createOwned(player, args, false));
    }

    private static CompoundTag createOwned(ServerPlayer player,SmpPartyRequest args,boolean posting) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        if(posting) {
            SmpRecords.require(SmpData.get(player.server).parties().currentRows().stream()
                    .noneMatch(row -> !SmpData.closed(row) && accepted(row, player.getUUID())), "already_in_party");
            SmpRecords.posting(player, "parties");
        }
        String activity = SmpRecords.text(args.activity(), SmpProtocol.MAX_LANGUAGE).toUpperCase(Locale.ROOT),
                title = SmpRecords.text(args.title(), SmpProtocol.MAX_TITLE);
        SmpRecords.require(ACTIVITIES.contains(activity) && !title.isBlank(), "invalid_party");
        int limit = args.slots();
        SmpRecords.require(limit >= 0, "invalid_number");
        String description = SmpRecords.text(args.description(), SmpConfig.MAX_TEXT.get());
        String language = SmpRecords.text(args.language(), SmpProtocol.MAX_LANGUAGE);
        long scheduled = args.scheduled();
        var data = SmpData.get(player.server);
        var row = data.parties().create(player.getUUID());
        row.putString("title", title);
        row.putString("activity", activity);
        row.putString("description", description);
        row.putString("language", language);
        row.putLong("scheduled", scheduled);
        row.putInt("limit", limit);
        row.putString("name", player.getGameProfile().getName());
        row.putBoolean("approval", args.approval());
        row.putBoolean("solo", args.solo());
        row.putString("state", "DRAFT");
        SmpRecords.locate(row, player);
        joinOwned(player, row, !posting);
        Profiles.count(player.server, player.getUUID(), "partiesHosted");
        
        data.parties().prune();
        return row;
    }

    public static void join(ServerPlayer player, CompoundTag row) {
        var data = SmpData.get(player.server);
        data.edit(() -> joinOwned(player, data.parties().bind(row), false));
    }

    private static void joinOwned(ServerPlayer player, CompoundTag row, boolean hosted) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        if (SmpRecords.member(row, player.getUUID())) return;
        SmpRecords.require(
                PartyAdmission.preparing(row), "combat_locked");
        boolean owner = row.getUUID("owner").equals(player.getUUID());
        SmpRecords.require(PartyAdmission.publishedFor(player, row), "not_published");
        SmpRecords.require(PartyAdmission.multiplayerFor(player, row), "solo");
        if(!hosted) SmpRecords.require(SmpData.get(player.server).parties().currentRows().stream()
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
        
        Profiles.count(player.server, player.getUUID(), "partiesJoined");
    }

    public static void makeSolo(ServerPlayer player, CompoundTag party) {
        var data = SmpData.get(player.server);
        data.edit(() -> makeSoloOwned(player, data.parties().bind(party)));
    }

    private static void makeSoloOwned(ServerPlayer player, CompoundTag party) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.owner(player, party);
        SmpRecords.require(PartyAdmission.canMakeSolo(player, party), "solo");
        party.putBoolean("solo", true);
        party.remove("invitations");
        SmpRecords.members(party).getCompound(player.getStringUUID()).putBoolean("ready", true);
        
    }

    public static boolean accepted(CompoundTag row, UUID player) {
        return SmpRecords.member(row, player)
                && row.getCompound("members").getCompound(player.toString()).getBoolean("accepted");
    }

    public static void action(
            ServerPlayer player, CompoundTag row, SmpActionKind action, SmpActionInput input) {
        var data = SmpData.get(player.server);
        data.edit(() -> actionOwned(player, data.parties().bind(row), action, input));
    }

    private static void actionOwned(
            ServerPlayer player, CompoundTag row, SmpActionKind action, SmpActionInput input) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.require(!SmpData.closed(row), "closed");
        var members = SmpRecords.members(row);
        switch (action) {
            case SOLO, MULTIPLAYER -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(
                        row.getString("state").equals("DRAFT") && members.size() == 1,
                        "unavailable");
                row.putBoolean("solo", action == SmpActionKind.SOLO);
            }
            case INVITE -> invite(player, row, ((SmpPlayerRequest) input).player());
            case JOIN -> join(player, row);
            case ARRIVE -> {
                SmpRecords.require(accepted(row, player.getUUID()), "not_member");
                HostedParties.arrive(player, row);
            }
            case LEAVE -> {
                SmpRecords.require(
                        PartyAdmission.canLeave(player, row),
                        "transfer_host_first");
                members.remove(player.getUUID().toString());
            }
            case READY -> {
                SmpRecords.require(PartyAdmission.canReady(player, row), "combat_locked");
                var member = members.getCompound(player.getUUID().toString());
                member.putBoolean("ready", !member.getBoolean("ready"));
            }
            case PUBLISH -> {
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
                                                        + " · "
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
            case APPROVE, REMOVE, TRANSFER -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(!row.getString("state").equals("ACTIVE"), "combat_locked");
                UUID target = ((SmpPlayerRequest) input).player();
                SmpRecords.require(members.contains(target.toString()), "unknown_player");
                if (action == SmpActionKind.APPROVE) {
                    var invited = player.server.getPlayerList().getPlayer(target);
                    SmpRecords.require(invited != null, "player_offline");
                    members.getCompound(target.toString()).putBoolean("accepted", true);
                    members.getCompound(target.toString()).putBoolean("ready", false);
                    SmpNetwork.open(invited, "parties", row.getUUID("id"));
                }
                else if (action == SmpActionKind.REMOVE) {
                    SmpRecords.require(!target.equals(player.getUUID()), "transfer_host_first");
                    members.remove(target.toString());
                } else {
                    SmpRecords.require(accepted(row, target), "not_member");
                    row.putUUID("owner", target);
                    row.putString("name", members.getCompound(target.toString()).getString("name"));
                }
            }
            case CANCEL, DISSOLVE -> {
                SmpRecords.owner(player, row);
                if (action == SmpActionKind.CANCEL && PartyAdmission.canCancelActive(player, row)) {
                    HostedParties.forfeit(player, row.getUUID("id"), com.siirio.jemserver.smp.events.HostedBoundary.actionToken(player.server, row));
                    return;
                }
                SmpRecords.require(!row.getString("state").equals("ACTIVE"), "combat_locked");
                HostedParties.release(player.server, row);
                row.putString("state", "CANCELLED");
            }
            case START -> {
                SmpRecords.owner(player, row);
                SmpRecords.require(PartyAdmission.canStart(player, row), "unavailable");
                HostedParties.start(player, row);
            }
            default -> throw new SmpActionFailure("unknown_action");
        }
        
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
        copy.putBoolean("closeLobby", SmpData.closed(row));
        if (SmpData.closed(row)) copy.putString("lobbyMessage", "party_dissolved");
        var candidates = new net.minecraft.nbt.ListTag();
        if (copy.getBoolean("leader") && !row.getBoolean("solo") && !row.getString("state").equals("ACTIVE") && !SmpData.closed(row)) {
            for (var target : player.server.getPlayerList().getPlayers()) {
                if (SmpRecords.member(row, target.getUUID()) || target.isSpectator()
                        || SmpData.get(player.server).parties().snapshots().stream().anyMatch(other -> !SmpData.closed(other) && accepted(other, target.getUUID()))) continue;
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
        var data = SmpData.get(player.server);
        data.edit(() -> inviteOwned(player, data.parties().bind(row), targetId));
    }

    private static void inviteOwned(ServerPlayer player, CompoundTag row, UUID targetId) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.owner(player, row);
        SmpRecords.require(PartyAdmission.canInvite(player, row), "combat_locked");
        var target = player.server.getPlayerList().getPlayer(targetId);
        SmpRecords.require(target != null && !target.isSpectator(), "player_offline");
        SmpRecords.require(!SmpRecords.member(row, targetId) && SmpData.get(player.server).parties().currentRows().stream()
                .noneMatch(other -> !SmpData.closed(other) && accepted(other, targetId)), "already_in_party");
        var invitations = row.getCompound("invitations");
        if (invitations.contains(targetId.toString())) return;
        SmpRecords.require(row.getInt("limit") == 0 || SmpRecords.members(row).size() < row.getInt("limit"), "full");
        invitations.putLong(targetId.toString(), System.currentTimeMillis());
        row.put("invitations", invitations);
        row.putString("state", "PUBLISHED");
        
        Notifications.send(player.server, targetId, "party_invitation", "parties " + row.getUUID("id"));
    }

    public static void resetReady(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> resetReadyOwned(player));
    }

    private static void resetReadyOwned(ServerPlayer player) {
        var data = SmpData.get(player.server);
        for (var row : data.parties().currentRows()) {
            if (SmpData.closed(row) || row.getString("state").equals("ACTIVE") || !SmpRecords.member(row, player.getUUID())) continue;
            SmpRecords.members(row).getCompound(player.getStringUUID()).putBoolean("ready", false);
            
        }
    }


}
