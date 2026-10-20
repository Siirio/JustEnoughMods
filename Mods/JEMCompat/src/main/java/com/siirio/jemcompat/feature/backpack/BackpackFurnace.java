package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.common.ForgeHooks;

/** One storage owner attached to the native inventory lifetime; durable state stays on its exact stack. */
public final class BackpackFurnace extends SimpleContainer implements ContainerData {
    private final ItemStack backpack;
    private final Map<ResourceLocation, Integer> recipes = new HashMap<>();
    private int burning, fuelDuration, progress, duration = 200;
    private ResourceLocation cooking;
    private boolean loading;

    public BackpackFurnace(ItemStack backpack) {
        super(3);
        this.backpack = backpack;
        loading = true;
        if (!backpack.isEmpty()) {
            CompoundTag tag = backpack.getOrCreateTag().getCompound("JemFurnace");
            NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
            for (int i = 0; i < 3; i++) super.setItem(i, items.get(i));
            burning = Math.max(0, tag.getInt("Burning"));
            fuelDuration = Math.max(burning, tag.getInt("FuelDuration"));
            duration = Math.max(1, tag.getInt("Duration"));
            progress = Math.min(Math.max(0, tag.getInt("Progress")), duration);
            cooking = ResourceLocation.tryParse(tag.getString("Cooking"));
            CompoundTag used = tag.getCompound("Recipes");
            for (String key : used.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null && used.getInt(key) > 0) recipes.put(id, used.getInt(key));
            }
        }
        loading = false;
    }

    @Override public void setChanged() { super.setChanged(); if (!loading) persist(); }
    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        return index != 2 && BackpackInventory.isAllowedItem(stack)
                && (index != 1 || ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0);
    }
    public void persist() {
        if (backpack.isEmpty()) return;
        CompoundTag tag = new CompoundTag();
        NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
        for (int i = 0; i < 3; i++) items.set(i, getItem(i));
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Burning", burning); tag.putInt("FuelDuration", fuelDuration);
        tag.putInt("Progress", progress); tag.putInt("Duration", duration);
        if (cooking != null) tag.putString("Cooking", cooking.toString());
        CompoundTag used = new CompoundTag();
        recipes.forEach((id, count) -> used.putInt(id.toString(), count));
        tag.put("Recipes", used);
        backpack.getOrCreateTag().put("JemFurnace", tag);
    }

    public void tick(ServerPlayer player) {
        var level = player.serverLevel();
        var found = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, this, level);
        SmeltingRecipe recipe = found.orElse(null);
        ResourceLocation id = recipe == null ? null : recipe.getId();
        if (!java.util.Objects.equals(cooking, id)) { progress = 0; cooking = id; }
        duration = recipe == null ? 200 : Math.max(1, recipe.getCookingTime());
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.assemble(this, level.registryAccess());
        ItemStack output = getItem(2);
        ItemStack input = getItem(0);
        // Input remainders are unusual for smelting; refuse those recipes before consuming anything.
        boolean canCook = !input.isEmpty() && !input.hasCraftingRemainingItem() && BackpackInventory.isAllowedItem(input) && !result.isEmpty()
                && result.isItemEnabled(level.enabledFeatures()) && BackpackInventory.isAllowedItem(result)
                && (output.isEmpty() || ItemStack.isSameItemSameTags(output, result))
                && output.getCount() + result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
        boolean changed = burning > 0 || progress > 0;
        if (burning > 0) burning--;
        if (canCook && burning == 0) {
            ItemStack fuel = getItem(1);
            int time = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
            ItemStack remainder = fuel.getCraftingRemainingItem();
            if (time > 0 && BackpackInventory.isAllowedItem(fuel) && (remainder.isEmpty() || fuel.getCount() == 1 && BackpackInventory.isAllowedItem(remainder))) {
                burning = fuelDuration = time;
                fuel.shrink(1);
                if (fuel.isEmpty()) super.setItem(1, remainder);
                changed = true;
            }
        }
        if (canCook && burning > 0) {
            changed = true;
            if (++progress >= duration) {
                if (output.isEmpty()) super.setItem(2, result.copy()); else output.grow(result.getCount());
                if (input.is(net.minecraft.world.item.Items.WET_SPONGE) && getItem(1).is(net.minecraft.world.item.Items.BUCKET))
                    super.setItem(1, net.minecraft.world.item.Items.WATER_BUCKET.getDefaultInstance());
                input.shrink(1);
                recipes.merge(recipe.getId(), 1, (a, b) -> Math.addExact(a, b));
                progress = 0;
            }
        } else if (progress != 0) { progress = 0; changed = true; }
        if (changed) setChanged();
    }

    /** Vanilla furnace awards accumulated recipe XP on a real output take, once. */
    public void award(ServerPlayer player) {
        for (var entry : recipes.entrySet()) {
            var recipe = player.serverLevel().getRecipeManager().byKey(entry.getKey()).orElse(null);
            if (recipe instanceof SmeltingRecipe smelting) {
                float amount = entry.getValue() * smelting.getExperience();
                int whole = net.minecraft.util.Mth.floor(amount);
                if (player.getRandom().nextFloat() < amount - whole) whole++;
                if (whole > 0) ExperienceOrb.award(player.serverLevel(), player.position(), whole);
                player.awardRecipes(java.util.List.of(recipe));
            }
        }
        recipes.clear();
        persist();
    }
    public int value(int index) { return switch(index) { case 0 -> burning; case 1 -> fuelDuration; case 2 -> progress; case 3 -> duration; default -> 0; }; }
    private void setValue(int index, int value) { switch(index) { case 0 -> burning = value; case 1 -> fuelDuration = value; case 2 -> progress = value; case 3 -> duration = value; default -> { } } }
    @Override public int get(int index) { return value(index / 2) >>> (index % 2 * 16) & 65535; }
    @Override public void set(int index, int part) {
        int shift = index % 2 * 16;
        setValue(index / 2, value(index / 2) & ~(65535 << shift) | (part & 65535) << shift);
    }
    @Override public int getCount() { return 8; }
}
