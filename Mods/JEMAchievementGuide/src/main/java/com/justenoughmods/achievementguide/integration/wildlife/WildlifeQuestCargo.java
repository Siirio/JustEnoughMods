package com.justenoughmods.achievementguide.integration.wildlife;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
public final class WildlifeQuestCargo {
    public record Binding(TamableAnimal animal, Container inventory) {}
    public record Click(ServerPlayer actor, AbstractContainerMenu menu, Binding binding, List<ItemStack> container, List<ItemStack> held) {}
    private static final Map<AbstractContainerMenu, Binding> MENUS = new WeakHashMap<>();
    private WildlifeQuestCargo() {}
    public static void opened(TamableAnimal animal, ServerPlayer actor, Container inventory) {
        if (WildlifeQuestProof.real(actor) != null && animal.isOwnedBy(actor) && actor.containerMenu.slots.stream().anyMatch(slot -> slot.container == inventory)) MENUS.put(actor.containerMenu, new Binding(animal, inventory));
    }
    public static Click begin(ServerPlayer actor, AbstractContainerMenu menu) {
        Binding binding = MENUS.get(menu);
        if (binding == null || WildlifeQuestProof.real(actor) == null || !binding.animal().isAlive() || !binding.animal().isOwnedBy(actor) || actor.containerMenu != menu) return null;
        List<ItemStack> inventory = new ArrayList<>(); for (int i = 0; i < binding.inventory().getContainerSize(); i++) inventory.add(binding.inventory().getItem(i).copy());
        return new Click(actor, menu, binding, inventory, WildlifeQuestCrafts.held(actor, menu).stream().map(ItemStack::copy).toList());
    }
    private static int count(List<ItemStack> list, ItemStack variant) { return list.stream().filter(s -> ItemStack.isSameItemSameTags(s, variant)).mapToInt(ItemStack::getCount).sum(); }
    public static void end(Click run) {
        if (run == null || run.actor().containerMenu != run.menu() || !run.binding().animal().isOwnedBy(run.actor())) return;
        Container inventory = run.binding().inventory(); List<ItemStack> now = new ArrayList<>(); for (int i = 0; i < inventory.getContainerSize(); i++) now.add(inventory.getItem(i));
        List<ItemStack> held = WildlifeQuestCrafts.held(run.actor(), run.menu()); var physical = NativeFoodSources.get(run.actor().serverLevel()); List<ItemStack> processed = new ArrayList<>();
        boolean elephant = run.binding().animal() instanceof EntityElephant;
        String kind = elephant ? "wildlife_elephant_cargo" : "wildlife_kangaroo_cargo";
        for (ItemStack stack : now) {
            if (stack.isEmpty() || processed.stream().anyMatch(known -> ItemStack.isSameItemSameTags(known, stack))) continue;
            processed.add(stack.copyWithCount(1));
            int gain = count(now, stack) - count(run.container(), stack), loss = count(run.held(), stack) - count(held, stack);
            if (gain > 0 && gain == loss) {
                int left = gain;
                for (ItemStack target : now) if (ItemStack.isSameItemSameTags(stack, target) && left > 0) {
                    int unclaimed = target.getCount() - NativeFoodSources.claims(target).stream().mapToInt(NativeFoodSources.Claim::count).sum();
                    int amount = Math.min(left, Math.max(0, unclaimed));
                    if (amount > 0 && physical.certify(run.actor().serverLevel(), run.actor().getUUID(), kind, target, amount) != null) left -= amount;
                }
            }
        }
        if (!elephant) return;
        for (ItemStack stack : held) {
            if (stack.isEmpty()) continue; int gained = count(held, stack) - count(run.held(), stack), removed = count(run.container(), stack) - count(now, stack);
            if (gained > 0 && gained == removed && run.actor().getUUID().equals(physical.owner(stack, kind)) && physical.consume(run.actor().getUUID(), stack, kind, 1)) {
                WildlifeQuestProof.emit(run.actor(), "v3_fauna_cargo_and_equipment"); return;
            }
        }
    }
    public static boolean supplied(EntityKangaroo animal, ItemStack stack) {
        var actor = WildlifeQuestProof.owner(animal);
        return actor != null && actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(stack, "wildlife_kangaroo_cargo"));
    }
}
