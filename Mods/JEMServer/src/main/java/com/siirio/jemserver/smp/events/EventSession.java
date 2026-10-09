package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.*;

public record EventSession(CompoundTag data) {
    public static final String SESSION = "jem:event_session_id";
    public static final String SPAWNED = "jem:event_spawned";
    public static final String TYPE = "jem:event_type";
    public UUID id() { return data.getUUID("id"); }
    public boolean accepted(UUID id) {
        if (data.contains("encounterParticipants", net.minecraft.nbt.Tag.TAG_LIST))
            return new EncounterContext(data).isParticipant(id) && !SmpRecords.members(data).getCompound(id.toString()).getBoolean("eliminated");
        return legacyAccepted(id);
    }
    boolean legacyAccepted(UUID id) {
        var member=SmpRecords.members(data).getCompound(id.toString());
        return member.getBoolean("accepted")&&!member.getBoolean("eliminated");
    }
    public List<ServerPlayer> active(MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return List.of();
        return server.getPlayerList().getPlayers().stream().filter(p -> accepted(p.getUUID()) && p.isAlive() && !p.isSpectator() && EventRegions.contains(data,p.serverLevel(),p.blockPosition())).toList();
    }
    public void accept(ServerPlayer player) {
        var member=SmpRecords.members(data).getCompound(player.getStringUUID());
        member.putBoolean("accepted",true);
        member.putBoolean("eliminated",false);
        member.putString("name",player.getGameProfile().getName());
        SmpRecords.members(data).put(player.getStringUUID(),member);
        SmpData.get(player.server).changed(data);
    }
    public void mark(Entity entity) {
        var tag=entity.getPersistentData();
        tag.putUUID(SESSION,id()); tag.putBoolean(SPAWNED,true);
        tag.putString(TYPE,data.getString("activity").toLowerCase(Locale.ROOT));
    }
    public static Entity owner(Entity entity) {
        return com.siirio.jemworldbosstiers.api.HostedEncounterApi.damageOwner(entity);
    }

    public static CompoundTag forPlayer(ServerPlayer player) {
        if (!SmpEnvironment.active(player)) return null;
        for(var row:SmpData.get(player.server).all("events"))
            if(row.getString("activity").equals("BLOOD_MOON") && row.getString("state").equals("ACTIVE") && new EventSession(row).accepted(player.getUUID())) return row;
        return null;
    }
}
