package com.siirio.jemsettlements.runtime;

import com.siirio.jemsettlements.happiness.FoodValueRegistry;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** One food selection and extraction policy for residents and native recruit needs. */
public final class ResidentFoodService {
    private ResidentFoodService() { }
    public record Food(ItemStack stack, Supplier<ItemStack> extract, BlockPos storage, int priority, double distance,
                       net.minecraft.world.level.pathfinder.Path path) {
        public Food(ItemStack stack, Supplier<ItemStack> extract, BlockPos storage, int priority, double distance) {
            this(stack, extract, storage, priority, distance, null);
        }
    }
    public static boolean eligible(Mob mob, ResidentMood needs, ItemStack stack, long now, Predicate<ItemStack> nativeRule) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var value = FoodValueRegistry.classify(stack, mob);
        return !stack.isEmpty() && value.isPresent() && id != null && nativeRule.test(stack) && needs.canEat(now, id)
                && (value.get().mood() >= 0 || needs.satiety() < 10);
    }
    public static int priority(ResidentMood needs, ItemStack stack, Mob mob) {
        if (FoodValueRegistry.classify(stack, mob).orElseThrow().mood() < 0) return 2;
        return needs.requestedDish().filter(BuiltInRegistries.ITEM.getKey(stack.getItem())::equals).isPresent() ? 0 : 1;
    }
    public static boolean feed(ServerLevel level, TownRecord town, Mob mob, ResidentMood needs, long now,
                               Predicate<ItemStack> nativeRule, java.util.List<Food> inventory,
                               java.util.function.ToIntFunction<ItemStack> nativeNutrition,
                               java.util.function.Consumer<ItemStack> nativeAccepted) {
        return feed(level, town, mob, needs, now, nativeRule, inventory, nativeNutrition, nativeAccepted, true);
    }
    public static boolean feed(ServerLevel level, TownRecord town, Mob mob, ResidentMood needs, long now,
                               Predicate<ItemStack> nativeRule, java.util.List<Food> inventory,
                               java.util.function.ToIntFunction<ItemStack> nativeNutrition,
                               java.util.function.Consumer<ItemStack> nativeAccepted, boolean allowTravel) {
        BlockPos orderedChest = allowTravel ? TownMealService.target(level, town, mob, now) : null;
        var choices = new ArrayList<Food>(orderedChest == null ? inventory : java.util.List.of());
        if (orderedChest == null && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(3),
                    value -> value.isAlive() && value.getOwner() == null && !value.hasPickUpDelay())) {
                ItemStack stack = item.getItem();
                if (eligible(mob, needs, stack, now, nativeRule)) choices.add(new Food(stack.copyWithCount(1), () -> {
                    ItemStack taken = item.getItem().split(1); if (item.getItem().isEmpty()) item.discard(); return taken;
                }, null, priority(needs, stack, mob), mob.distanceToSqr(item)));
            }
        }
        int budget = 8192;
        if (town != null) for (var link : town.storage()) {
            if (budget <= 0) break;
            if (orderedChest != null && !link.contains(orderedChest)) continue;
            if (!link.dimension().equals(level.dimension().location()) || !level.hasChunkAt(link.position())) continue;
            var capability = SharedStorageService.handler(level, link); if (capability.isEmpty()) continue;
            double distance = mob.distanceToSqr(link.position().getX() + .5, link.position().getY() + .5, link.position().getZ() + .5);
            if (!allowTravel && distance > 6.25) continue;
            // Recruits returns a pending AsyncPath: canReach() is false until its worker finishes.
            // Retain that exact path and let native navigation complete it; extraction still requires physical reach.
            var path = distance > 6.25 ? mob.getNavigation().createPath(link.position(), 2) : null;
            if (distance > 6.25 && (path == null || mob instanceof Villager && !path.canReach())) continue;
            var handler = capability.get(); int checked = Math.min(budget, handler.getSlots()); budget -= checked;
            for (int slot = 0; slot < checked; slot++) {
                ItemStack simulated = handler.extractItem(slot, 1, true);
                if (!eligible(mob, needs, simulated, now, nativeRule)) continue;
                int selectedSlot = slot;
                choices.add(new Food(simulated, () -> handler.extractItem(selectedSlot, 1, false), link.position(), priority(needs, simulated, mob), distance, path));
            }
        }
        choices.sort(Comparator.comparingInt(Food::priority).thenComparingDouble(Food::distance));
        if (choices.isEmpty()) return false;
        Food first = choices.get(0);
        // Available desired food comes first across inventory, drops and marked storage.
        Food selected = choices.stream().filter(value -> value.storage() == null || value.distance() <= 6.25)
                .filter(value -> value.priority() == first.priority()).findFirst().orElse(first);
        if (selected.storage() != null && selected.distance() > 6.25) {
            if (mob instanceof Villager villager) {
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new net.minecraft.world.entity.ai.memory.WalkTarget(selected.storage(), .65F, 2));
                villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new net.minecraft.world.entity.ai.behavior.BlockPosTracker(selected.storage()));
            } else return mob.getNavigation().moveTo(selected.path(), .65);
            return true;
        }
        if (!eligible(mob, needs, selected.stack(), now, nativeRule)) return false;
        ItemStack taken = selected.extract().get();
        if (taken.isEmpty() || taken.getCount() != 1 || !ItemStack.isSameItemSameTags(taken, selected.stack())) return false;
        var value = FoodValueRegistry.classify(taken, mob).orElseThrow();
        int gain = value.mood() < 0 ? 1 : value.satiety() != 0 ? value.satiety() : Math.max(0, nativeNutrition.applyAsInt(taken));
        needs.acceptFood(BuiltInRegistries.ITEM.getKey(taken.getItem()), value.mood(), gain, now);
        nativeAccepted.accept(taken);
        if (selected.storage() != null) SharedStorageService.open(level, selected.storage(), now);
        mob.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, .8F, .9F + mob.getRandom().nextFloat() * .2F);
        mob.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
        level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, taken), mob.getX(), mob.getEyeY(), mob.getZ(), 8, .2, .2, .2, .05);
        if (town != null && selected.storage() != null && value.mood() >= 0 && mob.isAlive() && needs.living()) {
            var data = com.siirio.jemsettlements.town.SettlementSavedData.get(level.getServer());
            if (data.resident(mob.getUUID()).orElse(null) == needs && needs.townId().equals(town.id())) {
                data.changed();
                com.siirio.jemsettlements.api.SettlementOperationEvent.publishOwner(level.getServer(), town,
                        com.siirio.jemsettlements.api.SettlementOperationEvent.Kind.RESIDENT_MEAL);
            }
        }
        return true;
    }
    public static void preserveBreedingFood(Villager villager, ItemStack stack) {
        if (FoodValueRegistry.classify(stack, villager).orElseThrow().mood() > 0
                && (stack.is(Items.BREAD) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT))) villager.getInventory().addItem(stack.copy());
    }
}
