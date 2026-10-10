package com.siirio.jemcompat.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

final class BreedingProvider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
    private static final ResourceLocation BREEDING_UID = new ResourceLocation("jemcompat", "breeding_item");
    private static final String BREEDING_ITEMS = "JEMBreedingItems";
    private static final int COMPACT_FOOD_THRESHOLD = 6;
    private static final long FOOD_ICON_ROTATION_NANOS = 1_000_000_000L;
    private static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "seeds"));
    private static final TagKey<Item> RAW_MEATS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "raw_meats"));

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof Animal animal)) {
            return;
        }
        ListTag items = new ListTag();
        BreedingFoodCatalog.itemsFor(animal).stream()
                .map(ResourceLocation::toString)
                .map(StringTag::valueOf)
                .forEach(items::add);
        if (!items.isEmpty()) {
            tag.put(BREEDING_ITEMS, items);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        ListTag items = accessor.getServerData().getList(BREEDING_ITEMS, Tag.TAG_STRING);
        List<ItemStack> foods = new ArrayList<>();
        for (Tag itemTag : items) {
            ResourceLocation itemId = ResourceLocation.tryParse(itemTag.getAsString());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                continue;
            }
            foods.add(new ItemStack(BuiltInRegistries.ITEM.get(itemId)));
        }
        if (foods.size() >= COMPACT_FOOD_THRESHOLD) {
            long rotation = System.nanoTime() / FOOD_ICON_ROTATION_NANOS;
            ItemStack rotatingFood = foods.get((int) (rotation % foods.size()));
            tooltip.add(compactFoodLabel(foods));
            tooltip.append(tooltip.getElementHelper().smallItem(rotatingFood));
            return;
        }
        for (ItemStack stack : foods) {
            tooltip.add(Component.translatable("jade.jemcompat.breedable_with", Component.translatable(stack.getDescriptionId())));
            tooltip.append(tooltip.getElementHelper().smallItem(stack));
        }
    }

    private static Component compactFoodLabel(List<ItemStack> foods) {
        if (foods.stream().allMatch(stack -> stack.is(ItemTags.FISHES))) {
            return Component.translatable("jade.jemcompat.breedable_with.any_fish");
        }
        if (foods.stream().allMatch(stack -> stack.is(SEEDS))) {
            return Component.translatable("jade.jemcompat.breedable_with.any_seed");
        }
        if (foods.stream().allMatch(stack -> stack.is(RAW_MEATS))) {
            return Component.translatable("jade.jemcompat.breedable_with.any_raw_meat");
        }
        return Component.translatable("jade.jemcompat.breedable_with.many");
    }

    @Override
    public ResourceLocation getUid() {
        return BREEDING_UID;
    }
}
