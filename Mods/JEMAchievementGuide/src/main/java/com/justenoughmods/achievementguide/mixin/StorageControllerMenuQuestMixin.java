package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StorageControllerProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class StorageControllerMenuQuestMixin {
    @Unique private StorageControllerProof.Before jem$controllerBefore;
    @Inject(method = "m_150399_", at = @At("HEAD"))
    private void jem$beforeInsert(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        jem$controllerBefore = (Object) this instanceof StorageContainerMenu menu && player instanceof ServerPlayer actor
                ? StorageControllerProof.before(menu, actor) : null;
    }
    @Inject(method = "m_150399_", at = @At("RETURN"))
    private void jem$afterInsert(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        var before = jem$controllerBefore; jem$controllerBefore = null;
        if ((Object) this instanceof StorageContainerMenu menu && player instanceof ServerPlayer actor) StorageControllerProof.inserted(menu, actor, before);
    }
}
