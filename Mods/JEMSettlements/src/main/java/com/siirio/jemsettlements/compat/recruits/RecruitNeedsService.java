package com.siirio.jemsettlements.compat.recruits;

import com.siirio.jemsettlements.runtime.ResidentFoodService;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Recruits retains native needs/decay/combat; JEM supplies only the shared food policy. */
public final class RecruitNeedsService {
    private RecruitNeedsService() { }
    public static boolean managed(AbstractRecruitEntity recruit) {
        if (!(recruit.level() instanceof ServerLevel) || !SettlementAuthority.active(recruit.getServer())
                || !recruit.getIsOwned() || recruit.getOwnerUUID() == null) return false;
        return SettlementSavedData.get(recruit.getServer()).workforceEntity(recruit.getUUID())
                .filter(record -> record.type() == WorkforceType.RECRUIT && record.settlementActive()).isPresent();
    }
    /** Explicit starvation exception applies only to bad edibles, never arbitrary blacklisted items. */
    private static boolean allowedFood(AbstractRecruitEntity recruit, ItemStack stack) {
        return recruit.canEatItemStack(stack) || recruit.getHunger() < 10.0F
                && com.siirio.jemsettlements.happiness.FoodValueRegistry.classify(stack, recruit)
                        .filter(value -> value.mood() < 0).isPresent();
    }
    public static void feed(SettlementSavedData data, long now) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || !com.talhanation.recruits.config.RecruitsServerConfig.RecruitsUpdateHungerAndMorale.get()) return;
        var dishes = com.siirio.jemsettlements.happiness.AvailableDishService.moddedDishes(server);
        for (WorkforceRecord record : data.workforce()) {
            if (record.type() != WorkforceType.RECRUIT || !record.settlementActive()) continue;
            AbstractRecruitEntity recruit = null;
            for (var level : server.getAllLevels()) if (level.getEntity(record.entityId()) instanceof AbstractRecruitEntity found) { recruit = found; break; }
            if (recruit == null || !recruit.isAlive() || !managed(recruit)) continue;
            var needs = record.foodHistory();
            needs.nativeNeeds(recruit.getMorale(), recruit.getHunger());
            needs.retainRequestedDishes(dishes); needs.refreshCraving();
            if (needs.craving() && needs.requestedDish().isEmpty()) {
                var candidates = dishes.stream().filter(needs::canRequest).toList();
                if (!candidates.isEmpty()) needs.requestedDish(candidates.get(Math.floorMod(record.entityId().hashCode() * 31 + Long.hashCode(now / 24000), candidates.size())));
            }
            if (!needs.craving() || recruit.getTarget() != null || recruit.hurtTime > 0 || recruit.isSleeping() || recruit.isUsingItem() || recruit.isNoAi()) continue;
            final AbstractRecruitEntity consumer = recruit;
            java.util.function.Predicate<ItemStack> nativeRule = stack -> allowedFood(consumer, stack);
            var inventory = recruit.getInventory();
            var food = new ArrayList<ResidentFoodService.Food>();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!ResidentFoodService.eligible(recruit, needs, stack, now, nativeRule)) continue;
                int selected = slot;
                food.add(new ResidentFoodService.Food(stack.copyWithCount(1), () -> inventory.removeItem(selected, 1), null,
                        ResidentFoodService.priority(needs, stack, recruit), 0));
            }
            var dimension = recruit.level().dimension().location();
            var town = data.town(record.townId()).filter(value -> value.owner().equals(record.ownerId())
                    && value.dimension().equals(dimension)).orElse(null);
            float morale = recruit.getMorale(), satiety = recruit.getHunger();
            int previousMood = needs.mood();
            ResidentFoodService.feed((ServerLevel) recruit.level(), town, recruit, needs, now, nativeRule, food, stack -> {
                var properties = stack.getItem().getFoodProperties(stack, consumer);
                return properties == null ? 0 : (int) (properties.getNutrition() * 5 + properties.getSaturationModifier());
            }, accepted -> {
                consumer.setMoral(Math.max(0, Math.min(100, morale + needs.mood() - previousMood)));
                var classification = com.siirio.jemsettlements.happiness.FoodValueRegistry.classify(accepted, consumer).orElseThrow();
                var properties = accepted.getItem().getFoodProperties(accepted, consumer);
                if (properties != null) consumer.heal(properties.getSaturationModifier());
                float gain = classification.mood() < 0 ? 1 : classification.satiety() != 0 ? classification.satiety()
                        : properties == null ? 0 : properties.getNutrition() * 5 + properties.getSaturationModifier();
                consumer.setHunger(Math.max(0, Math.min(100, satiety + gain)));
                needs.nativeNeeds(consumer.getMorale(), consumer.getHunger());
                // Keep native container remainders without duplicating the extracted serving.
                ItemStack remainder = accepted.hasCraftingRemainingItem() ? accepted.getCraftingRemainingItem() : ItemStack.EMPTY;
                if (!remainder.isEmpty()) { ItemStack left = inventory.addItem(remainder); if (!left.isEmpty()) consumer.spawnAtLocation(left); }
            });
        }
    }
}
