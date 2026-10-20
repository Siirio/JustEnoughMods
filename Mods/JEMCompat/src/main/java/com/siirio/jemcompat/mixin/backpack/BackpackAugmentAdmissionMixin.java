package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.network.message.MessageChangeAugment;
import com.mrcrayfish.backpacked.network.message.MessageSetAugmentState;
import com.mrcrayfish.backpacked.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.siirio.jemcompat.feature.backpack.BackpackWorkbench;
import com.siirio.jemcompat.feature.backpack.*;
import com.siirio.jemcompat.feature.transport.AircraftPolicy;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayHandler.class, remap = false)
public abstract class BackpackAugmentAdmissionMixin {
    @Inject(method = "handleChangeAugment", at = @At("HEAD"), cancellable = true)
    private static void jemcompat$validateWorkbenchSelection(MessageChangeAugment message, MessageContext context, CallbackInfo callback) {
        ServerPlayer player = context.getPlayer();
        if (player == null || !AircraftPolicy.enabled(player) || !jemcompat$isOwnedType(message.augmentTypeId())) return;
        if (!(player.containerMenu instanceof BackpackContainerMenu menu) || !menu.isOwner() || !menu.stillValid(player)
                || !(menu.getBackpackInventory() instanceof com.mrcrayfish.backpacked.inventory.BackpackInventory)
                || !menu.getAugmentBayController().isSlotUnlocked(message.position().ordinal())) {
            callback.cancel();
        }
    }

    @Inject(method = "handleChangeAugment", at = @At("TAIL"))
    private static void jemcompat$refreshChangedLayout(MessageChangeAugment message, MessageContext context, CallbackInfo callback) {
        ServerPlayer player = context.getPlayer();
        if (player != null && AircraftPolicy.enabled(player) && player.containerMenu instanceof BackpackContainerMenu menu
                && menu.isOwner() && menu.stillValid(player) && jemcompat$isOwnedType(message.augmentTypeId())
                && menu.getAugmentBayController().isSlotUnlocked(message.position().ordinal())
                && menu.getAugments().getAugment(message.position()).type().id().equals(message.augmentTypeId())) {
            // A successful native selection immediately exposes its crafting layout.
            Augments.cached(BackpackHelper.getBackpackStack(player, menu.getBackpackIndex())).setState(message.position(), true);
            menu.setAugments(Augments.cached(BackpackHelper.getBackpackStack(player, menu.getBackpackIndex())).copy());
        }
        jemcompat$refresh(context.getPlayer());
    }

    @Inject(method = "handleSetAugmentState", at = @At("TAIL"))
    private static void jemcompat$refreshToggledLayout(MessageSetAugmentState message, MessageContext context, CallbackInfo callback) {
        jemcompat$refresh(context.getPlayer());
    }

    private static boolean jemcompat$isOwnedType(net.minecraft.resources.ResourceLocation id) {
        return id.equals(WorkbenchAugment.TYPE.id()) || id.equals(FurnaceAugment.TYPE.id()) || id.equals(AutoFeedAugment.TYPE.id());
    }

    private static void jemcompat$refresh(ServerPlayer player) {
        if (player == null || !AircraftPolicy.enabled(player) || !(player.containerMenu instanceof BackpackContainerMenu menu)) return;
        var layout = (BackpackWorkbench) menu;
        if (layout.jemcompat$hasWorkbench() != layout.jemcompat$installedWorkbench()
                || layout.jemcompat$hasFurnace() != layout.jemcompat$installedFurnace()) BackpackItem.openBackpack(player, player, menu.getBackpackIndex());
    }
}
