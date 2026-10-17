package com.siirio.jemsettlements.compat.recruits;

import com.siirio.jemsettlements.runtime.ResidentFoodService;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.runtime.TownMealService;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Native entities own morale, satiety and decay; explicit bell enrollment enables JEM serving policy. */
public final class RecruitNeedsService {
    private RecruitNeedsService() { }
    public static boolean managed(AbstractRecruitEntity recruit) {
        if (!(recruit.level() instanceof ServerLevel) || !SettlementAuthority.active(recruit.getServer())
                || !recruit.getIsOwned() || recruit.getOwnerUUID() == null) return false;
        var data = SettlementSavedData.get(recruit.getServer());
        return data.workforceEntity(recruit.getUUID()).filter(record -> record.type() == WorkforceType.RECRUIT
                && record.settlementActive() && record.needsEnrolled() && recruit.getOwnerUUID().equals(record.ownerId())
                && data.town(record.townId()).filter(town -> town.owner().equals(record.ownerId())).isPresent()).isPresent();
    }
    private static boolean allowedFood(AbstractRecruitEntity recruit, ItemStack stack) {
        return recruit.canEatItemStack(stack) || recruit.getHunger() < 10.0F
                && com.siirio.jemsettlements.happiness.FoodValueRegistry.classify(stack, recruit)
                        .filter(value -> value.mood() < 0).isPresent();
    }
    public static boolean criticalFoodAvailable(AbstractRecruitEntity recruit) {
        if (!managed(recruit) || recruit.getHunger() >= 10F || recruit.isSleeping() || recruit.isNoAi()
                || !com.talhanation.recruits.config.RecruitsServerConfig.RecruitsUpdateHungerAndMorale.get()) return false;
        var needs = SettlementSavedData.get(recruit.getServer()).workforceEntity(recruit.getUUID()).orElseThrow().foodHistory();
        needs.nativeNeeds(recruit.getMorale(), recruit.getHunger()); needs.refreshCraving();
        long now = recruit.getServer().overworld().getGameTime();
        var record = SettlementSavedData.get(recruit.getServer()).workforceEntity(recruit.getUUID()).orElseThrow();
        var town = SettlementSavedData.get(recruit.getServer()).town(record.townId()).orElseThrow();
        return foodAvailable(recruit, needs, town, now, true, null);
    }
    private static boolean foodAvailable(AbstractRecruitEntity recruit, com.siirio.jemsettlements.resident.ResidentMood needs,
                                         com.siirio.jemsettlements.town.TownRecord town, long now, boolean immediate,
                                         net.minecraft.core.BlockPos orderedChest) {
        if (orderedChest == null) for (int slot = 0; slot < recruit.getInventory().getContainerSize(); slot++)
            if (ResidentFoodService.eligible(recruit, needs, recruit.getInventory().getItem(slot), now, stack -> allowedFood(recruit, stack))) return true;
        ServerLevel level = (ServerLevel) recruit.level();
        if (orderedChest == null && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                && !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, recruit.getBoundingBox().inflate(3),
                        item -> item.isAlive() && item.getOwner() == null && !item.hasPickUpDelay()
                                && ResidentFoodService.eligible(recruit, needs, item.getItem(), now, stack -> allowedFood(recruit, stack))).isEmpty()) return true;
        int budget = 8192;
        if (town != null) for (var link : town.storage()) {
            if (budget <= 0) break;
            if (orderedChest != null && !link.contains(orderedChest)) continue;
            if (immediate && recruit.distanceToSqr(link.position().getX() + .5D, link.position().getY() + .5D, link.position().getZ() + .5D) > 6.25D) continue;
            var physical = com.siirio.jemsettlements.runtime.SharedStorageService.handler(level, link); if (physical.isEmpty()) continue;
            var handler = physical.get(); int checked = Math.min(budget, handler.getSlots()); budget -= checked;
            for (int slot = 0; slot < checked; slot++)
                if (ResidentFoodService.eligible(recruit, needs, handler.extractItem(slot, 1, true), now, stack -> allowedFood(recruit, stack))) return true;
        }
        return false;
    }
    public static boolean criticalMeal(AbstractRecruitEntity recruit) {
        var data = SettlementSavedData.get(recruit.getServer());
        var record = data.workforceEntity(recruit.getUUID()).orElse(null);
        return record != null && managed(recruit) && feedOne(data, record, recruit, recruit.getServer().overworld().getGameTime(), true);
    }
    public static boolean meal(AbstractRecruitEntity recruit) {
        if (!managed(recruit)) return false;
        var data = SettlementSavedData.get(recruit.getServer());
        return feedOne(data, data.workforceEntity(recruit.getUUID()).orElseThrow(), recruit, recruit.getServer().overworld().getGameTime(), false);
    }
    public static boolean hasMealTask(AbstractRecruitEntity recruit) {
        if (!managed(recruit) || recruit.isSleeping() || recruit.isNoAi()
                || !com.talhanation.recruits.config.RecruitsServerConfig.RecruitsUpdateHungerAndMorale.get()) return false;
        var data = SettlementSavedData.get(recruit.getServer()); var record = data.workforceEntity(recruit.getUUID()).orElseThrow();
        long now = recruit.getServer().overworld().getGameTime();
        var town = prepareMeal(data, record, recruit, now, false); var needs = record.foodHistory();
        var orderedChest = TownMealService.target((ServerLevel) recruit.level(), town, recruit, now);
        return needs.craving() && needs.satiety() < 100 && (orderedChest != null || recruit.getTarget() == null)
                && foodAvailable(recruit, needs, town, now, false, orderedChest);
    }
    private static com.siirio.jemsettlements.town.TownRecord prepareMeal(SettlementSavedData data, WorkforceRecord record,
                                                                 AbstractRecruitEntity recruit, long now, boolean critical) {
        var needs = record.foodHistory();
        needs.nativeNeeds(recruit.getMorale(), recruit.getHunger());
        var dishes = com.siirio.jemsettlements.happiness.AvailableDishService.moddedDishes(recruit.getServer());
        needs.retainRequestedDishes(dishes); needs.refreshCraving();
        var town = data.town(record.townId()).filter(value -> value.owner().equals(record.ownerId())
                && value.dimension().equals(recruit.level().dimension().location())).orElse(null);
        if (!critical) TownMealService.prepare((ServerLevel) recruit.level(), town, recruit, needs, now);
        if (needs.craving() && needs.requestedDish().isEmpty()) {
            var candidates = dishes.stream().filter(needs::canRequest).toList();
            if (!candidates.isEmpty()) needs.requestedDish(candidates.get(Math.floorMod(record.entityId().hashCode() * 31 + Long.hashCode(now / 24000), candidates.size())));
        }
        return town;
    }
    private static boolean feedOne(SettlementSavedData data, WorkforceRecord record, AbstractRecruitEntity recruit, long now, boolean critical) {
        var town = prepareMeal(data, record, recruit, now, critical); var needs = record.foodHistory();
        boolean ordered = !critical && TownMealService.target((ServerLevel) recruit.level(), town, recruit, now) != null;
        if (ordered) recruit.stopUsingItem();
        if (!needs.craving() || !critical && !ordered && (recruit.getTarget() != null || recruit.hurtTime > 0)
                || recruit.isSleeping() || recruit.isUsingItem() || recruit.isNoAi()) return false;
        java.util.function.Predicate<ItemStack> nativeRule = stack -> allowedFood(recruit, stack);
        var inventory = recruit.getInventory(); var food = new ArrayList<ResidentFoodService.Food>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!ResidentFoodService.eligible(recruit, needs, stack, now, nativeRule)) continue;
            int selected = slot;
            food.add(new ResidentFoodService.Food(stack.copyWithCount(1), () -> inventory.removeItem(selected, 1), null,
                    ResidentFoodService.priority(needs, stack, recruit), 0));
        }
        float morale = recruit.getMorale(), satiety = recruit.getHunger(); int previousMood = needs.mood();
        // A critical combat pause uses food already carried or within reach; it never drags combat AI across town.
        boolean fed = ResidentFoodService.feed((ServerLevel) recruit.level(), town, recruit, needs, now, nativeRule, food,
                stack -> { var properties = stack.getItem().getFoodProperties(stack, recruit); return properties == null ? 0 : (int) (properties.getNutrition() * 5 + properties.getSaturationModifier()); }, accepted -> {
            recruit.setMoral(Math.max(0, Math.min(100, morale + needs.mood() - previousMood)));
            var classification = com.siirio.jemsettlements.happiness.FoodValueRegistry.classify(accepted, recruit).orElseThrow();
            var properties = accepted.getItem().getFoodProperties(accepted, recruit);
            if (properties != null) recruit.heal(properties.getSaturationModifier());
            float gain = classification.mood() < 0 ? 1 : classification.satiety() != 0 ? classification.satiety()
                    : properties == null ? 0 : properties.getNutrition() * 5 + properties.getSaturationModifier();
            recruit.setHunger(Math.max(0, Math.min(100, satiety + gain))); needs.nativeNeeds(recruit.getMorale(), recruit.getHunger());
            ItemStack remainder = accepted.hasCraftingRemainingItem() ? accepted.getCraftingRemainingItem() : ItemStack.EMPTY;
            if (!remainder.isEmpty()) { ItemStack left = inventory.addItem(remainder); if (!left.isEmpty()) recruit.spawnAtLocation(left); }
        }, !critical);
        data.changed(); return fed;
    }
}
