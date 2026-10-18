package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.network.message.MessageChangeAugment;
import com.mrcrayfish.backpacked.network.message.MessageSetAugmentState;
import com.mrcrayfish.backpacked.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.siirio.jemcompat.feature.backpack.AugmentBayMenu;
import com.siirio.jemcompat.feature.backpack.BackpackWorkbench;
import com.siirio.jemcompat.feature.backpack.WorkbenchAugment;
import com.siirio.jemcompat.feature.transport.AircraftPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayHandler.class, remap = false)
public abstract class BackpackAugmentAdmissionMixin {
    @Inject(method = "handleChangeAugment", at = @At("HEAD"), cancellable = true)
    private static void jemcompat$requireWorkbenchPurchase(MessageChangeAugment message, MessageContext context, CallbackInfo callback) {
        ServerPlayer player = context.getPlayer();
        if (player == null || !AircraftPolicy.enabled(player) || !message.augmentTypeId().equals(WorkbenchAugment.TYPE.id())) return;
        if (!(player.containerMenu instanceof BackpackContainerMenu menu) || !menu.isOwner() || !menu.stillValid(player)
                || !menu.getAugmentBayController().isSlotUnlocked(message.position().ordinal())
                || !BackpackHelper.getBackpackStack(player, menu.getBackpackIndex()).getOrCreateTag().getBoolean(AugmentBayMenu.WORKBENCH_PURCHASED)) {
            callback.cancel();
            player.displayClientMessage(Component.translatable("message.jemcompat.augment_bay.purchase_workbench"), true);
        }
    }

    @Inject(method = "handleChangeAugment", at = @At("TAIL"))
    private static void jemcompat$refreshChangedLayout(MessageChangeAugment message, MessageContext context, CallbackInfo callback) {
        jemcompat$refresh(context.getPlayer());
    }

    @Inject(method = "handleSetAugmentState", at = @At("TAIL"))
    private static void jemcompat$refreshToggledLayout(MessageSetAugmentState message, MessageContext context, CallbackInfo callback) {
        jemcompat$refresh(context.getPlayer());
    }

    private static void jemcompat$refresh(ServerPlayer player) {
        if (player == null || !AircraftPolicy.enabled(player) || !(player.containerMenu instanceof BackpackContainerMenu menu)) return;
        boolean installed = BackpackHelper.findAugment(BackpackHelper.getBackpackStack(player, menu.getBackpackIndex()), WorkbenchAugment.TYPE) != null;
        boolean present = menu.slots.size() > menu.getCols() * menu.getRows() + 36;
        if (installed != present) BackpackItem.openBackpack(player, player, menu.getBackpackIndex());
    }
}
