package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public final class WildlifeQuestCrafts {
    public static final Map<String, String> TOOLS = Map.of("alexsmobs:shield_of_the_deep", "wildlife_shield",
            "alexsmobs:squid_grapple", "wildlife_grapple", "alexsmobs:flying_fish_boots", "wildlife_boots",
            "alexsmobs:fish_oil", "wildlife_oil", "alexsmobs:straddleboard", "wildlife_board",
            "alexsmobs:blood_sprayer", "wildlife_blood_charge", "alexsmobs:hemolymph_blaster", "wildlife_hemolymph_charge");
    public record Pending(Recipe<?> recipe, ItemStack output, List<ItemStack> removed) {}
    public static final class Click {
        public final ServerPlayer actor; public final AbstractContainerMenu menu; public final List<ItemStack> before;
        public final List<Pending> pending = new ArrayList<>();
        public Click(ServerPlayer actor, AbstractContainerMenu menu) {
            this.actor = actor; this.menu = menu; before = held(actor, menu).stream().map(ItemStack::copy).toList();
        }
    }
    private static final ThreadLocal<Click> CURRENT = new ThreadLocal<>();
    private WildlifeQuestCrafts() {}
    public static List<ItemStack> held(ServerPlayer actor, AbstractContainerMenu menu) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < actor.getInventory().getContainerSize(); i++) result.add(actor.getInventory().getItem(i));
        result.add(menu.getCarried()); return result;
    }
    public static void begin(ServerPlayer actor, AbstractContainerMenu menu) {
        CURRENT.remove();
        if (WildlifeQuestProof.real(actor) != null && actor.containerMenu == menu && menu.slots.size() <= 256) CURRENT.set(new Click(actor, menu));
    }
    public static void crafted(ServerPlayer actor, Recipe<?> recipe, ItemStack output, List<ItemStack> removed) {
        Click run = CURRENT.get();
        if (run == null || run.actor != actor || recipe == null || removed.isEmpty() || run.pending.size() >= 64) return;
        String id = recipe.getId().toString();
        if (!id.startsWith("alexsmobs:")) return;
        ItemStack nativeResult = recipe.getResultItem(actor.level().registryAccess());
        if (id.equals("alexsmobs:mimicream_repair_recipe")) nativeResult = copiedOutput(removed);
        if (!nativeResult.isEmpty() && (output.isEmpty() || output.getItem() == nativeResult.getItem())) {
            ItemStack expected = output.isEmpty() ? nativeResult.copy() : output.copyWithCount(nativeResult.getCount());
            run.pending.add(new Pending(recipe, expected, removed.stream().map(ItemStack::copy).toList()));
        }
    }
    public static void end(ServerPlayer actor, AbstractContainerMenu menu) {
        Click run = CURRENT.get(); CURRENT.remove();
        if (run == null || run.actor != actor || run.menu != menu || actor.containerMenu != menu) return;
        List<ItemStack> after = held(actor, menu); List<ItemStack> allocated = new ArrayList<>();
        for (Pending pending : run.pending) {
            ItemStack output = pending.output();
            int before = count(run.before, output), now = count(after, output), used = count(allocated, output);
            if (now - before - used < output.getCount()) continue;
            String recipe = pending.recipe().getId().toString(); String item = WildlifeQuestProof.item(output); String kind = "";
            if (recipe.equals("alexsmobs:shield_of_the_deep") && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_tooth", 3)) kind = "wildlife_shield";
            else if (recipe.equals("alexsmobs:squid_grapple") && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_tentacle", 3)) kind = "wildlife_grapple";
            else if (recipe.equals("alexsmobs:flying_fish_boots") && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_flying_fish", 2)) kind = "wildlife_boots";
            else if (recipe.equals("alexsmobs:fish_oil") && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_captured_blobfish", 4)) kind = "wildlife_oil";
            else if (recipe.equals("alexsmobs:straddleboard")) kind = "wildlife_board";
            else if (recipe.equals("alexsmobs:rainbow_glass") && item.equals("alexsmobs:rainbow_glass")) WildlifeQuestProof.emit(actor, "v3_fauna_mutable_materials");
            else if (recipe.equals("alexsmobs:mimicream_repair_recipe") && copied(pending)) WildlifeQuestProof.emit(actor, "v3_fauna_mutable_materials");
            else if (recipe.equals("alexsmobs:tarantula_hawk_wing") && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_hawk_fragment", 9)) kind = "wildlife_hawk_wing";
            else if (recipe.equals("alexsmobs:tarantula_hawk_elytra") && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_hawk_wing", 2)) WildlifeQuestProof.emit(actor, "v3_fauna_insect_nursery");
            else if ((recipe.equals("alexsmobs:void_worm_beak") || recipe.equals("alexsmobs:dimensional_carver"))
                    && WildlifeQuestSources.consume(actor, pending.removed(), "wildlife_worm_material", recipe.endsWith("beak") ? 2 : 3)) WildlifeQuestProof.emit(actor, "v3_fauna_worm_aftermath");
            if (!kind.isEmpty()) certify(actor, after, output, kind);
            allocated.add(output);
        }
    }
    private static int count(List<ItemStack> stacks, ItemStack variant) { return stacks.stream().filter(s -> ItemStack.isSameItemSameTags(s, variant)).mapToInt(ItemStack::getCount).sum(); }
    private static void certify(ServerPlayer actor, List<ItemStack> held, ItemStack output, String kind) {
        int left = output.getCount(); var sources = NativeFoodSources.get(actor.serverLevel());
        for (ItemStack target : held) {
            if (!ItemStack.isSameItemSameTags(target, output)) continue;
            int known = NativeFoodSources.claims(target).stream().mapToInt(NativeFoodSources.Claim::count).sum();
            int amount = Math.min(left, Math.max(0, target.getCount() - known));
            if (amount > 0) {
                var batch = sources.certify(actor.serverLevel(), actor.getUUID(), kind, target, amount);
                if (batch != null) { if (TOOLS.containsKey(WildlifeQuestProof.item(output))) WildlifeQuestTools.get(actor.serverLevel()).record(batch, batch, target); left -= amount; }
            }
            if (left == 0) break;
        }
    }
    private static boolean copied(Pending pending) {
        ItemStack expected = copiedOutput(pending.removed());
        return !expected.isEmpty() && ItemStack.isSameItemSameTags(expected, pending.output());
    }
    private static ItemStack copiedOutput(List<ItemStack> removed) {
        ItemStack original = removed.stream().filter(ItemStack::isDamageableItem).findFirst().orElse(ItemStack.EMPTY);
        if (original.isEmpty() || removed.stream().filter(s -> WildlifeQuestProof.item(s).equals("alexsmobs:mimicream")).mapToInt(ItemStack::getCount).sum() < 8) return ItemStack.EMPTY;
        ItemStack expected = original.copyWithCount(1);
        if (WildlifeQuestProof.item(expected).equals("alexsmobs:ghostly_pickaxe")) expected.removeTagKey("Items");
        ListTag enchantments = expected.getOrCreateTag().getList("Enchantments", 10); ListTag retained = new ListTag();
        for (int i = 0; i < enchantments.size(); i++) if (!enchantments.getCompound(i).getString("id").equals("minecraft:mending")) retained.add(enchantments.getCompound(i).copy());
        expected.getOrCreateTag().put("Enchantments", retained); expected.setDamageValue(expected.getMaxDamage());
        return expected;
    }
}
