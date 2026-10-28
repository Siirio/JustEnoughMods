package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsCableTransferProof;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import com.tom.storagemod.util.StoredItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.StorageTerminalMenu", remap = false)
public abstract class TomsCableTerminalQuestMixin {
    @Unique private TomsCableTransferProof.Before jem$cableTransfer;
    @Inject(method = "onInteract", at = @At("HEAD"))
    private void jem$beforeTransfer(StoredItemStack item, StorageTerminalMenu.SlotAction action, boolean modifier, CallbackInfo callback) {
        jem$cableTransfer = TomsCableTransferProof.before((StorageTerminalMenu) (Object) this, item);
    }
    @Redirect(method = "onInteract", at = @At(value = "INVOKE", target =
            "Lcom/tom/storagemod/tile/StorageTerminalBlockEntity;pullStack(Lcom/tom/storagemod/util/StoredItemStack;J)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jem$nativeExtraction(StorageTerminalBlockEntity terminal, StoredItemStack item, long amount) {
        StoredItemStack result = terminal.pullStack(item, amount);
        TomsCableTransferProof.pulled(jem$cableTransfer, result);
        return result;
    }
    @Inject(method = "onInteract", at = @At("RETURN"))
    private void jem$afterTransfer(StoredItemStack item, StorageTerminalMenu.SlotAction action, boolean modifier, CallbackInfo callback) {
        var before = jem$cableTransfer;
        jem$cableTransfer = null;
        TomsCableTransferProof.after((StorageTerminalMenu) (Object) this, before);
    }
}
