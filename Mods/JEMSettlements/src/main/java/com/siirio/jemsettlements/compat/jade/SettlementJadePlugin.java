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
    static void appendConditions(CompoundTag tag, com.siirio.jemsettlements.resident.ResidentMood history) {
        tag.putInt("JemCrowdingPenalty",history.crowdingPenalty());tag.putInt("JemLightPenalty",history.lightPenalty());
        tag.putInt("JemInjuryPenalty",history.injuryPenalty());tag.putInt("JemHarmfulFoodPenalty",history.harmfulFoodPenalty());
        tag.putBoolean("JemHousingKnown",history.needObservation().contains("HasBed"));
        tag.putBoolean("JemHasBed",history.needObservation().getBoolean("HasBed"));tag.putBoolean("JemRoofed",history.needObservation().getBoolean("Roofed"));
        tag.putInt("JemFearPenalty",history.fearPenalty());tag.putBoolean("JemConditionsUnknown",history.conditionsUnknown());
        tag.putInt("JemFamilyLightPenalty",history.familyLightPenalty());tag.putInt("JemFamilyResourcePenalty",history.familyResourcePenalty());
    }
    @Override public void register(IWailaCommonRegistration registration) { registration.registerEntityDataProvider(RESIDENT, net.minecraft.world.entity.LivingEntity.class); }
    @Override public void registerClient(IWailaClientRegistration registration) { registration.registerEntityComponent(RESIDENT, net.minecraft.world.entity.LivingEntity.class); }

    private static final class ResidentProvider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
        private static final ResourceLocation UID = new ResourceLocation("jem_settlements", "resident");
        @Override public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            if (accessor.getEntity().getServer() == null) return;
            if (com.siirio.jemsettlements.family.FamilyRuntime.supported(accessor.getEntity()) && accessor.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
                var families=com.siirio.jemsettlements.family.VillagerFamilySavedData.get(living.getServer());
                tag.merge(com.siirio.jemsettlements.family.FamilyRuntime.familyTag(families,families.ensure(living)));
            }
            if (!(accessor.getEntity() instanceof Villager villager)) return;
            var resident = com.siirio.jemsettlements.town.SettlementSavedData.get(villager.getServer()).resident(villager.getUUID());
            if (resident.isEmpty()) return;
            tag.putBoolean("JemResident", true);tag.putBoolean("JemOrdinaryResident",true);
            com.siirio.jemsettlements.town.SettlementSavedData.get(villager.getServer()).town(resident.get().townId())
                    .ifPresent(town -> tag.putString("JemSettlementName", town.name()));
            tag.putInt("JemHappiness", resident.get().mood());
            tag.putBoolean("JemCrowdedHome", resident.get().crowdedHome().isPresent());
            appendConditions(tag, resident.get());
            tag.putInt("JemSatiety", resident.get().satiety()); tag.putBoolean("JemCraving", resident.get().craving());
            resident.get().requestedDish().ifPresent(value -> tag.putString("JemRequestedDish", value.toString()));
            resident.get().repeatedDish().ifPresent(value -> tag.putString("JemRepeatedDish", value.toString()));
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            appendFamily(tooltip,tag);
            if (!tag.getBoolean("JemResident")) return;
            if (tag.contains("JemSettlementName")) tooltip.add(Component.translatable("jade.jem_settlements.settlement", tag.getString("JemSettlementName")));
            var elements = tooltip.getElementHelper();
            tooltip.add(new NeedsBarElement(tag.getInt("JemHappiness"), true));
            if (tag.getBoolean("JemCrowdedHome")) tooltip.add(Component.translatable("jem.settlements.housing.personal_space_hint"));
            if (tag.getInt("JemCrowdingPenalty")>0) tooltip.add(Component.translatable("jem.settlements.conditions.crowding",tag.getInt("JemCrowdingPenalty")));
            if (tag.getInt("JemLightPenalty")>0) tooltip.add(Component.translatable("jem.settlements.conditions.light",tag.getInt("JemLightPenalty")));
            if (tag.getInt("JemFamilyLightPenalty")>tag.getInt("JemLightPenalty")) tooltip.add(Component.translatable("jem.settlements.conditions.home_light",tag.getInt("JemFamilyLightPenalty")));
            if (tag.getInt("JemFamilyResourcePenalty")>0) tooltip.add(Component.translatable("jem.settlements.conditions.home_resources",tag.getInt("JemFamilyResourcePenalty")));
            if (tag.getInt("JemFearPenalty")>0) tooltip.add(Component.translatable("jem.settlements.conditions.fear",tag.getInt("JemFearPenalty")));
            if (tag.getInt("JemInjuryPenalty")>0) tooltip.add(Component.translatable("jem.settlements.conditions.injury",tag.getInt("JemInjuryPenalty")));
            if (tag.getInt("JemHarmfulFoodPenalty")>0) tooltip.add(Component.translatable("jem.settlements.conditions.harmful_food",tag.getInt("JemHarmfulFoodPenalty")));
            if (tag.getBoolean("JemOrdinaryResident") && tag.getBoolean("JemHousingKnown") && !tag.getBoolean("JemHasBed")) tooltip.add(Component.translatable("jem.settlements.gui.no_bed"));
            else if (tag.getBoolean("JemOrdinaryResident") && tag.getBoolean("JemHousingKnown") && !tag.getBoolean("JemRoofed")) tooltip.add(Component.translatable("jem.settlements.gui.no_roof"));
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
        private static void appendFamily(ITooltip tooltip,CompoundTag tag){if(!tag.getBoolean("JemFamily"))return;tooltip.add(Component.literal(tag.getString("JemFamilyName")));
            boolean male=tag.getString("JemGender").equals("MALE");tooltip.add(Component.translatable("jade.jem_settlements.gender").append(Component.translatable("jade.jem_settlements.gender."+(male?"male":"female")).withStyle(male?net.minecraft.ChatFormatting.BLUE:net.minecraft.ChatFormatting.LIGHT_PURPLE)));
            String family=tag.getBoolean("JemEternalSingle")?"eternal_"+(male?"male":"female"):tag.contains("JemSpouseName")?tag.getString("JemFamilyRole").toLowerCase(java.util.Locale.ROOT):"single_"+(male?"male":"female");net.minecraft.network.chat.MutableComponent familyText=Component.translatable("jade.jem_settlements.family").append(Component.translatable("jade.jem_settlements.family."+family,tag.getString("JemSpouseName")).withStyle(net.minecraft.ChatFormatting.GOLD));tooltip.add(familyText);
            String orientation=tag.getString("JemOrientation");net.minecraft.network.chat.MutableComponent label=Component.translatable("jade.jem_settlements.orientation");if(orientation.equals("SAME_SEX"))label.append(rainbow(Component.translatable(male?"jade.jem_settlements.orientation.gay":"jade.jem_settlements.orientation.lesbian").getString()));else label.append(Component.translatable("jade.jem_settlements.orientation.straight").withStyle(net.minecraft.ChatFormatting.GREEN));tooltip.add(label);}
        private static Component rainbow(String text){int[] colors={0xff5555,0xffaa00,0xffff55,0x55ff55,0x55ffff,0x5555ff,0xff55ff};net.minecraft.network.chat.MutableComponent result=Component.empty();for(int i=0;i<text.length();i++){int color=colors[i%colors.length];result.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(style->style.withColor(color)));}return result;}
        @Override public ResourceLocation getUid() { return UID; }
    }
}
