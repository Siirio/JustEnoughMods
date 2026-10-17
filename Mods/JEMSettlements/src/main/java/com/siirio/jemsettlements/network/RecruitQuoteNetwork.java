package com.siirio.jemsettlements.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.network.PacketDistributor;

/** Loaded only by guarded Recruits calls; no optional types in the common channel. */
final class RecruitQuoteNetwork {
    static void sendNobleQuote(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        var noble = (com.talhanation.recruits.entities.VillagerNobleEntity) entity;
        CompoundTag view = new CompoundTag(); view.putInt("NobleId", noble.getId());
        view.putLong("Spendable", Math.addExact(com.siirio.jemeconomy.api.EconomyApi.balance(player), com.siirio.jemeconomy.api.EconomyApi.heldCash(player).value()));
        ListTag quotes = new ListTag();
        for (var trade : noble.getTrades()) {
            var rank = com.siirio.jemsettlements.payroll.RecruitRank.of(trade.resourceLocation);
            if (rank == com.siirio.jemsettlements.payroll.RecruitRank.UNSUPPORTED) continue;
            CompoundTag row = new CompoundTag(); row.putString("Id", trade.resourceLocation.toString());
            row.putLong("Cost", rank.hiringCost()); row.putLong("Wage", rank.wage()); quotes.add(row);
        }
        view.put("Quotes", quotes); SettlementNetwork.channel().send(PacketDistributor.PLAYER.with(() -> player), new NobleQuotePacket(view));
    }

    static void sendHireQuote(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        var recruit = (com.talhanation.recruits.entities.AbstractRecruitEntity) entity;
        long price = com.siirio.jemsettlements.compat.recruits.RecruitHiringService.quote(recruit);
        long balance = com.siirio.jemeconomy.api.EconomyApi.balance(player);
        boolean affordable = balance >= price || com.siirio.jemeconomy.api.EconomyApi.heldCash(player).value() >= price - balance;
        String team = player.getTeam() == null ? "" : player.getTeam().getName();
        boolean canHire = affordable && com.siirio.jemsettlements.compat.recruits.RecruitHiringService.mayHire(player, recruit)
                && com.talhanation.recruits.RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(team, player.getUUID());
        com.talhanation.recruits.Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.talhanation.recruits.network.MessageToClientUpdateHireState(canHire));
        SettlementNetwork.channel().send(PacketDistributor.PLAYER.with(() -> player), new HireQuotePacket(recruit.getId(),
                com.siirio.jemsettlements.compat.recruits.RecruitHiringService.quote(recruit),
                com.siirio.jemsettlements.payroll.RecruitRank.of(BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType())).wage()));
    }

}
