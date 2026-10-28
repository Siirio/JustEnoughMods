package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.compat.ftb.BookMigration;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guard before native parent settlement, then pass the exact native source through delivery. */
@Mixin(value = TeamData.class, remap = false)
public abstract class FtbRewardClaimMixin {
    @Inject(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$guard(ServerPlayer player, Reward reward, boolean notify, long time, CallbackInfo callback) {
        if (!BookMigration.allow((TeamData)(Object)this, player, reward)) callback.cancel();
    }

    @Redirect(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V",
            at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbquests/quest/reward/Reward;claim(Lnet/minecraft/server/level/ServerPlayer;Z)V"), remap = false)
    private void jem$sourceTeam(Reward reward, ServerPlayer player, boolean notify) {
        BookMigration.invoke(reward, player, notify, (TeamData)(Object)this);
    }
}
