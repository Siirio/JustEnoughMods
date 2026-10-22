package com.siirio.jemserver.smp;


import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class Parties {
    private final SmpData storage;
    private final java.util.LinkedHashMap<UUID, PartyState> saved = new java.util.LinkedHashMap<>();
    private java.util.LinkedHashMap<UUID, PartyState> working;
    private boolean editing;
    Parties(SmpData storage) { this.storage = storage; }
    void beginEdit() { editing = true; }
    void endEdit() { working = null; editing = false; }
    private java.util.LinkedHashMap<UUID, PartyState> working() {
        if (!editing) throw new IllegalStateException("Mutation requires an SMP command");
        if (working == null) { working = new java.util.LinkedHashMap<>(); saved.forEach((id, row) -> working.put(id, row.copy())); }
        return working;
    }
    public PartyState snapshot(UUID id) { var row = (working == null ? saved : working).get(id); return row == null ? null : row.snapshot(); }
    public java.util.List<PartyState> snapshots() { return (working == null ? saved : working).values().stream().map(PartyState::snapshot).toList(); }
    public PartyState current(UUID id) { return working().get(id); }
    public java.util.Collection<PartyState> currentRows() { return java.util.List.copyOf(working().values()); }
    public PartyState bind(PartyState row) {
        if (row == null) return null;
        var current = current(row.id()); SmpRecords.require(current != null, "unavailable"); return current;
    }
    void read(net.minecraft.nbt.ListTag rows) { for (var tag : rows) { var row = PartyStateNbtCodec.decode((net.minecraft.nbt.CompoundTag) tag); if (row.id() != null) saved.put(row.id(), row); } }
    net.minecraft.nbt.ListTag write() { var rows = new net.minecraft.nbt.ListTag(); saved.values().forEach(row -> rows.add(PartyStateNbtCodec.encode(row))); return rows; }
    int checkpoint() {
        if (working == null) return 0;
        int changes = 0;
        for (var row : working.values()) {
            var previous = saved.get(row.id());
            if (previous != null && PartyStateNbtCodec.encode(row).equals(PartyStateNbtCodec.encode(previous))) continue;
            row.revision(previous == null ? Math.max(1, row.revision() + 1) : previous.revision() + 1);
            saved.put(row.id(), row.copy()); changes++;
        }
        for (UUID id : java.util.List.copyOf(saved.keySet())) if (!working.containsKey(id)) { saved.remove(id); changes++; }
        return changes;
    }
    public PartyState create(UUID owner) {
        var row = new PartyState(); row.id(UUID.randomUUID()); row.owner(owner); row.revision(0);
        row.created(System.currentTimeMillis()); row.state("DRAFT"); working().put(row.id(), row); return row;
    }
    public void prune() {
        int excess = working().size() - SmpConfig.HISTORY.get();
        var iterator = working().values().iterator();
        while (iterator.hasNext() && excess > 0) {
            var row = iterator.next();
            if (row.closed() && !com.siirio.jemserver.smp.events.StructureStaging.hasPendingRaidExit(row)) { iterator.remove(); excess--; }
        }
    }

    public static final Set<String> ACTIVITIES =
            Set.of("BOSS", "CUSTOM");

    public static PartyState create(ServerPlayer player, SmpPartyRequest args) {
        return SmpData.get(player.server).edit(() -> createOwned(player, args, true));
    }

    public static PartyState createHosted(ServerPlayer player, SmpPartyRequest args) {
        return SmpData.get(player.server).edit(() -> createOwned(player, args, false));
    }

    private static PartyState createOwned(ServerPlayer player,SmpPartyRequest args,boolean posting) {
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
        row.title(title);
        row.activity(activity);
        row.description(description);
        row.language(language);
        row.scheduled(scheduled);
        row.limit(limit);
        row.name(player.getGameProfile().getName());
        row.approval(args.approval());
        row.solo(args.solo());
        row.state("DRAFT");
        SmpRecords.locate(row, player);
        joinOwned(player, row, !posting);
        Profiles.count(player.server, player.getUUID(), com.siirio.jemserver.smp.ProfileCounter.PARTIES_HOSTED);
        
        data.parties().prune();
        return row;
    }

    public static void join(ServerPlayer player, PartyState row) {
        var data = SmpData.get(player.server);
        data.edit(() -> joinOwned(player, data.parties().bind(row), false));
    }

    private static void joinOwned(ServerPlayer player, PartyState row, boolean hosted) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        if (SmpRecords.member(row, player.getUUID())) return;
        SmpRecords.require(
                PartyAdmission.preparing(row), "combat_locked");
        boolean owner = row.owner().equals(player.getUUID());
        SmpRecords.require(PartyAdmission.publishedFor(player, row), "not_published");
        SmpRecords.require(PartyAdmission.multiplayerFor(player, row), "solo");
        if(!hosted) SmpRecords.require(SmpData.get(player.server).parties().currentRows().stream()
                .noneMatch(other -> other != row && !SmpData.closed(other) && accepted(other, player.getUUID())), "already_in_party");
        var members = row.members();
        SmpRecords.require(
                row.limit() == 0 || members.size() < row.limit(), "full");
        var member = new PartyMember();
        member.name(player.getGameProfile().getName());
        member.joinedAt(System.currentTimeMillis());
        member.accepted(owner || !row.approval());
        member.ready(owner && row.solo());
        row.removeInvitation(player.getUUID());
        row.addMember(player.getUUID(), member);
        
        Profiles.count(player.server, player.getUUID(), com.siirio.jemserver.smp.ProfileCounter.PARTIES_JOINED);
    }

    public static void makeSolo(ServerPlayer player, PartyState party) {
        var data = SmpData.get(player.server);
        data.edit(() -> makeSoloOwned(player, data.parties().bind(party)));
    }

    private static void makeSoloOwned(ServerPlayer player, PartyState party) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.owner(player, party);
        SmpRecords.require(PartyAdmission.canMakeSolo(player, party), "solo");
        party.solo(true);
        party.clearInvitations();
        party.member(player.getUUID()).ready(true);
        
    }

    public static boolean accepted(PartyState row, UUID player) {
        return SmpRecords.member(row, player)
                && row.member(player).accepted();
    }

    private static void command(ServerPlayer player, PartyState party, java.util.function.Consumer<PartyState> operation) {
        var data = SmpData.get(player.server);
        data.edit(() -> {
            var row = data.parties().bind(party);
            SmpRecords.require(SmpEnvironment.active(player), "unavailable");
            SmpRecords.require(!row.closed(), "closed");
            operation.accept(row);
        });
    }

    public static void setSolo(ServerPlayer player, PartyState party, boolean solo) {
        command(player, party, row -> {
            SmpRecords.owner(player, row);
            SmpRecords.require(row.state().equals("DRAFT") && row.members().size() == 1, "unavailable");
            row.solo(solo);
        });
    }
    public static void arrive(ServerPlayer player, PartyState party) {
        command(player, party, row -> {
            SmpRecords.require(accepted(row, player.getUUID()), "not_member");
            HostedParties.arrive(player, row);
        });
    }
    public static void leave(ServerPlayer player, PartyState party) {
        command(player, party, row -> {
            SmpRecords.require(PartyAdmission.canLeave(player, row), "transfer_host_first");
            row.removeMember(player.getUUID());
        });
    }
    public static void ready(ServerPlayer player, PartyState party) {
        command(player, party, row -> {
            SmpRecords.require(PartyAdmission.canReady(player, row), "combat_locked");
            var member = row.member(player.getUUID()); member.ready(!member.ready());
        });
    }
    public static void publish(ServerPlayer player, PartyState party) {
        command(player, party, row -> {

                SmpRecords.owner(player, row);
                SmpRecords.require(
                        !row.solo() && row.state().equals("DRAFT"),
                        "unavailable");
                row.state("PUBLISHED");
                String target = "/smp parties " + row.id();
                player.server
                        .getPlayerList()
                        .broadcastSystemMessage(
                                Component.literal(
                                                "["
                                                        + row.activity()
                                                        + "] "
                                                        + player.getGameProfile().getName()
                                                        + ": "
                                                        + row.title()
                                                        + " · "
                                                        + (row.limit() == 0
                                                                ? "\u221e"
                                                                : row.limit()))
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
            
        });
    }
    private static void requireMemberManagement(ServerPlayer player, PartyState row, UUID target) {
        SmpRecords.owner(player, row);
        SmpRecords.require(!row.state().equals("ACTIVE"), "combat_locked");
        SmpRecords.require(row.members().containsKey(target), "unknown_player");
    }
    public static void approve(ServerPlayer player, PartyState party, UUID target) {
        command(player, party, row -> {
            requireMemberManagement(player, row, target);
            var invited = player.server.getPlayerList().getPlayer(target);
            SmpRecords.require(invited != null, "player_offline");
            row.member(target).accepted(true); row.member(target).ready(false);
            SmpNetwork.open(invited, "parties", row.id());
        });
    }
    public static void remove(ServerPlayer player, PartyState party, UUID target) {
        command(player, party, row -> {
            requireMemberManagement(player, row, target);
            SmpRecords.require(!target.equals(player.getUUID()), "transfer_host_first"); row.removeMember(target);
        });
    }
    public static void transfer(ServerPlayer player, PartyState party, UUID target) {
        command(player, party, row -> {
            requireMemberManagement(player, row, target);
            SmpRecords.require(accepted(row, target), "not_member"); row.owner(target); row.name(row.member(target).name());
        });
    }
    public static void cancel(ServerPlayer player, PartyState party, boolean forfeitActive) {
        command(player, party, row -> {
            SmpRecords.owner(player, row);
            if (forfeitActive && PartyAdmission.canCancelActive(player, row)) {
                HostedParties.forfeit(player, row.id(), com.siirio.jemserver.smp.events.HostedBoundary.actionToken(player.server, row)); return;
            }
            SmpRecords.require(!row.state().equals("ACTIVE"), "combat_locked");
            HostedParties.release(player.server, row); row.state("CANCELLED");
        });
    }
    public static void start(ServerPlayer player, PartyState party) {
        command(player, party, row -> {
            SmpRecords.owner(player, row);
            SmpRecords.require(PartyAdmission.canStart(player, row), "unavailable"); HostedParties.start(player, row);
        });
    }

    public static void invite(ServerPlayer player, PartyState row, UUID targetId) {
        var data = SmpData.get(player.server);
        data.edit(() -> inviteOwned(player, data.parties().bind(row), targetId));
    }

    private static void inviteOwned(ServerPlayer player, PartyState row, UUID targetId) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.owner(player, row);
        SmpRecords.require(PartyAdmission.canInvite(player, row), "combat_locked");
        var target = player.server.getPlayerList().getPlayer(targetId);
        SmpRecords.require(target != null && !target.isSpectator(), "player_offline");
        SmpRecords.require(!SmpRecords.member(row, targetId) && SmpData.get(player.server).parties().currentRows().stream()
                .noneMatch(other -> !SmpData.closed(other) && accepted(other, targetId)), "already_in_party");
        var invitations = row.invitations();
        if (invitations.containsKey(targetId)) return;
        SmpRecords.require(row.limit() == 0 || row.members().size() < row.limit(), "full");
        row.invite(targetId, System.currentTimeMillis());
        row.state("PUBLISHED");
        
        Notifications.send(player.server, targetId, "party_invitation", "parties " + row.id());
    }

    public static void resetReady(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> resetReadyOwned(player));
    }

    private static void resetReadyOwned(ServerPlayer player) {
        var data = SmpData.get(player.server);
        for (var row : data.parties().currentRows()) {
            if (SmpData.closed(row) || row.state().equals("ACTIVE") || !SmpRecords.member(row, player.getUUID())) continue;
            row.member(player.getUUID()).ready(false);
            
        }
    }


}
