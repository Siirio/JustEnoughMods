package com.siirio.jembuilding.client;

import com.siirio.jembuilding.JEMBuilding;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlueprintClientModels {
    static final ResourceLocation CLOSED = model("blueprint_roll_closed_model");
    static final ResourceLocation OPEN = model("blueprint_roll_open_model");
    static final ResourceLocation FINISHED_ICON = model("structure_blueprint_icon");

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(CLOSED);
        event.register(OPEN);
        event.register(FINISHED_ICON);
    }

    private static ResourceLocation model(String name) {
        return new ResourceLocation(JEMBuilding.MOD_ID, "item/" + name);
    }

    private BlueprintClientModels() { }
}
