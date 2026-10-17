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
    @Override public void register(IWailaCommonRegistration registration) { registration.registerEntityDataProvider(RESIDENT, net.minecraft.world.entity.LivingEntity.class); }
    @Override public void registerClient(IWailaClientRegistration registration) { registration.registerEntityComponent(RESIDENT, net.minecraft.world.entity.LivingEntity.class); }

    private static final class ResidentProvider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
        private static final ResourceLocation UID = new ResourceLocation("jem_settlements", "resident");
        @Override public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            if (accessor.getEntity().getServer() == null) return;
            if (net.minecraftforge.fml.ModList.get().isLoaded("recruits")
                    && RecruitJadeData.append(tag, accessor.getEntity())) return;
            if (!(accessor.getEntity() instanceof Villager villager)) return;
            var resident = com.siirio.jemsettlements.town.SettlementSavedData.get(villager.getServer()).resident(villager.getUUID());
            if (resident.isEmpty()) return;
            tag.putBoolean("JemResident", true);
            com.siirio.jemsettlements.town.SettlementSavedData.get(villager.getServer()).town(resident.get().townId())
                    .ifPresent(town -> tag.putString("JemSettlementName", town.name()));
            tag.putInt("JemHappiness", resident.get().mood());
            tag.putInt("JemSatiety", resident.get().satiety()); tag.putBoolean("JemCraving", resident.get().craving());
            resident.get().requestedDish().ifPresent(value -> tag.putString("JemRequestedDish", value.toString()));
            resident.get().repeatedDish().ifPresent(value -> tag.putString("JemRepeatedDish", value.toString()));
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            if (!tag.getBoolean("JemResident")) return;
            if (tag.contains("JemSettlementName")) tooltip.add(Component.translatable("jade.jem_settlements.settlement", tag.getString("JemSettlementName")));
            var elements = tooltip.getElementHelper();
            tooltip.add(new NeedsBarElement(tag.getInt("JemHappiness"), true));
            tooltip.add(new NeedsBarElement(tag.getInt("JemSatiety"), false));
            if (tag.getBoolean("JemCraving")) tooltip.add(Component.translatable("jade.jem_settlements.craving"));
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
