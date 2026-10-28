package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.ItemTransferObservation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper",
        "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper"
}, remap = false)
public abstract class BackpackTransferQuestMixin {
    @Unique private ServerPlayer jem$actor;
    @Unique private ItemTransferObservation.Before jem$transfer;

    @Inject(method = "onHandlerInteract", at = @At("HEAD"))
    private void jem$beforeTransfer(IItemHandler external, Player player, CallbackInfo callback) {
        jem$actor = null;
        jem$transfer = null;
        var wrapper = ((UpgradeWrapperQuestAccess) this).jem$questStorageWrapper();
        if (player instanceof ServerPlayer actor && !(actor instanceof FakePlayer) && !actor.isSpectator()
                && !actor.getAbilities().instabuild && wrapper instanceof IBackpackWrapper && ((IUpgradeWrapper) this).isEnabled()) {
            var backpack = wrapper.getInventoryForUpgradeProcessing();
            jem$transfer = (Object) this instanceof DepositUpgradeWrapper
                    ? ItemTransferObservation.before(backpack, external) : ItemTransferObservation.before(external, backpack);
            if (jem$transfer != null) jem$actor = actor;
        }
    }

    @Inject(method = "onHandlerInteract", at = @At("RETURN"))
    private void jem$afterTransfer(IItemHandler external, Player player, CallbackInfo callback) {
        ServerPlayer actor = jem$actor;
        var transfer = jem$transfer;
        jem$actor = null;
        jem$transfer = null;
        if (actor == player && actor != null && ItemTransferObservation.committed(transfer)) {
            QuestActionTrigger.INSTANCE.emit(actor, (Object) this instanceof DepositUpgradeWrapper ? "storage_deposit" : "storage_restock");
        }
    }
}
