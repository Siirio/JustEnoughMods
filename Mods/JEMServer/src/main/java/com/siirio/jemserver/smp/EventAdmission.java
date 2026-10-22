package com.siirio.jemserver.smp;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

/** Event lifecycle and group-entry rules shared by commands and their projection. */
final class EventAdmission {
    private EventAdmission() {}

    static boolean groupActivity(CompoundTag event) {
        return Set.of("BOSS_RAID", "BLOOD_MOON").contains(event.getString("activity"));
    }
    static boolean staging(CompoundTag event) {
        return !event.getBoolean("combatStarted") && !event.hasUUID("bossEntity");
    }
    static boolean combat(CompoundTag event) {
        return groupActivity(event) || event.getString("activity").equals("BOSS");
    }
    static boolean canJoinExisting(CompoundTag party, boolean solo) {
        return !solo && !party.getBoolean("solo");
    }
    static String lifecycle(CompoundTag event) {
        String state=event.getString("state");
        if(state.equals("PLANNED"))return "SCHEDULED";
        return state.equals("ACTIVE")&&groupActivity(event)&&staging(event)?"STAGING":state;
    }
    static ListTag actions(ServerPlayer player, CompoundTag event) {
        var result=new ListTag();
        // Already-active membership reopens its lobby by command; the view omits that duplicate control.
        if(SmpData.closed(event)||!groupActivity(event)||!staging(event))return result;
        var party=event.hasUUID("party")?SmpData.get(player.server).parties().snapshot(event.getUUID("party")):null;
        boolean newParty=party==null||SmpData.closed(party);
        if(newParty||canJoinExisting(party,false)||Parties.accepted(party,player.getUUID())) {
            result.add(StringTag.valueOf("create_party"));
            if(newParty||PartyAdmission.canMakeSolo(player,party))result.add(StringTag.valueOf("solo"));
        }
        return result;
    }
}
