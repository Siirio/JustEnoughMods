package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestWorm;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.siirio.jemrewards.PendingRewardContainer", remap = false)
public abstract class WildlifeQuestWormRewardClaimMixin {
    @Unique private static WildlifeQuestWorm.Claim jem$claim;
    @Inject(method = "claimSelection", at = @At("HEAD"))
    private static void jem$before(ServerPlayer actor, Set<UUID> bundles, int item, CallbackInfoReturnable<Boolean> callback) { jem$claim = WildlifeQuestWorm.beforeClaim(actor); }
    @Inject(method = "claimSelection", at = @At("RETURN"))
    private static void jem$after(ServerPlayer actor, Set<UUID> bundles, int item, CallbackInfoReturnable<Boolean> callback) { var run = jem$claim; jem$claim = null; WildlifeQuestWorm.claimed(run, callback.getReturnValue()); }
}
