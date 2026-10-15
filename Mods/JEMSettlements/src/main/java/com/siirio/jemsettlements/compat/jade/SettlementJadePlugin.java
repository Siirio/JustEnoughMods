package com.siirio.jemsettlements.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class SettlementJadePlugin implements IWailaPlugin {
    private static final ResidentProvider RESIDENT = new ResidentProvider();
    @Override public void register(IWailaCommonRegistration registration) { registration.registerEntityDataProvider(RESIDENT, Villager.class); }
    @Override public void registerClient(IWailaClientRegistration registration) { registration.registerEntityComponent(RESIDENT, Villager.class); }

    private static final class ResidentProvider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
        private static final ResourceLocation UID = new ResourceLocation("jem_settlements", "resident");
        @Override public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof Villager villager) || villager.getServer() == null) return;
            var resident = com.siirio.jemsettlements.town.SettlementSavedData.get(villager.getServer()).resident(villager.getUUID());
            if (resident.isEmpty()) return;
            tag.putBoolean("JemResident", true);
            tag.putInt("JemHappiness", resident.get().mood());
            tag.putInt("JemHunger", resident.get().hunger());
            tag.putString("JemProfession", net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION
                    .getKey(villager.getVillagerData().getProfession()).toString());
            resident.get().requestedDish().ifPresent(value -> tag.putString("JemRequestedDish", value.toString()));
            resident.get().repeatedDish().ifPresent(value -> tag.putString("JemRepeatedDish", value.toString()));
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            if (!tag.getBoolean("JemResident")) return;
            tooltip.add(Component.translatable("jade.jem_settlements.happiness", tag.getInt("JemHappiness")));
            tooltip.add(Component.translatable("jade.jem_settlements.hunger", tag.getInt("JemHunger")));
            ResourceLocation profession = ResourceLocation.tryParse(tag.getString("JemProfession"));
            if (profession != null) tooltip.add(Component.translatable("jade.jem_settlements.profession",
                    Component.translatable("entity.minecraft.villager." + profession.getPath())));
            ResourceLocation request = ResourceLocation.tryParse(tag.getString("JemRequestedDish"));
            if (request != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(request)) {
                ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(request));
                tooltip.add(Component.translatable("jade.jem_settlements.request", stack.getHoverName()));
                tooltip.append(tooltip.getElementHelper().smallItem(stack));
            }
            ResourceLocation repeated = ResourceLocation.tryParse(tag.getString("JemRepeatedDish"));
            if (repeated != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(repeated)) {
                ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(repeated));
                tooltip.add(Component.translatable("jade.jem_settlements.repeated", stack.getHoverName()));
                tooltip.append(tooltip.getElementHelper().smallItem(stack));
            }
        }
        @Override public ResourceLocation getUid() { return UID; }
    }
}
