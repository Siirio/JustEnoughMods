package com.siirio.jemcompat.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

final class TamingProvider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
    private static final ResourceLocation TAMING_UID = new ResourceLocation("jemcompat", "taming_item");
    private static final String TAMING_ITEMS = "JEMTamingItems";

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
