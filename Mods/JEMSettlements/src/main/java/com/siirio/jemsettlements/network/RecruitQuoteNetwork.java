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
        CompoundTag view = new CompoundTag(); view.putInt("NobleId", noble.getId()); view.putBoolean("CoinCurrency", com.siirio.jemsettlements.currency.SettlementCurrency.coins());
        view.putLong("Spendable", Math.addExact(com.siirio.jemsettlements.currency.SettlementCurrency.balance(player), com.siirio.jemsettlements.currency.SettlementCurrency.held(player)));
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
        long balance = com.siirio.jemsettlements.currency.SettlementCurrency.balance(player);
        boolean affordable = balance >= price || com.siirio.jemsettlements.currency.SettlementCurrency.held(player) >= price - balance;
        String team = player.getTeam() == null ? "" : player.getTeam().getName();
        boolean eligible=com.siirio.jemsettlements.compat.recruits.RecruitHiringService.mayHire(player,recruit);
        boolean capacity=com.talhanation.recruits.RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(team,player.getUUID());
        boolean canHire=affordable&&eligible&&capacity;
        String reason=recruit.getIsOwned()?"jem.settlements.hire.reason.owned":!eligible?"jem.settlements.hire.reason.unavailable":!capacity?"jem.settlements.hire.reason.cap":!affordable?"jem.settlements.hire.reason.funds":"";
        com.talhanation.recruits.Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.talhanation.recruits.network.MessageToClientUpdateHireState(canHire));
        SettlementNetwork.channel().send(PacketDistributor.PLAYER.with(() -> player), new HireQuotePacket(recruit.getId(),
                com.siirio.jemsettlements.compat.recruits.RecruitHiringService.quote(recruit),
                com.siirio.jemsettlements.payroll.RecruitRank.of(BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType())).wage(), com.siirio.jemsettlements.currency.SettlementCurrency.coins(), affordable, reason, new HireQuotePacket.Stats(recruit.getHealth(),recruit.getMaxHealth(),recruit.getXpLevel(),
                        recruit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),recruit.getKills(),
                        recruit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),recruit.getXp(),recruit.getArmorValue(),recruit.getMorale())));
    }

}
