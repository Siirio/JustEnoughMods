package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.compat.ftb.BookMigration;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Audited native 2001.4.22 choice packet delivers its option before checking parent claim state. */
@Mixin(targets = "dev.ftb.mods.ftbquests.net.ClaimChoiceRewardMessage", remap = false)
public abstract class FtbChoiceClaimMixin {
    @Shadow @Final private long id;
    @Shadow @Final private int index;
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$guardChoice(NetworkManager.PacketContext context, CallbackInfo callback) {
        if (context.getPlayer() instanceof ServerPlayer player && BookMigration.choice(player, id, index)) callback.cancel();
    }
}
