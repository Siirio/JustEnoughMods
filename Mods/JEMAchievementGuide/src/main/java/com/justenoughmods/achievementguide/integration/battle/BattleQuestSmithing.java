package com.justenoughmods.achievementguide.integration.battle;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

/** Exact native armor output receipt, physical sidecars preserve native item NBT. */
public final class BattleQuestSmithing {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final Map<SmithingMenu, Preview> PREVIEWS = new IdentityHashMap<>();
    private static final class Calls { final ArrayDeque<Receipt> frames = new ArrayDeque<>(); int overflow; }
    private record Preview(ServerPlayer actor, UUID batch, ItemStack variant, String kind) {}
    private record Receipt(SmithingMenu menu, ServerPlayer actor, ItemStack[] inputs, Preview preview, boolean valid) {}
    private BattleQuestSmithing() {}
    private static String kind(ItemStack stack) {
        String id = BattleQuestProof.item(stack);
        if (java.util.Set.of("cataclysm:ignitium_helmet", "cataclysm:ignitium_chestplate", "cataclysm:ignitium_leggings", "cataclysm:ignitium_boots").contains(id)) return "ignitium";
        if (java.util.Set.of("cataclysm:cursium_helmet", "cataclysm:cursium_chestplate", "cataclysm:cursium_leggings", "cataclysm:cursium_boots").contains(id)) return "cursium";
        return "";
    }
    public static void beforeResult(SmithingMenu menu) { closed(menu); }
    public static void result(SmithingMenu menu, Player actor) {
        if (!BattleQuestProof.real(actor) || PREVIEWS.size() >= 256) return;
        ItemStack result = menu.getSlot(3).getItem(); String kind = kind(result);
        if (kind.isEmpty() || result.getCount() != 1) return;
        ServerPlayer player = (ServerPlayer)actor;
        // Fresh transformed output has another item identity than its base. Old base-sidecar claims are invalid here.
        NativeFoodSources.claims(result, java.util.List.of());
        ItemStack variant = result.copy();
        UUID batch = NativeFoodSources.get(player.serverLevel()).provisional(player.getUUID(), "battle_smithing_" + kind, result, 1);
        if (batch != null) PREVIEWS.put(menu, new Preview(player, batch, variant, kind));
    }
    public static void begin(SmithingMenu menu, SmithingRecipe recipe, Player actor, ItemStack taken) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.frames.size() >= BattleQuestProof.LIMIT) { calls.overflow++; return; }
        Preview preview = PREVIEWS.remove(menu);
        ItemStack result = taken.isEmpty() && menu instanceof QuestResultSnapshot quick ? quick.jem$questQuickResult() : taken;
        ItemStack[] inputs = {menu.getSlot(0).getItem().copy(), menu.getSlot(1).getItem().copy(), menu.getSlot(2).getItem().copy()};
        String kind = kind(result), path = BattleQuestProof.item(result).replace("cataclysm:", "");
        boolean valid = BattleQuestProof.real(actor) && actor.containerMenu == menu && preview != null && preview.actor == actor
                && preview.kind.equals(kind) && NativeFoodSources.claims(result).stream().anyMatch(c -> c.batch().equals(preview.batch) && c.count() == 1)
                && recipe instanceof SmithingTransformRecipe && recipe.getId().toString().equals("cataclysm:smithing/" + path)
                && actor.level().getRecipeManager().byKey(recipe.getId()).orElse(null) == recipe
                && BattleQuestProof.item(inputs[0]).equals("cataclysm:" + kind + "_upgrade_smithing_template")
                && BattleQuestProof.item(inputs[1]).equals("minecraft:netherite_" + path.substring(kind.length() + 1))
                && BattleQuestProof.item(inputs[2]).equals("cataclysm:" + kind + "_ingot") && result.getCount() == 1;
        if (valid) {
            SimpleContainer inventory = new SimpleContainer(inputs[0], inputs[1], inputs[2]);
            ItemStack expected = recipe.assemble(inventory, actor.level().registryAccess());
            valid = recipe.matches(inventory, actor.level()) && ItemStack.isSameItemSameTags(expected, result) && expected.getCount() == result.getCount();
        }
        calls.frames.push(new Receipt(menu, actor instanceof ServerPlayer player ? player : null, inputs, preview, valid));
    }
    public static void end(SmithingMenu menu, Player actor) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Receipt receipt = calls.frames.isEmpty() ? null : calls.frames.pop();
        if (calls.frames.isEmpty()) CALLS.remove();
        if (receipt == null || receipt.preview == null) return;
        boolean committed = receipt.valid && receipt.menu == menu && receipt.actor == actor && BattleQuestProof.real(actor);
        for (int i = 0; i < 3 && committed; i++) {
            ItemStack after = menu.getSlot(i).getItem(), before = receipt.inputs[i];
            committed = before.getCount() - after.getCount() == 1 && (after.isEmpty() || ItemStack.isSameItemSameTags(before, after));
        }
        NativeFoodSources.get(receipt.preview.actor.serverLevel()).activate(receipt.preview.batch, committed);
        if (committed) WorldBattleEquipmentOrigins.confirmed(receipt.actor, receipt.preview.batch,
                "battle_smithing_" + receipt.preview.kind, receipt.preview.variant);
    }
    public static boolean certified(ServerPlayer actor, ItemStack stack, String kind) {
        return WorldBattleEquipmentOrigins.has(actor, stack, "battle_smithing_" + kind);
    }
    public static void closed(SmithingMenu menu) {
        Preview preview = PREVIEWS.remove(menu);
        if (preview != null) NativeFoodSources.get(preview.actor.serverLevel()).activate(preview.batch, false);
    }
    public static void clear() { for (var menu : java.util.List.copyOf(PREVIEWS.keySet())) closed(menu); CALLS.remove(); }
}
