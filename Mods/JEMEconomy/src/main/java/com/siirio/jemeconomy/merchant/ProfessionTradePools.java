package com.siirio.jemeconomy.merchant;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** Original offers keep native ownership; separately saved extensions share its native restock lifecycle. */
public final class ProfessionTradePools {
    public record Entry(ResourceLocation profession, int level, ResourceLocation item, int count, int weight, int maxUses) { }
    private static final List<Entry> ENTRIES = load();
    private static final String KEY = "jem_economy_profession_choices_v1";
    private ProfessionTradePools() { }
    public static List<Entry> entries() { return ENTRIES; }

    public static boolean legacyMixed(AbstractVillager merchant, MerchantOffers nativeOffers) {
        if (merchant.getPersistentData().contains(KEY)) return true;
        // Legacy wandering traders had no choices marker. The exact old companion shape
        // plus its opposite direction is ambiguous with third-party offers: preserve it and fail closed off.
        for (MerchantOffer offer : nativeOffers) {
            if (offer.getXp() != 0 || offer.getPriceMultiplier() != 0.05F || !offer.getCostB().isEmpty()) continue;
            boolean sells = offer.getResult().is(Items.EMERALD);
            ItemStack money = sells ? offer.getResult() : offer.getBaseCostA();
            ItemStack goods = sells ? offer.getBaseCostA() : offer.getResult();
            if (money.is(Items.EMERALD) && money.getCount() == 1 && !goods.is(Items.EMERALD)
                    && hasDirection(nativeOffers, goods, !sells)) return true;
        }
        return false;
    }

    public static MerchantOffers project(AbstractVillager merchant, MerchantOffers nativeOffers) {
        var access = (NativeMerchantOffersAccess) merchant;
        boolean legacy = access.jem$legacyMixedOffers();
        if (!com.siirio.jemeconomy.api.EconomyApi.isEnabled(merchant.level().getServer()))
            return legacy ? new MerchantOffers() : nativeOffers;
        MerchantOffers view = new MerchantOffers();
        for (MerchantOffer offer : nativeOffers)
            if (!com.siirio.jemeconomy.policy.TradeRestrictions.offerProhibited(offer)) view.add(offer);
        for (MerchantOffer offer : access.jem$extensions())
            if (!com.siirio.jemeconomy.policy.TradeRestrictions.offerProhibited(offer)) view.add(offer);
        return view;
    }

    public static void extend(AbstractVillager merchant) {
        var offers = merchant.getOffers();
        var extensions = ((NativeMerchantOffersAccess) merchant).jem$extensions();
        if (merchant instanceof Villager villager) {
            var profession = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(villager.getVillagerData().getProfession());
            CompoundTag saved = merchant.getPersistentData().getCompound(KEY);
            for (int level = 2; level <= villager.getVillagerData().getLevel(); level++) {
                String selectionKey = profession + ":" + level;
                if (saved.contains(selectionKey)) continue;
                List<Entry> candidates = new ArrayList<>();
                for (Entry entry : ENTRIES) {
                    if (entry.profession().equals(profession) && entry.level() == level
                            && ForgeRegistries.ITEMS.containsKey(entry.item())
                            && !com.siirio.jemeconomy.policy.TradeRestrictions.npcProhibited(new ItemStack(ForgeRegistries.ITEMS.getValue(entry.item())))
                            && !hasItem(offers, entry.item())) candidates.add(entry);
                }
                if (candidates.isEmpty()) { saved.putString(selectionKey, "none"); continue; }
                int total = candidates.stream().mapToInt(Entry::weight).sum();
                int roll = merchant.getRandom().nextInt(total);
                Entry selected = candidates.get(0);
                for (Entry entry : candidates) { roll -= entry.weight(); if (roll < 0) { selected = entry; break; } }
                var item = ForgeRegistries.ITEMS.getValue(selected.item());
                ItemStack goods = new ItemStack(item, selected.count());
                // Refuse policy/resource disagreement instead of inserting an unusable or protected item.
                if (NativeItemPricing.policy(goods).isEmpty()) throw new IllegalStateException("Unpriced profession merchandise " + selected.item());
                var offer = new MerchantOffer(new ItemStack(Items.EMERALD), goods, selected.maxUses(), 5 * level, 0.05F);
                extensions.add(offer); offers.add(offer);
                saved.putString(selectionKey, selected.item().toString());
            }
            merchant.getPersistentData().put(KEY, saved);
        }
        // Every admitted native item has a counterpart, retaining the exact item count and NBT.
        // Vanilla serializes these MerchantOffers, uses and demand with the NPC itself.
        List<MerchantOffer> companions = new ArrayList<>();
        for (MerchantOffer nativeOffer : List.copyOf(offers)) {
            if (!NativeTradePricing.monetary(nativeOffer)) continue;
            boolean sells = nativeOffer.getResult().is(Items.EMERALD);
            ItemStack goods = sells ? nativeOffer.getBaseCostA() : nativeOffer.getResult();
            if (goods.is(Items.EMERALD) || NativeItemPricing.policy(goods).isEmpty()) continue;
            if (hasDirection(offers, goods, !sells) || hasDirection(companions, goods, !sells)) continue;
            companions.add(sells
                    ? new MerchantOffer(new ItemStack(Items.EMERALD), goods.copy(), nativeOffer.getMaxUses(), 0, 0.05F)
                    : new MerchantOffer(goods.copy(), new ItemStack(Items.EMERALD), nativeOffer.getMaxUses(), 0, 0.05F));
        }
        extensions.addAll(companions);
    }
    private static boolean hasItem(List<MerchantOffer> offers, ResourceLocation id) {
        return offers.stream().anyMatch(offer -> id.equals(ForgeRegistries.ITEMS.getKey(offer.getResult().getItem()))
                || id.equals(ForgeRegistries.ITEMS.getKey(offer.getBaseCostA().getItem())));
    }
    private static boolean hasDirection(List<MerchantOffer> offers, ItemStack goods, boolean sells) {
        return offers.stream().anyMatch(offer -> offer.getResult().is(Items.EMERALD) == sells
                && same(sells ? offer.getBaseCostA() : offer.getResult(), goods));
    }
    private static boolean same(ItemStack a, ItemStack b) {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b);
    }
    private static List<Entry> load() {
        List<Entry> result = new ArrayList<>();
        try (var stream = ProfessionTradePools.class.getResourceAsStream("/data/jem_economy/economy/profession_pools.json")) {
            if (stream == null) throw new IllegalStateException("Missing profession pools");
            for (var element : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                var row = element.getAsJsonObject();
                int level = row.get("level").getAsInt(), weight = row.get("weight").getAsInt();
                int count = row.get("count").getAsInt(), uses = row.get("max_uses").getAsInt();
                if (level < 2 || level > 5 || weight < 1 || weight > 1000 || count < 1 || count > 64 || uses < 1 || uses > 64)
                    throw new IllegalStateException("Invalid profession entry: " + row);
                result.add(new Entry(new ResourceLocation(row.get("profession").getAsString()), level,
                        new ResourceLocation(row.get("item").getAsString()), count, weight, uses));
            }
            return List.copyOf(result);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load profession pools", failure); }
    }
}
