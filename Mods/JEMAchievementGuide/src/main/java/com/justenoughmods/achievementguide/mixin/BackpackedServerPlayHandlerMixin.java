package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import com.mrcrayfish.backpacked.network.message.MessageChangeAugment;
import com.mrcrayfish.backpacked.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayHandler.class, remap = false)
public abstract class BackpackedServerPlayHandlerMixin {
    private static final ResourceLocation FIRST_UPGRADE = new ResourceLocation("jem_guide", "qol/7_1");

    @Inject(
            method = "handleChangeAugment",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mrcrayfish/backpacked/common/augment/Augments;setAugment(Lcom/mrcrayfish/backpacked/common/augment/Augments$Position;Lcom/mrcrayfish/backpacked/common/augment/Augment;)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private static void onChangeAugment(MessageChangeAugment message, MessageContext context, CallbackInfo callback) {
        ServerPlayer player = context.getPlayer();
        if (player != null) {
            JemCriteria.fire(player, FIRST_UPGRADE);
        }
    }
}
