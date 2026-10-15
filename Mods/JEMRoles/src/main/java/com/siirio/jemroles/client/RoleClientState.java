package com.siirio.jemroles.client;

import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.network.RaceStatusPacket;
import com.siirio.jemroles.network.RaceActionPacket;
import com.siirio.jemroles.network.RolesNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemRoles.MOD_ID, value = Dist.CLIENT)
public final class RoleClientState {
    private static RaceStatusPacket status;
    private static final KeyMapping SIGNATURE = new KeyMapping("key.jem_roles.signature", InputConstants.Type.KEYSYM,
            InputConstants.KEY_V, "key.categories.jem_roles");
    private static final KeyMapping UTILITY = new KeyMapping("key.jem_roles.utility", InputConstants.Type.KEYSYM,
            InputConstants.KEY_B, "key.categories.jem_roles");
    private static boolean utilityHeld;

    private RoleClientState() {}

    public static void accept(RaceStatusPacket packet) {
        status = packet;
        if (packet.open()) Minecraft.getInstance().setScreen(new RaceSelectionScreen(packet));
    }

    @Mod.EventBusSubscriber(modid = JemRoles.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(SIGNATURE);
            event.register(UTILITY);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (SIGNATURE.consumeClick()) RolesNetwork.action(RaceActionPacket.Action.SIGNATURE);
        boolean down = UTILITY.isDown();
        if (down && !utilityHeld) RolesNetwork.action(RaceActionPacket.Action.UTILITY_PRESS);
        if (!down && utilityHeld) RolesNetwork.action(RaceActionPacket.Action.UTILITY_RELEASE);
        utilityHeld = down;
    }

    @SubscribeEvent
    public static void hud(RenderGuiOverlayEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (status == null || !status.selected() || status.race() != com.siirio.jemroles.race.RaceId.NETHER_HEARTEDS
                || minecraft.player == null || !minecraft.player.isInLava()
                || !event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id())) return;
        int max = Math.max(1, status.lavaCapacity());
        int remaining = Math.max(0, max - status.lavaExposure());
        int bubbles = (int)Math.ceil(remaining / (double)max * 10.0D);
        int start = event.getWindow().getGuiScaledWidth() / 2 + 91;
        int y = event.getWindow().getGuiScaledHeight() - 49;
        for (int i = 0; i < 10; i++) {
            int color = i < bubbles ? 0xFFFF8A24 : 0x557A3218;
            event.getGuiGraphics().fill(start - i * 9, y, start - i * 9 + 7, y + 7, color);
            event.getGuiGraphics().fill(start - i * 9 + 1, y + 1, start - i * 9 + 3, y + 3, 0xFFFFC06A);
        }
    }
}
