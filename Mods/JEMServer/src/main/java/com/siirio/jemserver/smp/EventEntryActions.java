package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import net.minecraft.server.level.ServerPlayer;

public final class EventEntryActions {
    public static void choose(ServerPlayer player,EventChoice choice) {
        var data = SmpData.get(player.server);
        data.edit(() -> chooseOwned(player, choice));
    }

    private static void chooseOwned(ServerPlayer player,EventChoice choice) {
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
                    var party=SmpData.get(player.server).parties().currentRows().stream().filter(p->p.hasBossEntity() && p.bossEntity().equals(choice.id()) && p.owner().equals(player.getUUID())).findFirst().orElseThrow();
                    Parties.makeSolo(player,party);HostedParties.start(player,party);
                }
                return;
            }
            SmpRecords.require(EventRuntime.enabled(player.server),"unavailable");
            var row=SmpData.get(player.server).events().current(choice.id());
            SmpRecords.require(row!=null && row.activity().equals(choice.type().name()),"unavailable");
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
