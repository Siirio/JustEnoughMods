package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import net.minecraft.server.level.ServerPlayer;

public final class EventEntryActions {
    public static void choose(ServerPlayer player,EventChoice choice) {
        if (!SmpEnvironment.active(player) || !SmpRequests.allow(player)) return;
        try {
            if(choice.type()==EventEntryKind.BOSS_STRUCTURE) { StructureStaging.choose(player,choice.id(),choice.mode().name(),choice.solo());return; }
            if(choice.type()==EventEntryKind.BOSS_FIGHT) {
                var entity=player.serverLevel().getEntity(choice.id());
                SmpRecords.require(entity instanceof net.minecraft.world.entity.LivingEntity,"unavailable");
                var boss=(net.minecraft.world.entity.LivingEntity)entity;
                SmpRecords.require(BossHostingPrompt.canOffer(player,boss),"unavailable");
                SmpRuntime.host(player,boss);
                if(choice.solo()) {
                    var party=SmpData.get(player.server).all("parties").stream().filter(p->p.hasUUID("bossEntity") && p.getUUID("bossEntity").equals(choice.id()) && p.getUUID("owner").equals(player.getUUID())).findFirst().orElseThrow();
                    Parties.makeSolo(player,party);HostedParties.start(player,party);
                }
                return;
            }
            var row=SmpData.get(player.server).find("events",choice.id());
            SmpRecords.require(row!=null && row.getString("activity").equals(choice.type().name()),"unavailable");
            SmpRecords.require(choice.type()==EventEntryKind.BLOOD_MOON || choice.type()==EventEntryKind.BOSS_RAID, "unavailable");
            EventParties.join(player,row,choice.solo());
        } catch(SmpActionFailure failure) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error."+failure.code()));
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Event entry failed: player={}, event={}, type={}, mode={}",
                    player.getUUID(), choice.id(), choice.type(), choice.mode(), failure);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error.unavailable"));
        }
    }

    private EventEntryActions() {}
}
