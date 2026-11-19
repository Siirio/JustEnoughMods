package com.siirio.jemcompat.client;

import com.siirio.jemcompat.JEMCompat;
import com.siirio.jemcompat.feature.transport.TransportNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransportBurdenClient {
    private static int occupiedSlots;
    private static int totalSlots;
    private static int reductionTenths;

    private TransportBurdenClient() {
    }

    public static void accept(TransportNetwork.BurdenStatus status) {
        occupiedSlots = Math.max(0, status.occupiedSlots());
        totalSlots = Math.max(0, status.totalSlots());
        reductionTenths = Math.max(0, Math.min(700, status.reductionTenths()));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        occupiedSlots = 0;
        totalSlots = 0;
        reductionTenths = 0;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) || totalSlots <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        Item backpack = ForgeRegistries.ITEMS.getValue(new ResourceLocation("sophisticatedbackpacks", "backpack"));
        if (backpack == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int x = event.getWindow().getGuiScaledWidth() / 2 + 96;
        int y = event.getWindow().getGuiScaledHeight() - 21;
        int filledPixels = Math.min(16, Math.max(0, (int) Math.ceil(16.0 * occupiedSlots / totalSlots)));
        int color = burdenColor(reductionTenths / 700.0F);
        ItemStack icon = new ItemStack(backpack);

        graphics.renderItem(icon, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0x99000000);
        if (filledPixels > 0) {
            graphics.enableScissor(x, y + 16 - filledPixels, x + 16, y + 16);
            graphics.renderItem(icon, x, y);
            graphics.fill(x, y + 16 - filledPixels, x + 16, y + 16, 0x66000000 | color);
            graphics.disableScissor();
        }
        graphics.drawString(minecraft.font, reductionLabel(), x + 19, y + 5, 0xFFFFFF, true);
    }

    private static String reductionLabel() {
        int whole = reductionTenths / 10;
        int fraction = reductionTenths % 10;
        return fraction == 0 ? "-" + whole + "%" : "-" + whole + "." + fraction + "%";
    }

    private static int burdenColor(float progress) {
        float bounded = Math.max(0.0F, Math.min(1.0F, progress));
        if (bounded < 0.5F) {
            int red = Math.round(255.0F * bounded * 2.0F);
            return red << 16 | 0xFF00;
        }
        int green = Math.round(255.0F * (1.0F - bounded) * 2.0F);
        return 0xFF0000 | green << 8;
    }
}
