package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.client.AugmentIcons;
import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "jemcompat", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BackpackClient {
    private BackpackClient() { }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("backpacked")) return;
        event.enqueueWork(() -> {
            AugmentIcons.set(WorkbenchAugment.TYPE, new WorkbenchIcon());
        });
    }
}
