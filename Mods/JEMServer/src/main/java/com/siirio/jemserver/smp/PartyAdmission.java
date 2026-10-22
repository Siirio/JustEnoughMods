package com.siirio.jemserver.smp;

import net.minecraft.server.level.ServerPlayer;

/** Shared party permission predicates. Target-dependent checks remain at the command boundary. */
final class PartyAdmission {
    private PartyAdmission() {}

    static boolean owner(ServerPlayer player, PartyState party) {
        return party.owner().equals(player.getUUID());
    }
    static boolean preparing(PartyState party) {
        return !SmpData.closed(party) && !party.state().equals("ACTIVE");
    }
    static boolean canInvite(ServerPlayer player, PartyState party) {
        return owner(player, party) && preparing(party) && !party.solo();
    }
    static boolean canLeave(ServerPlayer player, PartyState party) {
        return !owner(player, party) && preparing(party);
    }
    static boolean canReady(ServerPlayer player, PartyState party) {
        return preparing(party) && Parties.accepted(party, player.getUUID());
    }
    static boolean canArrive(ServerPlayer player, PartyState party) {
        return canReady(player, party);
    }
    static boolean canStart(ServerPlayer player, PartyState party) {
        return owner(player, party) && !SmpData.closed(party) && party.activity().equals("BOSS");
    }
    static boolean canCancelActive(ServerPlayer player, PartyState party) {
        return canStart(player, party) && party.state().equals("ACTIVE");
    }
    static boolean canDissolve(ServerPlayer player, PartyState party) {
        return owner(player, party) && preparing(party);
    }
    static boolean canMakeSolo(ServerPlayer player, PartyState party) {
        return owner(player, party) && party.members().size() == 1;
    }
    static boolean publishedFor(ServerPlayer player, PartyState party) {
        return owner(player, party) || party.state().equals("PUBLISHED");
    }
    static boolean multiplayerFor(ServerPlayer player, PartyState party) {
        return owner(player, party) || !party.solo();
    }

    static java.util.List<String> actions(ServerPlayer player, PartyState party) {
        var actions = new java.util.ArrayList<String>();
        if (canCancelActive(player, party)) add(actions, "cancel");
        if (!preparing(party)) return java.util.List.copyOf(actions);
        // Visibility intentionally omits idempotent START/JOIN and non-member LEAVE.
        if (owner(player, party)) {
            if (canStart(player, party)) add(actions, "start");
            if (canDissolve(player, party)) add(actions, "dissolve");
            if (canInvite(player, party)) add(actions, "invite");
        } else if (!SmpRecords.member(party, player.getUUID()) && publishedFor(player, party)
                && multiplayerFor(player, party)) add(actions, "join");
        else if (SmpRecords.member(party, player.getUUID()) && canLeave(player, party)) add(actions, "leave");
        if (party.activity().equals("BOSS") && canReady(player, party)) add(actions, "ready");
        if (canArrive(player, party)) add(actions, "arrive");
        return java.util.List.copyOf(actions);
    }
    private static void add(java.util.List<String> actions, String action) { actions.add(action); }
}
