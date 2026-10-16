package com.siirio.jemeconomy.merchant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in audit projection only. Never called during normal merchant interactions. */
public final class NativeTradeCatalog {
    private NativeTradeCatalog() { }
    public static JsonArray snapshot(ServerLevel level) {
        JsonArray rows = new JsonArray();
        Villager villager = new Villager(EntityType.VILLAGER, level);
        for (var profession : VillagerTrades.TRADES.entrySet()) {
            String id = String.valueOf(ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession.getKey()));
            for (var tier : profession.getValue().int2ObjectEntrySet()) {
                villager.setVillagerData(villager.getVillagerData().setProfession(profession.getKey()).setLevel(tier.getIntKey()));
                var listings = tier.getValue();
                for (int i = 0; i < listings.length; i++) rows.add(row(id, tier.getIntKey(), i, listings[i], villager));
            }
        }
        WanderingTrader wandering = new WanderingTrader(EntityType.WANDERING_TRADER, level);
        for (var tier : VillagerTrades.WANDERING_TRADER_TRADES.int2ObjectEntrySet()) {
            for (int i = 0; i < tier.getValue().length; i++)
                rows.add(row("minecraft:wandering_trader", tier.getIntKey(), i, tier.getValue()[i], wandering));
        }
        appendWinemaker(rows, wandering);
        return rows;
    }
    private static void appendWinemaker(JsonArray rows, WanderingTrader dummy) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("vinery")) return;
        try {
            // Exact installed 1.4.41 public API, inspected from its own JAR; no private reflection or optional class linkage.
            Class<?> type = Class.forName("net.satisfy.vinery.core.entity.WanderingWinemakerEntity");
            Object value = type.getField("TRADES").get(null);
            if (!(value instanceof java.util.Map<?, ?> pools)) throw new IllegalStateException("Vinery TRADES contract changed");
            for (var tier : pools.entrySet()) {
                if (!(tier.getKey() instanceof Integer level) || !(tier.getValue() instanceof VillagerTrades.ItemListing[] listings))
                    throw new IllegalStateException("Vinery TRADES entry contract changed");
                for (int index = 0; index < listings.length; index++) {
                    JsonObject entry = row("vinery:wandering_winemaker", level, index, listings[index], dummy);
                    entry.addProperty("restock", "native WanderingWinemaker selects eight offers; no restock");
                    rows.add(entry);
                }
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Installed Vinery public merchant API unavailable during explicit audit", failure);
        }
    }
    private static JsonObject row(String profession, int level, int index, VillagerTrades.ItemListing listing, Entity npc) {
        JsonObject row = new JsonObject();
        row.addProperty("profession", profession); row.addProperty("level", level); row.addProperty("pool_index", index);
        String provider = listing.getClass().getName();
        row.addProperty("provider", provider); row.addProperty("selection", "native provider pool; sample is not exhaustive randomized NBT");
        // Exact installed getOffer bytecode audited: Forge 47.4.10, Alex's Mobs
        // 1.22.9, Primal 1.1.6 and Vinery 1.4.41 only construct stacks/offers.
        // Exact runtime classes exclude subclass overrides and arbitrary callbacks.
        boolean verifiedItemFactory = switch (provider) {
            case "net.minecraftforge.common.BasicItemListing",
                    "com.github.alexthe666.alexsmobs.misc.ItemsForEmeraldsTrade",
                    "com.github.alexthe666.alexsmobs.misc.EmeraldsForItemsTrade",
                    "org.primal.registry.Primal_VillagerCustomTrades$ItemsForEmeralds",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$ExilirBuyTrade",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$ExilirSellTrade",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$InkBuyTrade",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$InkSellTrade",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$PotionSellTrade",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$SimpleBuy",
                    "io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$SimpleSell",
                    "net.satisfy.vinery.core.util.VillagerUtil$SellItemFactory",
                    "net.satisfy.vinery.core.util.VillagerUtil$BuyForOneEmeraldFactory" -> true;
            default -> false;
        };
        // Every ItemListing lambda in these exact installed owners was audited:
        // Nature's Spirit 2.2.5 wandererTrades$1..27 and Farm & Charm 1.0.14
        // registerFarmerTrades$0..6 only read registries and construct stacks/offers.
        // Require the audited version because JVM lambda suffixes are not stable.
        boolean verifiedLambda = auditedLambda(provider, "net.hibiscus.naturespirit.NatureSpirit", "natures_spirit", "2.2.5-1.20.1")
                || auditedLambda(provider, "net.satisfy.farm_and_charm.core.registry.VillagerTradeRegistryHandler", "farm_and_charm", "1.0.14");
        // Treasure map providers query/generate structures. Other optional providers remain uninvoked.
        // Iron's RandomScroll mutates its provider templates; RandomCurio and ScrollPouch
        // execute world loot tables. SimpleTrade itself accepts arbitrary callbacks.
        if ((!provider.startsWith("net.minecraft.world.entity.npc.VillagerTrades$") && !verifiedItemFactory && !verifiedLambda)
                || provider.endsWith("$TreasureMapForEmeralds")) {
            row.addProperty("sample_status", "not invoked: world-dependent or optional provider"); return row;
        }
        try {
            MerchantOffer offer = listing.getOffer(npc, RandomSource.create(20261016L + index));
            if (offer == null) { row.addProperty("sample_status", "provider returned null for audit context"); return row; }
            row.addProperty("sample_status", "loaded provider sample");
            row.add("cost_a", stack(offer.getBaseCostA())); row.add("cost_b", stack(offer.getCostB())); row.add("result", stack(offer.getResult()));
            row.addProperty("max_uses", offer.getMaxUses()); row.addProperty("xp", offer.getXp());
            row.addProperty("restock", profession.equals("minecraft:wandering_trader") ? "native wandering trader; no restock" : "native Villager restock");
        } catch (RuntimeException failure) { row.addProperty("sample_status", "provider failed: " + failure.getClass().getSimpleName()); }
        return row;
    }
    private static boolean auditedLambda(String provider, String owner, String mod, String version) {
        return provider.startsWith(owner + "$$Lambda$") && net.minecraftforge.fml.ModList.get().getModContainerById(mod)
                .map(container -> version.equals(container.getModInfo().getVersion().toString())).orElse(false);
    }
    private static JsonObject stack(ItemStack stack) {
        JsonObject result = new JsonObject();
        result.addProperty("item", String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));
        result.addProperty("count", stack.getCount());
        result.addProperty("nbt", stack.hasTag() ? stack.getTag().toString() : "");
        var policy = NativeItemPricing.policy(stack);
        if (policy.isPresent()) {
            result.addProperty("buy_copper", policy.get().buyCopper()); result.addProperty("sell_copper", policy.get().sellCopper());
            result.addProperty("sell_units", NativeItemPricing.sellUnits(stack));
            result.addProperty("sell_copper_for_stack", NativeItemPricing.saleCopper(stack, stack.getCount(), 100));
            result.addProperty("maximum_buyback_for_stack", NativeItemPricing.saleCopper(stack, stack.getCount(), 150));
        }
        return result;
    }
}
