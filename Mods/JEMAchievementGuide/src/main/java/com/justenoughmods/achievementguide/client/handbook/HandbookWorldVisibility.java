package com.justenoughmods.achievementguide.client.handbook;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.patchouli.client.book.gui.GuiBook;

/** Connection-local presentation snapshot. The server owns both discovery flags. */
public final class HandbookWorldVisibility {
    private static boolean nether;
    private static boolean end;

    private HandbookWorldVisibility() { }

    public static boolean hidden(ResourceLocation dimension) {
        if (dimension == null) return false;
        return switch (dimension.toString()) {
            case "minecraft:the_nether" -> !nether;
            case "minecraft:the_end" -> !end;
            default -> false;
        };
    }

    public static void update(boolean netherOpen, boolean endOpen) {
        boolean changed = nether != netherOpen || end != endOpen;
        nether = netherOpen;
        end = endOpen;
        Minecraft mc = Minecraft.getInstance();
        if (changed && mc.screen instanceof GuiBook screen && HandbookNavigation.BOOK.equals(screen.book.id)) {
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        nether = false;
        end = false;
    }
}
