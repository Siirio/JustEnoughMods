package com.siirio.jemvisualsandbox.client.races;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/** A display-only player that deliberately has no world capability lifecycle. */
final class DetachedPreviewPlayer extends RemotePlayer {
    private final LocalPlayer source;

    DetachedPreviewPlayer(ClientLevel level, LocalPlayer source) {
        // World render hooks can hide entities by UUID even when they have no capabilities.
        super(level, new GameProfile(UUID.randomUUID(), source.getGameProfile().getName()));
        this.source = source;
        noCulling = true;
    }

    @Override public boolean isSkinLoaded() { return source.isSkinLoaded(); }
    @Override public ResourceLocation getSkinTextureLocation() { return source.getSkinTextureLocation(); }
    @Override public boolean isCapeLoaded() { return source.isCapeLoaded(); }
    @Override public ResourceLocation getCloakTextureLocation() { return source.getCloakTextureLocation(); }
    @Override public boolean isElytraLoaded() { return source.isElytraLoaded(); }
    @Override public ResourceLocation getElytraTextureLocation() { return source.getElytraTextureLocation(); }
    @Override public String getModelName() { return source.getModelName(); }
    @Override public boolean isModelPartShown(PlayerModelPart part) { return source.isModelPartShown(part); }

    /**
     * This entity never joins its level. Exposing capabilities attached during construction would
     * let render adapters consume a patch that never received its required join event.
     */
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return LazyOptional.empty();
    }
}
