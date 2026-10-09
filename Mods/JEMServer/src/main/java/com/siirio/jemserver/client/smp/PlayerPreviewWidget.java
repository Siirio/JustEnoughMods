package com.siirio.jemserver.client.smp;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Quaternionf;

final class PlayerPreviewWidget {
    private static final long APPEARANCE_REFRESH_TICKS = 40;
    private final Map<UUID, PreviewPlayerEntity> previews = new HashMap<>();
    private long appearanceRefresh;

    void render(GuiGraphics graphics, UUID id, String name, int x, int y, int width, int height, int mouseX, int mouseY) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        AbstractClientPlayer player = player(minecraft, id, name);
        if (player == null) return;
        if (player instanceof PreviewPlayerEntity preview && minecraft.level.getGameTime() >= appearanceRefresh) {
            preview.refreshAppearance();
            appearanceRefresh = minecraft.level.getGameTime() + APPEARANCE_REFRESH_TICKS;
        }
        int scale = Math.max(20, Math.min(width * 7 / 16, height * 7 / 16));
        int bottom = y + height - 5;
        var dispatcher = minecraft.getEntityRenderDispatcher();
        var camera = new Quaternionf(dispatcher.cameraOrientation());
        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            RenderSystem.enableDepthTest();
            try {
                renderEntity(graphics, player, x, y, width, height, bottom, scale, mouseX, mouseY);
            } catch (RuntimeException incompatibleRenderer) {
                var fallback = previews.computeIfAbsent(id, key -> new PreviewPlayerEntity(minecraft.level, new GameProfile(id, name)));
                if (fallback != player) {
                    try {
                        renderEntity(graphics, fallback, x, y, width, height, bottom, scale, mouseX, mouseY);
                    } catch (RuntimeException ignored) {}
                }
            }
            graphics.flush();
        } finally {
            graphics.disableScissor();
            dispatcher.overrideCameraOrientation(camera);
            dispatcher.setRenderShadow(true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Lighting.setupForFlatItems();
            com.siirio.jemserver.client.ui.SmpRenderState.restoreMainTarget();
        }
    }

    private void renderEntity(GuiGraphics graphics, AbstractClientPlayer player, int x, int y, int width, int height, int bottom, int scale, int mouseX, int mouseY) {
        int center = x + width / 2;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, center, bottom, scale, center - mouseX, y + height / 2f - mouseY, player);
    }

    private AbstractClientPlayer player(Minecraft minecraft, UUID id, String name) {
        var loaded = minecraft.level.getPlayerByUUID(id);
        GameProfile profile = loaded instanceof AbstractClientPlayer player
                ? player.getGameProfile()
                : minecraft.player.getUUID().equals(id) ? minecraft.player.getGameProfile() : new GameProfile(id, name);
        var preview = previews.get(id);
        if (preview == null || preview.level() != minecraft.level || !preview.getGameProfile().equals(profile)) {
            preview = new PreviewPlayerEntity(minecraft.level, profile);
            previews.put(id, preview);
        }
        return preview;
    }

}
