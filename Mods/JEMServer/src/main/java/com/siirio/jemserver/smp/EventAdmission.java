package com.siirio.jemserver.smp;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Event lifecycle and group-entry rules shared by commands and their projection. */
final class EventAdmission {
    private EventAdmission() {}

    static boolean groupActivity(EventState event) {
        return Set.of("BOSS_RAID", "BLOOD_MOON").contains(event.activity());
    }
    static boolean staging(EventState event) {
        return event.pendingOutcome().isEmpty() && !event.hasEncounterParticipants()
                && !event.combatStarted() && !event.hasBossEntity();
    }
    static boolean combat(EventState event) {
        return groupActivity(event) || event.activity().equals("BOSS");
    }
    static boolean canJoinExisting(PartyState party, boolean solo) {
        return !solo && !party.solo();
    }
    static String lifecycle(EventState event) {
        String state=event.state();
        if(state.equals("PLANNED"))return "SCHEDULED";
        return state.equals("ACTIVE")&&groupActivity(event)&&staging(event)?"STAGING":state;
    }
    static java.util.List<String> actions(ServerPlayer player, EventState event) {
        var result=new java.util.ArrayList<String>();
        // Already-active membership reopens its lobby by command; the view omits that duplicate control.
        if(SmpData.closed(event)||!groupActivity(event)||!staging(event))return java.util.List.copyOf(result);
        var party=event.hasParty()?SmpData.get(player.server).parties().snapshot(event.party()):null;
        boolean newParty=party==null||SmpData.closed(party);
        if(newParty||canJoinExisting(party,false)||Parties.accepted(party,player.getUUID())) {
            result.add("create_party");
            if(newParty||PartyAdmission.canMakeSolo(player,party))result.add("solo");
        }
        return java.util.List.copyOf(result);
    }
}
