package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

/** Shared party permission predicates. Target-dependent checks remain at the command boundary. */
final class PartyAdmission {
    private PartyAdmission() {}

    static boolean owner(ServerPlayer player, CompoundTag party) {
        return party.getUUID("owner").equals(player.getUUID());
    }
    static boolean preparing(CompoundTag party) {
        return !SmpData.closed(party) && !party.getString("state").equals("ACTIVE");
    }
    static boolean canInvite(ServerPlayer player, CompoundTag party) {
        return owner(player, party) && preparing(party) && !party.getBoolean("solo");
    }
    static boolean canLeave(ServerPlayer player, CompoundTag party) {
        return !owner(player, party) && preparing(party);
    }
    static boolean canReady(ServerPlayer player, CompoundTag party) {
        return preparing(party) && Parties.accepted(party, player.getUUID());
    }
    static boolean canArrive(ServerPlayer player, CompoundTag party) {
        return canReady(player, party);
    }
    static boolean canStart(ServerPlayer player, CompoundTag party) {
        return owner(player, party) && !SmpData.closed(party) && party.getString("activity").equals("BOSS");
    }
    static boolean canCancelActive(ServerPlayer player, CompoundTag party) {
        return canStart(player, party) && party.getString("state").equals("ACTIVE");
    }
    static boolean canDissolve(ServerPlayer player, CompoundTag party) {
        return owner(player, party) && preparing(party);
    }
    static boolean canMakeSolo(ServerPlayer player, CompoundTag party) {
        return owner(player, party) && party.getCompound("members").size() == 1;
    }
    static boolean publishedFor(ServerPlayer player, CompoundTag party) {
        return owner(player, party) || party.getString("state").equals("PUBLISHED");
    }
    static boolean multiplayerFor(ServerPlayer player, CompoundTag party) {
        return owner(player, party) || !party.getBoolean("solo");
    }

    static ListTag actions(ServerPlayer player, CompoundTag party) {
        var actions = new ListTag();
        if (canCancelActive(player, party)) add(actions, "cancel");
        if (!preparing(party)) return actions;
        // Visibility intentionally omits idempotent START/JOIN and non-member LEAVE.
        if (owner(player, party)) {
            if (canStart(player, party)) add(actions, "start");
            if (canDissolve(player, party)) add(actions, "dissolve");
            if (canInvite(player, party)) add(actions, "invite");
        } else if (!SmpRecords.member(party, player.getUUID()) && publishedFor(player, party)
                && multiplayerFor(player, party)) add(actions, "join");
        else if (SmpRecords.member(party, player.getUUID()) && canLeave(player, party)) add(actions, "leave");
        if (party.getString("activity").equals("BOSS") && canReady(player, party)) add(actions, "ready");
        if (canArrive(player, party)) add(actions, "arrive");
        return actions;
    }
    private static void add(ListTag actions, String action) { actions.add(StringTag.valueOf(action)); }
}
