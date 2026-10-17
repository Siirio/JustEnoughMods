package com.siirio.jemsettlements.network;

import com.siirio.jemsettlements.happiness.FoodValueRegistry;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Server-only projections: physical stock and journal facts, never a second simulation. */
final class SettlementViewDetails {
    private static List<Item> foodItems;
    private SettlementViewDetails() { }
    static void invalidate() { foodItems = null; }

    static void foodCounts(CompoundTag row,com.siirio.jemsettlements.resident.ResidentMood history,long now){
        CompoundTag saved=history.save();ListTag counts=new ListTag();
        if(saved.getLong("ServingDay")==Math.floorDiv(now,24000L)){
            CompoundTag portions=saved.getCompound("ServingsToday");
            for(String id:portions.getAllKeys()){CompoundTag meal=new CompoundTag();meal.putString("Item",id);meal.putInt("Count",portions.getInt(id));counts.add(meal);}
        }
        row.put("FoodCounts",counts);
    }
    static void enrich(ServerPlayer player, TownRecord town, CompoundTag view) {
        var data = SettlementSavedData.get(player.getServer());
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension()));
        view.putLong("Wallet", com.siirio.jemsettlements.currency.SettlementCurrency.balance(player));
        view.putLong("PhysicalCoins", com.siirio.jemsettlements.currency.SettlementCurrency.held(player));
        view.putLong("Spendable", Math.addExact(view.getLong("Wallet"), view.getLong("PhysicalCoins")));
        view.putInt("UnhappyThreshold", 35); view.putInt("HungerThreshold", 35);
        view.putLong("GameTime", player.getServer().overworld().getGameTime());
        view.putString("Owner", town.owner().toString()); view.putBoolean("SameDimension", player.serverLevel().dimension().location().equals(town.dimension()));
        if (view.getBoolean("SameDimension")) view.putLong("PlayerPosition", player.blockPosition().asLong());
        ListTag residents = view.getList("Residents", Tag.TAG_COMPOUND);
        for (int i = 0; i < residents.size(); i++) {
            CompoundTag row = residents.getCompound(i); if (!row.hasUUID("Id")) continue;
            data.resident(row.getUUID("Id")).ifPresent(resident -> {
                CompoundTag saved = resident.save();
                row.put("ServingsToday", saved.getCompound("ServingsToday").copy()); row.putLong("ServingDay", saved.getLong("ServingDay"));
                row.put("RecentFoods", saved.getList("RecentFoods", Tag.TAG_COMPOUND).copy());
                foodCounts(row,resident,player.getServer().overworld().getGameTime());
                row.putBoolean("Hostile", resident.mood() < 10);
                resident.home().ifPresent(home -> { row.putLong("HomePos", home.pos().asLong()); row.putString("HomeDimension", home.dimension().location().toString()); });
                resident.workplace().ifPresent(work -> { row.putLong("WorkPos", work.pos().asLong()); row.putString("WorkDimension", work.dimension().location().toString()); });
            });
            var entity = level == null ? null : level.getEntity(row.getUUID("Id"));
            row.putBoolean("Loaded", entity != null && entity.isAlive());
            if (entity != null && entity.isAlive()) { row.putLong("Position", entity.blockPosition().asLong()); row.putInt("EntityId", entity.getId()); }
        }
        for (String list : List.of("WorkforceRows", "Defense")) {
            ListTag workers = view.getList(list, Tag.TAG_COMPOUND);
            for (int i = 0; i < workers.size(); i++) {
                CompoundTag row = workers.getCompound(i); if (!row.hasUUID("Id")) continue;
                for (ServerLevel candidate : player.getServer().getAllLevels()) {
                    var entity = candidate.getEntity(row.getUUID("Id"));
                    if (entity == null || !entity.isAlive()) continue;
                    row.putLong("Position", entity.blockPosition().asLong()); row.putString("Dimension", candidate.dimension().location().toString());
                    row.putInt("EntityId", entity.getId()); row.putBoolean("Loaded", true);
                    if (entity instanceof LivingEntity living) { row.putFloat("Health", living.getHealth()); row.putFloat("MaxHealth", living.getMaxHealth()); }
                    break;
                }
            }
        }
        ListTag storage = new ListTag();
        for (var link : town.storage()) {
            CompoundTag row = new CompoundTag(); row.putLong("Position", link.position().asLong()); row.putString("Dimension", link.dimension().toString());
            row.putInt("Number",link.number()); if(link.second()!=null)row.putLong("Second",link.second().asLong()); row.putBoolean("FeedResidents", true); row.putBoolean("ProductionInput", link.productionInput()); row.putBoolean("ProductionOutput", link.productionOutput());
            ServerLevel storageLevel = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, link.dimension()));
            row.putBoolean("Loaded", storageLevel != null && storageLevel.hasChunkAt(link.position()) && storageLevel.getBlockEntity(link.position()) != null);
            storage.add(row);
        }
        view.put("Storage", storage);
        ListTag transactions = new ListTag();
        if (com.siirio.jemsettlements.currency.SettlementCurrency.coins()) for (var receipt : com.siirio.jemeconomy.ledger.EconomySavedData.get(player.getServer()).receiptHistory(com.siirio.jemeconomy.api.EconomyApi.townAccount(town.id()), 16)) {
            CompoundTag row = new CompoundTag(); row.putUUID("Receipt", receipt.receipt()); row.putUUID("Source", receipt.source()); row.putUUID("Destination", receipt.destination());
            row.putString("Kind", receipt.destination().equals(com.siirio.jemeconomy.api.EconomyApi.townAccount(town.id())) ? "incoming" : "outgoing");
            row.putLong("Amount", receipt.amount()); row.putBoolean("Incoming", receipt.destination().equals(com.siirio.jemeconomy.api.EconomyApi.townAccount(town.id()))); transactions.add(row);
        }
        view.put("Transactions", com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? transactions : com.siirio.jemsettlements.currency.SettlementCurrency.emeraldHistory(player.getServer(), town.id()));
        ListTag territories = new ListTag();
        for (TownRecord other : data.towns().stream().filter(t -> t.dimension().equals(town.dimension())).limit(256).toList()) {
            CompoundTag row = new CompoundTag(); row.putUUID("Id", other.id()); row.putString("Name", other.name()); row.putString("Dimension", other.dimension().toString());
            row.putInt("MinX", other.territoryMinX()); row.putInt("MaxX", other.territoryMaxX()); row.putInt("MinZ", other.territoryMinZ()); row.putInt("MaxZ", other.territoryMaxZ()); territories.add(row);
        }
        view.put("Territories", territories); view.put("PublicTerritories", territories.copy());
    }

    /** At most one owner-authorized page; no chunk tickets, extraction or menu clicks. */
    static CompoundTag storagePage(ServerPlayer player, TownRecord town, BlockPos requested, int requestedOffset) {
        CompoundTag page = new CompoundTag();
        if (com.siirio.jemsettlements.api.SettlementApi.ownedTown(player, town.id()).isEmpty()) return page;
        page.putLong("Position", requested.asLong());
        page.putString("Dimension", town.dimension().toString()); page.put("Slots", new ListTag());
        var link = town.storage().stream().filter(value -> value.dimension().equals(town.dimension()) && value.contains(requested)).findFirst().orElse(null);
        if (link == null) return page;
        page.putLong("Position", link.position().asLong()); page.putInt("Number", link.number());
        page.putInt("MealOrders", com.siirio.jemsettlements.runtime.TownMealService.count(town.id(), link.position(), player.getServer().overworld().getGameTime()));
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, link.dimension()));
        page.putBoolean("Loaded", level != null && level.hasChunkAt(link.position()) && (link.second() == null || level.hasChunkAt(link.second())));
        var physical = com.siirio.jemsettlements.runtime.SharedStorageService.handler(level, link);
        if (physical.isEmpty()) return page;
        var handler = physical.get(); int total = handler.getSlots();
        int offset = Math.min(Math.max(0, requestedOffset), Math.max(0, total - 1));
        page.putBoolean("InventoryKnown", true); page.putInt("SlotsTotal", total); page.putInt("SlotOffset", offset);
        ListTag slots = new ListTag();
        for (int slot = offset; slot < total && slot < offset + 54; slot++) {
            ItemStack stack = handler.getStackInSlot(slot); CompoundTag row = new CompoundTag(); row.putInt("Slot", slot);
            CompoundTag saved = stack.save(new CompoundTag());
            if (saved.sizeInBytes() > 8192) {
                row.putBoolean("NbtOmitted", true); page.putBoolean("InventoryTruncated", true);
                saved = new ItemStack(stack.getItem(), stack.getCount()).save(new CompoundTag());
            }
            row.put("Stack", saved); slots.add(row);
        }
        page.put("Slots", slots); return page;
    }

    static void foodPage(ServerPlayer player, TownRecord town, CompoundTag view, int requestedOffset) {
        if (foodItems == null) foodItems = BuiltInRegistries.ITEM.stream().filter(item -> {
            try { return item.getFoodProperties(new ItemStack(item), null) != null; } catch (RuntimeException contextual) { return false; }
        }).sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString())).toList();
        var residents = SettlementSavedData.get(player.getServer()).residents(town.id());
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension()));
        LivingEntity consumer = null;
        for (var resident : residents) { var entity = level == null ? null : level.getEntity(resident.villagerId()); if (entity instanceof net.minecraft.world.entity.npc.Villager villager && villager.isAlive()) { consumer = villager; break; } }
        Map<String, Long> counts = new HashMap<>(); boolean known = true; int budget = 8192;
        for (var link : town.storage()) {
            ServerLevel storageLevel = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, link.dimension()));
            if (storageLevel == null || !storageLevel.hasChunkAt(link.position())) { known = false; continue; }
            var handler = com.siirio.jemsettlements.runtime.SharedStorageService.handler(storageLevel, link);
            if (handler.isEmpty()) { known = false; continue; }
            int slots = Math.min(budget, handler.get().getSlots()); if (slots < handler.get().getSlots()) known = false; budget -= slots;
            for (int slot = 0; slot < slots; slot++) { ItemStack stack = handler.get().getStackInSlot(slot); if (!stack.isEmpty()) counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), (long) stack.getCount(), Long::sum); }
        }
        int offset = Math.min(Math.max(0, requestedOffset), Math.max(0, ((foodItems.size() - 1) / 64) * 64));
        ListTag rows = new ListTag(); var requestable = com.siirio.jemsettlements.happiness.AvailableDishService.moddedDishes(player.getServer());
        for (Item item : foodItems.stream().skip(offset).limit(64).toList()) {
            ItemStack stack = new ItemStack(item); String id = BuiltInRegistries.ITEM.getKey(item).toString();
            CompoundTag row = new CompoundTag(); row.putString("Item", id); row.putLong("Count", counts.getOrDefault(id, 0L)); row.putBoolean("StockKnown", known);
            var classified = FoodValueRegistry.classify(stack, consumer); row.putBoolean("Classified", classified.isPresent());
            if (classified.isPresent()) {
                var value = classified.get(); row.putInt("Mood", value.mood());
                var nativeFood = item.getFoodProperties(stack, consumer);
                row.putInt("Satiety", value.satiety() != 0 ? value.satiety() : nativeFood == null ? 0 : Math.max(0, nativeFood.getNutrition()) * 4);
                row.putString("Classification", value.classification());
            }
            row.putBoolean("Requestable", requestable.contains(BuiltInRegistries.ITEM.getKey(item)));
            int demands = 0; for (var resident : residents) if (resident.requestedDish().map(dish -> dish.toString().equals(id)).orElse(false)) demands++;
            row.putInt("Demand", demands); row.putBoolean("Requested", demands > 0); rows.add(row);
        }
        view.put("FoodCatalogue", rows); view.putInt("FoodOffset", offset); view.putInt("FoodTotal", foodItems.size()); view.putBoolean("StockComplete", known);
    }
}
