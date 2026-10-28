package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import java.util.ArrayDeque;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraftforge.common.util.FakePlayer;

/** Native SmithingMenu take receipt for the installed Invulnerite equipment transforms. */
public final class SkarrierInvulneriteProof {
    private static final int LIMIT = 16;
    private static final Map<String, String> OUTPUTS = Map.ofEntries(
            Map.entry("sword", "invulnerite_sword"), Map.entry("pickaxe", "invulnerite_pickaxe"),
            Map.entry("axe", "invulnerite_axe"), Map.entry("shovel", "invulnerite_shovel"), Map.entry("hoe", "invulnerite_hoe"),
            Map.entry("helmet", "invulnerite_armor_helmet"), Map.entry("chestplate", "invulnerite_armor_chestplate"),
            Map.entry("leggings", "invulnerite_armor_leggings"), Map.entry("boots", "invulnerite_armor_boots"));
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Receipt> calls = new ArrayDeque<>(); int overflow; }
    private record Receipt(SmithingMenu menu, ServerPlayer actor, ItemStack template, ItemStack base, ItemStack addition, boolean valid) {}
    private SkarrierInvulneriteProof() {}
    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private static boolean real(Player player) {
        return player instanceof ServerPlayer actor && !(actor instanceof FakePlayer) && !actor.isSpectator()
                && !actor.getAbilities().instabuild && actor.serverLevel().getServer().isSameThread();
    }
    public static void begin(SmithingMenu menu, SmithingRecipe recipe, Player player, ItemStack actualResult) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.calls.size() >= LIMIT) { calls.overflow++; return; }
        ServerPlayer actor = player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        ItemStack template = menu.getSlot(0).getItem().copy(), base = menu.getSlot(1).getItem().copy(), addition = menu.getSlot(2).getItem().copy();
        ItemStack result = actualResult;
        if (result.isEmpty() && menu instanceof QuestResultSnapshot snapshot) result = snapshot.jem$questQuickResult();
        String suffix = id(base).startsWith("minecraft:netherite_") ? id(base).substring("minecraft:netherite_".length()) : "";
        String output = OUTPUTS.get(suffix);
        boolean valid = real(player) && player.containerMenu == menu && output != null && recipe instanceof SmithingTransformRecipe
                && recipe.getId().toString().equals("skarrier_mobs:invulnerite_" + suffix + "_recipe")
                && actor.level().getRecipeManager().byKey(recipe.getId()).orElse(null) == recipe
                && id(template).equals("skarrier_mobs:invulnerite_upgrade_template") && id(addition).equals("skarrier_mobs:invulnerite_chunk")
                && base.getCount() == 1 && !template.isEmpty() && !addition.isEmpty() && result.getCount() == 1
                && id(result).equals("skarrier_mobs:" + output);
        if (valid) {
            SimpleContainer inputs = new SimpleContainer(template.copy(), base.copy(), addition.copy());
            ItemStack expected = recipe.assemble(inputs, actor.level().registryAccess());
            valid = recipe.matches(inputs, actor.level()) && expected.getCount() == result.getCount() && ItemStack.isSameItemSameTags(expected, result);
        }
        calls.calls.push(new Receipt(menu, actor, template, base, addition, valid));
    }
    private static boolean debit(ItemStack before, ItemStack after) {
        return before.getCount() - after.getCount() == 1 && (after.isEmpty() || ItemStack.isSameItemSameTags(before, after));
    }
    public static void end(SmithingMenu menu, Player player) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Receipt receipt = calls.calls.isEmpty() ? null : calls.calls.pop();
        if (calls.calls.isEmpty()) CALLS.remove();
        if (receipt == null || !receipt.valid || receipt.menu != menu || receipt.actor != player || !real(player) || player.containerMenu != menu) return;
        if (debit(receipt.template, menu.getSlot(0).getItem()) && debit(receipt.base, menu.getSlot(1).getItem())
                && debit(receipt.addition, menu.getSlot(2).getItem())) QuestActionTrigger.INSTANCE.emit(receipt.actor, "v3_combat_elder_alloy");
    }
}
