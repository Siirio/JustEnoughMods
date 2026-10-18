package com.siirio.jemcompat.feature.backpack;

import com.mrcrayfish.backpacked.client.AugmentIcons;
import com.mrcrayfish.framework.api.client.screen.widget.texture.FrameworkTexture;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
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
            MenuScreens.register(BackpackContent.AUGMENT_BAY_MENU.get(), AugmentBayScreen::new);
            AugmentIcons.set(WorkbenchAugment.TYPE, FrameworkTexture.subImage(
                    new ResourceLocation("jemcompat", "textures/gui/workbench_augment.png"), 0, 0, 12, 12, 12, 12));
        });
    }
}
