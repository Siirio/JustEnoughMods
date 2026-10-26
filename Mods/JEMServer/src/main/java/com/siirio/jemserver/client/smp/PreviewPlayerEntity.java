package com.siirio.jemserver.client.smp;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.fml.ModList;

final class PreviewPlayerEntity extends RemotePlayer {
    private ResourceLocation skin;
    private ResourceLocation cape;
    private String model;

    PreviewPlayerEntity(ClientLevel level, GameProfile profile) {
        super(level, profile);
        skin = DefaultPlayerSkin.getDefaultSkin(profile.getId());
        model = DefaultPlayerSkin.getSkinModelName(profile.getId());
        noCulling = true;
        setSilent(true);
        refreshAppearance();
    }

    void refreshAppearance() {
        if (appearanceSource() != null) return;
        var minecraft = Minecraft.getInstance();
        minecraft.getSkinManager().registerSkins(getGameProfile(), (type, location, texture) -> minecraft.execute(() -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                skin = location;
                model = texture.getMetadata("model") == null ? "default" : texture.getMetadata("model");
            } else if (type == MinecraftProfileTexture.Type.CAPE) cape = location;
        }), true);
    }

    /** The loaded player's appearance already includes the installed skin provider. */
    private AbstractClientPlayer appearanceSource() {
        var level = Minecraft.getInstance().level;
        var entity = level == null ? null : level.getPlayerByUUID(getUUID());
        return entity instanceof AbstractClientPlayer player && player != this ? player : null;
    }

    /** Only the inspected native 2.6.2.4 API is called, and only after its mod is present. */
    private static boolean nativeQuickSkinAvailable() {
        return ModList.get().getModContainerById("quickskin")
                .map(mod -> "2.6.2.4".equals(mod.getModInfo().getVersion().toString())).orElse(false);
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
        return null;
    }

    @Override
    public ResourceLocation getSkinTextureLocation() {
        var source = appearanceSource();
        if (source != null) return source.getSkinTextureLocation();
        if (nativeQuickSkinAvailable()) {
            var nativeSkin = QuickSkinPreviewBridge.skin(getUUID());
            if (nativeSkin != null) return nativeSkin;
        }
        return skin;
    }

    @Override
    public ResourceLocation getCloakTextureLocation() {
        var source = appearanceSource();
        if (source != null) return source.getCloakTextureLocation();
        if (nativeQuickSkinAvailable() && QuickSkinPreviewBridge.hasCapeChoice(getUUID()))
            return QuickSkinPreviewBridge.cape(getUUID());
        return cape;
    }

    @Override
    public boolean isCapeLoaded() {
        var source = appearanceSource();
        return source == null ? getCloakTextureLocation() != null : source.isCapeLoaded();
    }

    @Override
    public String getModelName() {
        var source = appearanceSource();
        if (source != null) return source.getModelName();
        if (nativeQuickSkinAvailable()) {
            var nativeModel = QuickSkinPreviewBridge.model(getUUID());
            if (nativeModel != null) return nativeModel;
        }
        return model;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        var source = appearanceSource();
        return source == null ? super.isModelPartShown(part) : source.isModelPartShown(part);
    }
}
