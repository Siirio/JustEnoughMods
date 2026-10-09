package com.siirio.jemcompat.discovery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public final class CampaignDiscoveryEvents {
    private static final String CARTOGRAPHER_SELECTION = "jem_twelve_eyes_cartographer_maps";
    private static final String WANDERING_SELECTION = "jem_twelve_eyes_wandering_maps";
    private static final int MAP_TRADER_CHANCE = 75;
    private static final int PERCENT_SCALE = 100;
    private static final int MAPS_PER_LEVEL = 4;
    private static final int NOVICE_LEVEL = 1;
    private static final int APPRENTICE_LEVEL = 2;
    private static final int WANDERING_MIN_MAPS = 1;
    private static final int WANDERING_MAP_VARIANTS = 2;
    private static final int BASE_MAP_COST = 8;
    private static final int MAP_COST_PER_LEVEL = 2;

    private CampaignDiscoveryEvents() {
    }

    @SubscribeEvent
    public static void ensureMapTrades(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getTarget() instanceof Villager villager
                && villager.getVillagerData().getProfession() == VillagerProfession.CARTOGRAPHER) {
            refreshCartographerTrades(villager);
        } else if (event.getTarget() instanceof WanderingTrader trader) {
            ensureWanderingTrades(trader);
        }
    }

    public static void rerollCartographerTrades(Villager villager) {
        if (villager.getVillagerData().getProfession() != VillagerProfession.CARTOGRAPHER) {
            return;
        }
        villager.getPersistentData().remove(CARTOGRAPHER_SELECTION);
        refreshCartographerTrades(villager);
    }

    public static void refreshCartographerTrades(Villager villager) {
        if (villager.getVillagerData().getProfession() != VillagerProfession.CARTOGRAPHER) {
            return;
        }
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(CARTOGRAPHER_SELECTION, Tag.TAG_LIST)) {
            removeLocatorTrades(villager);
            List<CampaignTarget> selection = new ArrayList<>();
            if (villager.getRandom().nextInt(PERCENT_SCALE) < MAP_TRADER_CHANCE) {
                selection.addAll(select(villager.getRandom(), bossesAtLevel(NOVICE_LEVEL), MAPS_PER_LEVEL));
                selection.addAll(select(villager.getRandom(), bossesAtLevel(APPRENTICE_LEVEL), MAPS_PER_LEVEL));
            }
            saveSelection(data, CARTOGRAPHER_SELECTION, selection);
        }
        int villagerLevel = villager.getVillagerData().getLevel();
        List<CampaignTarget> available = loadSelection(data, CARTOGRAPHER_SELECTION).stream()
                .filter(target -> target.cartographerLevel() <= villagerLevel)
                .toList();
        retainSelectedLocatorTrades(villager, available);
        ensureSelectedTrades(villager, available);
    }

    private static void ensureWanderingTrades(WanderingTrader trader) {
        CompoundTag data = trader.getPersistentData();
        if (!data.contains(WANDERING_SELECTION, Tag.TAG_LIST)) {
            removeLocatorTrades(trader);
            int count = WANDERING_MIN_MAPS + trader.getRandom().nextInt(WANDERING_MAP_VARIANTS);
            saveSelection(data, WANDERING_SELECTION, select(trader.getRandom(), CampaignTarget.values(), count));
        }
        List<CampaignTarget> selection = loadSelection(data, WANDERING_SELECTION);
        retainSelectedLocatorTrades(trader, selection);
        ensureSelectedTrades(trader, selection);
    }

    private static List<CampaignTarget> bossesAtLevel(int level) {
        return CampaignTarget.values().stream().filter(target -> target.cartographerLevel() == level).toList();
    }

    private static List<CampaignTarget> select(RandomSource random, List<CampaignTarget> pool, int count) {
        List<CampaignTarget> remaining = new ArrayList<>(pool);
        List<CampaignTarget> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < count) {
            selected.add(remaining.remove(random.nextInt(remaining.size())));
        }
        return selected;
    }

    private static void saveSelection(CompoundTag data, String key, List<CampaignTarget> selection) {
        ListTag list = new ListTag();
        selection.forEach(target -> list.add(StringTag.valueOf(target.selectionKey())));
        data.put(key, list);
    }

    private static List<CampaignTarget> loadSelection(CompoundTag data, String key) {
        ListTag list = data.getList(key, Tag.TAG_STRING);
        List<CampaignTarget> selection = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            CampaignTarget.bySelectionKey(list.getString(index)).ifPresent(selection::add);
        }
        return selection;
    }

    private static void ensureSelectedTrades(AbstractVillager trader, List<CampaignTarget> selection) {
        for (CampaignTarget target : selection) {
            if (hasLocatorTrade(trader, target)) {
                continue;
            }
            MerchantOffer offer = new CampaignMapTrade(target, emeraldCost(target)).getOffer(trader, trader.getRandom());
            if (offer != null) {
                trader.getOffers().add(offer);
            }
        }
    }

    private static void retainSelectedLocatorTrades(AbstractVillager trader, List<CampaignTarget> selection) {
        trader.getOffers().removeIf(offer -> isLocator(offer.getResult())
                && selection.stream().noneMatch(target -> offer.getResult().is(CampaignItems.locator(target).get())));
    }

    private static void removeLocatorTrades(AbstractVillager trader) {
        trader.getOffers().removeIf(offer -> isLocator(offer.getResult()));
    }

    private static boolean isLocator(ItemStack stack) {
        return CampaignTarget.values().stream().map(target -> CampaignItems.locator(target).get()).anyMatch(stack::is);
    }

    private static boolean hasLocatorTrade(AbstractVillager trader, CampaignTarget target) {
        Item locator = CampaignItems.locator(target).get();
        return trader.getOffers().stream().anyMatch(offer -> offer.getResult().is(locator));
    }

    private static int emeraldCost(CampaignTarget target) {
        return BASE_MAP_COST + target.cartographerLevel() * MAP_COST_PER_LEVEL;
    }
}
