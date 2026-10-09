package com.siirio.jemcompat.client;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CuratedItemTooltips {
    private static final TagKey<Item> TRANSPORT=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","transport"));

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null) return;
        if (id.toString().equals("fairylights:spearhead_pennant"))
            event.getToolTip().add(Component.translatable("tooltip.jemcompat.spearhead_pennant").withStyle(ChatFormatting.GRAY));
        if (id.getNamespace().equals("moretotems") && id.getPath().endsWith("_totem_of_undying"))
            event.getToolTip().add(Component.translatable("tooltip.jemcompat.moretotems." + id.getPath()).withStyle(ChatFormatting.GRAY));
        if(event.getItemStack().is(TRANSPORT))
            event.getToolTip().add(Component.translatable(transportDescription(id)).withStyle(ChatFormatting.GRAY));
    }

    private static String transportDescription(ResourceLocation id) {
        String value=id.toString();
        if(value.equals("minecraft:elytra")||value.equals("alexsmobs:tarantula_hawk_elytra")) return "tooltip.jemcompat.transport.elytra";
        if(value.equals("paraglider:paraglider")||value.equals("paraglider:deku_leaf")) return "tooltip.jemcompat.transport.paraglider";
        if(value.equals("minecraft:happy_ghast_spawn_egg")) return "tooltip.jemcompat.transport.happy_ghast";
        if(id.getNamespace().equals("minecraft")&&id.getPath().endsWith("_harness")) return "tooltip.jemcompat.transport.harness";
        if(value.equals("alexsmobs:spawn_egg_laviathan")) return "tooltip.jemcompat.transport.laviathan";
        if(value.equals("alexsmobs:spawn_egg_spectre")) return "tooltip.jemcompat.transport.spectre";
        if(value.equals("alexsmobs:spawn_egg_straddler")||value.equals("alexsmobs:straddle_saddle")) return "tooltip.jemcompat.transport.straddler";
        if(value.equals("alexsmobs:spawn_egg_elephant")) return "tooltip.jemcompat.transport.elephant";
        if(value.equals("alexsmobs:spawn_egg_tusklin")) return "tooltip.jemcompat.transport.tusklin";
        if(value.equals("alexscaves:spawn_egg_subterranodon")) return "tooltip.jemcompat.transport.subterranodon";
        if(value.equals("alexscaves:spawn_egg_atlatitan")) return "tooltip.jemcompat.transport.atlatitan";
        if(value.equals("alexscaves:spawn_egg_tremorsaurus")) return "tooltip.jemcompat.transport.tremorsaurus";
        if(id.getPath().endsWith("spawn_egg")) return "tooltip.jemcompat.transport.mount";
        return "tooltip.jemcompat.transport.item";
    }

    private CuratedItemTooltips() {
    }
}
