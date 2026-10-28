package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.JukeboxQuestReceipt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer", remap = false)
public abstract class StorageJukeboxMenuQuestMixin {
    @Unique private JukeboxQuestReceipt jem$playback;

    @Inject(method = "handleMessage", at = @At("HEAD"))
    private void jem$beforeAction(CompoundTag message, CallbackInfo callback) {
        jem$playback = null;
        JukeboxUpgradeContainer upgrade = (JukeboxUpgradeContainer) (Object) this;
        if (message.getString("action").equals("play") && upgrade.getUpgradeWrapper().isEnabled() && upgrade.getPlayer() instanceof ServerPlayer actor
                && actor.containerMenu instanceof StorageContainerMenuBase<?> menu && menu.stillValid(actor)
                && menu.getUpgradeContainers().values().stream().anyMatch(container -> container == upgrade)) {
            jem$playback = (JukeboxQuestReceipt) upgrade.getUpgradeWrapper();
            jem$playback.jem$beginPlayback(actor);
        }
    }

    @Inject(method = "handleMessage", at = @At("TAIL"))
    private void jem$afterAction(CompoundTag message, CallbackInfo callback) {
        if (jem$playback != null) jem$playback.jem$finishPlayback();
        jem$playback = null;
    }
}
