package com.siirio.jemserver.client.smp;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

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
        var minecraft = Minecraft.getInstance();
        minecraft.getSkinManager().registerSkins(getGameProfile(), (type, location, texture) -> minecraft.execute(() -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                skin = location;
                model = texture.getMetadata("model") == null ? "default" : texture.getMetadata("model");
            } else if (type == MinecraftProfileTexture.Type.CAPE) cape = location;
        }), true);
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
        return null;
    }

    @Override
    public ResourceLocation getSkinTextureLocation() {
        return skin;
    }

    @Override
    public ResourceLocation getCloakTextureLocation() {
        return cape;
    }

    @Override
    public boolean isCapeLoaded() {
        return cape != null;
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        return true;
    }
}
