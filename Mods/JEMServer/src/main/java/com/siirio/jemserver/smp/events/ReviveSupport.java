package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import com.siirio.jemserver.smp.Parties;
import com.siirio.jemworldbosstiers.encounter.HostedEncounters;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

public final class ReviveSupport {
    private static final int TICKS_PER_SECOND=20;
    public static boolean downed(ServerPlayer player) {
        return net.minecraftforge.fml.ModList.get().isLoaded("playerrevive") && team.creative.playerrevive.server.PlayerReviveServer.isBleeding(player);
    }
    public static boolean timerPaused(ServerPlayer player) {
        if (EventSession.forPlayer(player) != null) return false;
        return downed(player) && team.creative.playerrevive.PlayerRevive.CONFIG.revive.haltBleedTime
                && !team.creative.playerrevive.server.PlayerReviveServer.getBleeding(player).revivingPlayers().isEmpty();
    }
    public static long remainingTicks(ServerPlayer player) {
        if (!downed(player)) return 0;
        var blood=EventSession.forPlayer(player);
        if(blood==null) return Math.max(0, team.creative.playerrevive.server.PlayerReviveServer.timeLeft(player));
        var member=SmpRecords.members(blood).getCompound(player.getStringUUID());
        long elapsed=Math.max(0,player.serverLevel().getGameTime()-member.getLong("downedAt"));
        return Math.max(0,EventRules.DOWNED_SECONDS.get()*TICKS_PER_SECOND-elapsed);
    }
    public static UUID session(ServerPlayer player) {
        if (player.isSpectator()) return null;
        var blood = EventSession.forPlayer(player);
        if (blood != null) return !blood.getBoolean("solo") && blood.getBoolean("combatStarted") ? blood.getUUID("id") : null;
        UUID hosted = HostedEncounters.participantSession(player);
        if (hosted != null) return hosted;
        var data = SmpData.get(player.server);
        for (var party : data.all("parties")) {
            UUID saved = persistedSession(party, player.getUUID(), player.serverLevel().dimension().location().toString());
            if (saved == null) continue;
            if (party.hasUUID("eventId")) {
                var event = data.find("events", party.getUUID("eventId"));
                if (event == null || !event.getString("state").equals("ACTIVE")) continue;
            }
            boolean loaded = false;
            for (var level : player.server.getAllLevels()) {
                if (level.getEntity(saved) != null) { loaded = true; break; }
            }
            if (!loaded) return saved;
        }
        return null;
    }
    public static void enforce(ServerPlayer player) {
        var blood=EventSession.forPlayer(player);
        if(blood==null) return;
        var member=SmpRecords.members(blood).getCompound(player.getStringUUID());
        if(!downed(player)) {
            if(member.contains("downedAt")) { member.remove("downedAt");SmpData.get(player.server).changed(blood); }
            return;
        }
        if(blood.getBoolean("solo")) {
            team.creative.playerrevive.server.PlayerReviveServer.kill(player);
            return;
        }
        if(!member.contains("downedAt")) {
            member.putLong("downedAt",player.serverLevel().getGameTime());
            SmpData.get(player.server).changed(blood);
            return;
        }
        if(player.serverLevel().getGameTime()-member.getLong("downedAt")>=EventRules.DOWNED_SECONDS.get()*TICKS_PER_SECOND)
            team.creative.playerrevive.server.PlayerReviveServer.kill(player);
    }
    static UUID persistedSession(CompoundTag party, UUID player, String dimension) {
        if (!party.getString("state").equals("ACTIVE") || !party.getString("activity").equals("BOSS")
                || party.getBoolean("solo") || party.getBoolean("bloodMoon")
                || (!party.hasUUID("bossEntity") && (!party.getBoolean("nativePending") || !party.hasUUID("id")))
                || !party.getString("dimension").equals(dimension) || !Parties.accepted(party, player)) return null;
        return SmpRecords.memberIds(party).stream().filter(id -> Parties.accepted(party, id)).limit(2).count() > 1
                ? party.getUUID(party.hasUUID("bossEntity") ? "bossEntity" : "id") : null;
    }
    public static boolean sameSession(ServerPlayer first, ServerPlayer second) {
        UUID session = session(first);
        return first != second && session != null && session.equals(session(second));
    }
    public static void revived(ServerPlayer helper) {
        var blood = EventSession.forPlayer(helper);
        if (blood != null && !blood.getBoolean("solo") && blood.getBoolean("combatStarted")) {
            var members = SmpRecords.members(blood);
            var member = members.getCompound(helper.getStringUUID());
            member.putBoolean("contributed", true);
            member.putBoolean("supported", true);
            member.putInt("revives", member.getInt("revives") + 1);
            members.put(helper.getStringUUID(), member);
            SmpData.get(helper.server).changed(blood);
        } else HostedEncounters.revived(helper);
    }
    public static void finish(MinecraftServer server, CompoundTag event) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("playerrevive")) return;
        for(UUID id:SmpRecords.memberIds(event)) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null && downed(player)) team.creative.playerrevive.server.PlayerReviveServer.kill(player);
        }
    }
    private ReviveSupport() {}
}
