package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastBloodProof;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastCauldronProof;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastFocusProof;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class MagicLastNativeSpawnQuestMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void jem$spawn(Entity output, CallbackInfoReturnable<Boolean> callback) { boolean accepted = callback.getReturnValue(); MagicLastFocusProof.spawned(output, accepted); MagicLastBloodProof.spawned(output, accepted); MagicLastCauldronProof.spawned(output, accepted); MagicLastSpellProof.spawned(output, accepted); }
}
