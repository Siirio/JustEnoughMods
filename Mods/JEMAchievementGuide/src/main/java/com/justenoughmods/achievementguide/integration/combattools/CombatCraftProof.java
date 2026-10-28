package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

/** Real result take and original matrix debits, native recipes remain the only crafting authority. */
public final class CombatCraftProof {
    public static final String MACE = "JemQuestCraftedMaceV4";
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Quick> quick = new ArrayDeque<>(); final ArrayDeque<Run> runs = new ArrayDeque<>(); int quickOverflow, overflow; }
    private record Quick(Player actor, AbstractContainerMenu menu, Recipe<?> recipe, ItemStack result) {}
    private static final class Run {
        final ServerPlayer actor; final CraftingContainer source; final CraftingRecipe recipe; final ItemStack result; final Grid grid;
        final int[] removed = new int[9]; boolean valid;
        Run(ServerPlayer actor, CraftingContainer source, CraftingRecipe recipe, ItemStack result) {
            this.actor = actor; this.source = source; this.recipe = recipe; this.result = result;
            List<ItemStack> cells = new ArrayList<>();
            for (int slot = 0; slot < Math.min(9, source.getContainerSize()); slot++) cells.add(source.getItem(slot).copy());
            grid = new Grid(cells, source.getWidth(), source.getHeight());
            if (actor != null && recipe != null && source.getContainerSize() <= 9 && recipe.matches(grid, actor.level())) {
                ItemStack expected = recipe.assemble(grid, actor.level().registryAccess()).copy(), actual = result.copy();
                strip(expected); strip(actual);
                valid = expected.getCount() == actual.getCount() && !actual.isEmpty() && ItemStack.isSameItemSameTags(expected, actual)
                        && actor.level().getRecipeManager().byKey(recipe.getId()).orElse(null) == recipe;
            }
        }
    }
    private record Grid(List<ItemStack> cells, int width, int height) implements CraftingContainer {
        public int getWidth() { return width; } public int getHeight() { return height; }
        public List<ItemStack> getItems() { return cells; }
        public void fillStackedContents(net.minecraft.world.entity.player.StackedContents contents) {
            cells.forEach(contents::accountStack);
        }
        public int getContainerSize() { return cells.size(); }
        public boolean isEmpty() { return cells.stream().allMatch(ItemStack::isEmpty); }
        public ItemStack getItem(int slot) { return cells.get(slot); }
        public ItemStack removeItem(int slot, int amount) { throw new UnsupportedOperationException("Read-only native recipe snapshot"); }
        public ItemStack removeItemNoUpdate(int slot) { throw new UnsupportedOperationException("Read-only native recipe snapshot"); }
        public void setItem(int slot, ItemStack stack) { throw new UnsupportedOperationException("Read-only native recipe snapshot"); }
        public void setChanged() {} public boolean stillValid(Player player) { return false; }
        public void clearContent() { throw new UnsupportedOperationException("Read-only native recipe snapshot"); }
    }
    private CombatCraftProof() {}
    public static void clear() { STATE.remove(); }
    private static void strip(ItemStack stack) { if (stack.getTag() != null) { stack.getTag().remove(MACE); if (stack.getTag().isEmpty()) stack.setTag(null); } }
    public static void prepare(Player player, ItemStack result) {
        if (!CombatToolProof.real(player) || !CombatToolProof.item(result).equals("trials:mace")) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", player.getUUID()); proof.putUUID("token", UUID.randomUUID());
        result.getOrCreateTag().put(MACE, proof);
    }
    public static void quickBegin(Player actor, AbstractContainerMenu menu, int index) {
        State state = STATE.get();
        if (state.quickOverflow > 0 || state.quick.size() >= CombatToolProof.LIMIT) { state.quickOverflow++; return; }
        ItemStack result = ItemStack.EMPTY; Recipe<?> recipe = null;
        if (index == 0 && !menu.slots.isEmpty() && menu.getSlot(0).container instanceof RecipeHolder holder) {
            recipe = holder.getRecipeUsed(); prepare(actor, menu.getSlot(0).getItem()); result = menu.getSlot(0).getItem().copy();
        }
        state.quick.push(new Quick(actor, menu, recipe, result));
    }
    public static void quickEnd() {
        State state = STATE.get(); if (state.quickOverflow > 0) state.quickOverflow--; else if (!state.quick.isEmpty()) state.quick.pop();
        if (state.quick.isEmpty() && state.runs.isEmpty()) STATE.remove();
    }
    public static void begin(Player actor, CraftingContainer source, Recipe<?> selected, ItemStack result) {
        State state = STATE.get();
        if (state.overflow > 0 || state.runs.size() >= CombatToolProof.LIMIT) { state.overflow++; return; }
        if (!state.quick.isEmpty() && state.quickOverflow == 0) {
            Quick quick = state.quick.peek();
            if (quick.actor == actor && actor.containerMenu == quick.menu) {
                if (selected == null) selected = quick.recipe;
                if (result.isEmpty()) result = quick.result;
            }
        }
        state.runs.push(new Run(CombatToolProof.real(actor) ? (ServerPlayer)actor : null, source,
                selected instanceof CraftingRecipe crafting ? crafting : null, result));
    }
    public static void removed(CraftingContainer source, int slot, ItemStack removed) {
        State state = STATE.get();
        if (state.overflow > 0 || state.runs.isEmpty()) return;
        Run run = state.runs.peek();
        if (run.source == source && slot >= 0 && slot < run.grid.getContainerSize() && ItemStack.isSameItemSameTags(removed, run.grid.getItem(slot))) run.removed[slot] += removed.getCount();
    }
    private static int ingredients(Run run, String id) {
        int count = 0; for (int slot = 0; slot < run.grid.getContainerSize(); slot++) if (CombatToolProof.item(run.grid.getItem(slot)).equals(id)) count += run.removed[slot]; return count;
    }
    private static void certifyMace(ItemStack item, UUID token) {
        CompoundTag proof = item.getTagElement(MACE);
        if (proof != null && proof.hasUUID("token") && token.equals(proof.getUUID("token"))) proof.putBoolean("committed", true);
    }
    public static void end() {
        State state = STATE.get();
        if (state.overflow > 0) { state.overflow--; return; }
        Run run = state.runs.isEmpty() ? null : state.runs.pop();
        if (state.runs.isEmpty() && state.quick.isEmpty()) STATE.remove();
        if (run == null || !run.valid || !CombatToolProof.real(run.actor)) return;
        for (int slot = 0; slot < run.grid.getContainerSize(); slot++) if (run.removed[slot] != (run.grid.getItem(slot).isEmpty() ? 0 : 1)) return;
        String recipe = run.recipe.getId().toString(), output = CombatToolProof.item(run.result);
        if (recipe.equals("trials:mace") && output.equals("trials:mace") && ingredients(run, "trials:heavy_core") == 1 && ingredients(run, "trials:breeze_rod") == 1) {
            CompoundTag proof = run.result.getTagElement(MACE);
            if (proof != null && proof.hasUUID("token") && proof.hasUUID("actor") && run.actor.getUUID().equals(proof.getUUID("actor"))) {
                UUID token = proof.getUUID("token"); certifyMace(run.result, token);
                for (int slot = 0; slot < run.actor.getInventory().getContainerSize(); slot++) certifyMace(run.actor.getInventory().getItem(slot), token);
                certifyMace(run.actor.containerMenu.getCarried(), token); CombatToolProof.emit(run.actor, "mace_smash_crafted");
            }
        }
        if (recipe.equals("block_factorys_bosses:kraken_trident") && output.equals("block_factorys_bosses:kraken_trident") && ingredients(run, "block_factorys_bosses:kraken_tooth") == 3
                || recipe.startsWith("block_factorys_bosses:dragon_bones_") && output.equals(recipe) && ingredients(run, "block_factorys_bosses:dragon_bone") > 0) CombatToolProof.emit(run.actor, "bone_trophy_craft");
        if (recipe.equals("deep_dark_regrowth:white_neon_lamp_off_r") && output.equals("deep_dark_regrowth:white_neon_lamp_off") && ingredients(run, "deep_dark_regrowth:fluorite_crystal") == 4) CombatToolProof.emit(run.actor, "fluorite_light");
        if (recipe.equals("rottencreatures:corrupted_wart") && output.equals("rottencreatures:corrupted_wart") && ingredients(run, "rottencreatures:frozen_rotten_flesh") == 1) CombatToolProof.emit(run.actor, "frozen_brewing_processed");
    }
}
