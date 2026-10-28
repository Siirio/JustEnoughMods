package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StorageCompressionProof;
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
public abstract class StorageCompressionQuestMixin {
    @Unique private StorageCompressionProof.Before jem$compressionBefore;
    @Inject(method = "m_150399_", at = @At("HEAD"))
    private void jem$beforeCompression(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        jem$compressionBefore = (Object) this instanceof StorageContainerMenu menu && player instanceof ServerPlayer actor
                && actor.containerMenu == menu ? StorageCompressionProof.before(menu, actor) : null;
    }
    @Inject(method = "m_150399_", at = @At("RETURN"))
    private void jem$afterCompression(int slot, int button, ClickType click, Player player, CallbackInfo callback) {
        var before = jem$compressionBefore;
        jem$compressionBefore = null;
        if ((Object) this instanceof StorageContainerMenu menu && player instanceof ServerPlayer actor) StorageCompressionProof.after(menu, actor, before);
    }
}
