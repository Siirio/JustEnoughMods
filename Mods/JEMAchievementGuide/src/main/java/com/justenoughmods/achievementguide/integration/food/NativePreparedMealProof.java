package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** Native recipe removal plus actual player receipt, followed by consumption of its physical portion. */
public final class NativePreparedMealProof {
    private static final Set<String> WILD = Set.of("alexscaves:deep_sea_sushi_roll", "alexscaves:vesper_stew", "alexsmobs:mosquito_repellent_stew");
    private static final Set<String> GAME = Set.of("cnc:hunters_stew", "cnc:pemmican", "cnc:cooked_turkey", "cnc:cooked_venison");
    private record Pending(ItemStack output, List<ItemStack> spent, String kind) {}
    public static final class Click {
        final ServerPlayer actor;
        final AbstractContainerMenu menu;
        final List<ItemStack> before;
        final List<Pending> pending = new ArrayList<>();
        final java.util.Map<ItemStack,ItemStack> moving = new java.util.IdentityHashMap<>();
        Click(ServerPlayer actor, AbstractContainerMenu menu) { this.actor = actor; this.menu = menu; before = held(actor, menu).stream().map(ItemStack::copy).toList(); }
    }
    private static final ThreadLocal<Click> CURRENT = new ThreadLocal<>();
    private NativePreparedMealProof() {}
    private static List<ItemStack> held(ServerPlayer actor, AbstractContainerMenu menu) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < Math.min(256, actor.getInventory().getContainerSize()); i++) stacks.add(actor.getInventory().getItem(i));
        stacks.add(menu.getCarried()); return stacks;
    }
    public static void begin(ServerPlayer actor, AbstractContainerMenu menu) {
        CURRENT.remove();
        if (RemainingFoodActors.real(actor) != null && actor.containerMenu == menu && menu.slots.size() <= 256) CURRENT.set(new Click(actor, menu));
    }
    public static void moving(AbstractContainerMenu menu,ItemStack source) {
        Click run=CURRENT.get();
        if(run==null || run.menu!=menu || source.isEmpty() || run.moving.size()>=64)return;
        if(menu.slots.stream().anyMatch(slot->slot instanceof net.minecraft.world.inventory.ResultSlot && slot.getItem()==source))run.moving.put(source,source.copy());
    }
    public static ItemStack actualCraftOutput(ServerPlayer actor,ItemStack output) {
        Click run=CURRENT.get();
        ItemStack before=run!=null && run.actor==actor?run.moving.remove(output):null;
        return output.isEmpty() && before!=null?before:output;
    }
    public static void crafted(ServerPlayer actor, Recipe<?> recipe, ItemStack output, List<ItemStack> removed) {
        Click run = CURRENT.get();
        if (run == null || run.actor != actor || RemainingFoodActors.real(actor)==null || recipe == null || actor.level().getRecipeManager().byKey(recipe.getId()).orElse(null)!=recipe || output.isEmpty() || removed.isEmpty() || run.pending.size() >= 64) return;
        ItemStack nativeResult = recipe.getResultItem(actor.level().registryAccess());
        if (nativeResult.isEmpty() || output.getItem() != nativeResult.getItem()) return;
        String id = RemainingFoodActors.item(output);
        String kind = WILD.contains(id) ? "wild_meal" : GAME.contains(id) ? "prepared_game"
                : id.equals("primal:apple_fritter") ? "apple_fritter"
                : id.startsWith("snowyspirit:gingerbread") && !id.endsWith("cookie") ? "ginger_part" : "";
        if (!kind.isEmpty()) run.pending.add(new Pending(output.copyWithCount(nativeResult.getCount()), List.copyOf(removed), kind));
    }
    public static void end(ServerPlayer actor, AbstractContainerMenu menu) {
        Click run = CURRENT.get(); CURRENT.remove();
        if (run == null || run.actor != actor || RemainingFoodActors.real(actor)==null || actor.containerMenu != menu || run.menu != menu) return;
        List<ItemStack> after = held(actor, menu);
        List<ItemStack> used = new ArrayList<>();
        for (Pending pending : run.pending) {
            int before = run.before.stream().filter(s -> ItemStack.isSameItemSameTags(s, pending.output)).mapToInt(ItemStack::getCount).sum();
            int now = after.stream().filter(s -> ItemStack.isSameItemSameTags(s, pending.output)).mapToInt(ItemStack::getCount).sum();
            int allocated = used.stream().filter(s -> ItemStack.isSameItemSameTags(s, pending.output)).mapToInt(ItemStack::getCount).sum();
            int actual = Math.min(pending.output.getCount(), Math.max(0, now - before - allocated));
            if (actual < pending.output.getCount()) continue;
            var sources = NativeFoodSources.get(actor.serverLevel());
            if (pending.kind.equals("prepared_game") && !consumeIngredient(sources, actor, pending.spent, "prepared_game")) continue;
            if (pending.kind.equals("ginger_part") && !consumeIngredient(sources, actor, pending.spent, "ginger")
                    && !consumeIngredient(sources, actor, pending.spent, "ginger_part")) continue;
            int left = actual;
            // Native outputs may merge into an existing stack. Only the actual gained portion is certified.
            for (ItemStack target : after) {
                if (!ItemStack.isSameItemSameTags(target, pending.output)) continue;
                int known = NativeFoodSources.claims(target).stream().mapToInt(NativeFoodSources.Claim::count).sum();
                int amount = Math.min(left, Math.max(0, target.getCount() - known));
                if (amount > 0) sources.certify(actor.serverLevel(), actor.getUUID(), pending.kind, target, amount);
                left -= amount;
                if (left == 0) break;
            }
            used.add(pending.output.copyWithCount(actual));
            if (pending.kind.equals("ginger_part") && left == 0) QuestActionTrigger.INSTANCE.emit(actor, "v3_era_ginger_house");
        }
    }
    private static boolean consumeIngredient(NativeFoodSources sources, ServerPlayer actor, List<ItemStack> removed, String kind) {
        for (ItemStack spent : removed) if (sources.consume(actor.getUUID(), spent, kind, 1)) return true;
        return false;
    }
    public static void eaten(ServerPlayer actor, ItemStack before, ItemStack actual, int initialCount) {
        if (RemainingFoodActors.real(actor) == null || initialCount - actual.getCount() != 1) return;
        String id = RemainingFoodActors.item(before);
        String kind = WILD.contains(id) ? "wild_meal" : GAME.contains(id) ? "prepared_game" : id.equals("primal:apple_fritter") ? "apple_fritter" : "";
        if (kind.isEmpty()) return;
        if (id.equals("alexsmobs:mosquito_repellent_stew")) {
            var repellent = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("alexsmobs", "mosquito_repellent"));
            if (repellent == null || !actor.hasEffect(repellent)) return;
        }
        if (!NativeFoodSources.get(actor.serverLevel()).consume(actor.getUUID(), NativeFoodSources.spent(before, 1), kind, 1)) return;
        QuestActionTrigger.INSTANCE.emit(actor, kind.equals("wild_meal") ? "v3_cave_wild_kitchen"
                : kind.equals("prepared_game") ? "v3_fauna_venison_table" : "v3_fauna_fritter_and_thorns");
    }
}
