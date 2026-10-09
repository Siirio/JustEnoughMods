package com.siirio.jemcompat.compat.jade;

import com.siirio.jemcompat.config.JEMClientConfig;
import com.siirio.jemcompat.feature.create.DrillWearAccess;
import com.siirio.jemcompat.feature.create.StationaryDrillWear;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;
import java.util.List;

@WailaPlugin
public final class DrillWearJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = new ResourceLocation("jemcompat", "drill_wear");
    private static final ResourceLocation TAMING_UID = new ResourceLocation("jemcompat", "taming_item");
    private static final ResourceLocation BREEDING_UID = new ResourceLocation("jemcompat", "breeding_item");
    private static final String TAMING_ITEMS = "JEMTamingItems";
    private static final String BREEDING_ITEMS = "JEMBreedingItems";
    private static final int COMPACT_FOOD_THRESHOLD = 6;
    private static final long FOOD_ICON_ROTATION_NANOS = 1_000_000_000L;
    private static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "seeds"));
    private static final TagKey<Item> RAW_MEATS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "raw_meats"));
    private static final DataProvider DATA = new DataProvider();
    private static final ComponentProvider COMPONENT = new ComponentProvider();
    private static final TamingDataProvider TAMING_DATA = new TamingDataProvider();
    private static final TamingComponentProvider TAMING_COMPONENT = new TamingComponentProvider();
    private static final BreedingDataProvider BREEDING_DATA = new BreedingDataProvider();
    private static final BreedingComponentProvider BREEDING_COMPONENT = new BreedingComponentProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        if (ModList.get().isLoaded("create")) {
            registration.registerBlockDataProvider(DATA, DrillBlockEntity.class);
        }
        registration.registerEntityDataProvider(TAMING_DATA, LivingEntity.class);
        registration.registerEntityDataProvider(BREEDING_DATA, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(Integer.MAX_VALUE, (tooltip, accessor) -> {
            if (!JEMClientConfig.SHOW_MOD_NAMES.get()) {
                tooltip.remove(Identifiers.CORE_MOD_NAME);
            }
            if (accessor instanceof EntityAccessor entityAccessor && !entityAccessor.getEntity().hasCustomName()) {
                tooltip.remove(Identifiers.CORE_OBJECT_NAME);
                tooltip.add(0, Component.translatable(entityAccessor.getEntity().getType().getDescriptionId()), Identifiers.CORE_OBJECT_NAME);
            }
        });
        if (ModList.get().isLoaded("create")) {
            registration.registerBlockComponent(COMPONENT, DrillBlock.class);
        }
        registration.registerEntityComponent(TAMING_COMPONENT, LivingEntity.class);
        registration.registerEntityComponent(BREEDING_COMPONENT, LivingEntity.class);
    }

    private static final class TamingDataProvider implements IServerDataProvider<EntityAccessor> {
        @Override
        public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(accessor.getEntity().getType());
            ListTag items = new ListTag();
            TamingDataLoader.catalog().itemsFor(entityId).stream()
                    .filter(BuiltInRegistries.ITEM::containsKey)
                    .map(ResourceLocation::toString)
                    .map(StringTag::valueOf)
                    .forEach(items::add);
            if (!items.isEmpty()) {
                tag.put(TAMING_ITEMS, items);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return TAMING_UID;
        }
    }

    private static final class TamingComponentProvider implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            ListTag items = accessor.getServerData().getList(TAMING_ITEMS, Tag.TAG_STRING);
            for (Tag itemTag : items) {
                ResourceLocation itemId = ResourceLocation.tryParse(itemTag.getAsString());
                if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                    continue;
                }
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
                tooltip.add(Component.translatable("jade.jemcompat.tamable_with", Component.translatable(stack.getDescriptionId())));
                tooltip.append(tooltip.getElementHelper().smallItem(stack));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return TAMING_UID;
        }
    }

    private static final class BreedingDataProvider implements IServerDataProvider<EntityAccessor> {
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
        public ResourceLocation getUid() {
            return BREEDING_UID;
        }
    }

    private static final class BreedingComponentProvider implements IEntityComponentProvider {
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

    private static final class DataProvider implements IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof DrillWearAccess wear) {
                tag.putInt(StationaryDrillWear.WEAR_KEY, wear.jemcompat$getWear());
                tag.putInt(StationaryDrillWear.REPAIRS_KEY, wear.jemcompat$getRepairs());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static final class ComponentProvider implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            int wear = data.getInt(StationaryDrillWear.WEAR_KEY);
            int repairs = data.getInt(StationaryDrillWear.REPAIRS_KEY);
            float remaining = 1.0F - (float) wear / StationaryDrillWear.MAX_WEAR;
            IElementHelper elements = tooltip.getElementHelper();
            tooltip.add(elements.progress(remaining,
                    Component.translatable("jemcompat.drill.wear", wear, StationaryDrillWear.MAX_WEAR),
                    elements.progressStyle().color(0xFFCC3333, 0xFF3A3A3A), BoxStyle.DEFAULT, false));
            tooltip.add(Component.translatable("jemcompat.drill.repairs", repairs, StationaryDrillWear.MAX_REPAIRS));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
