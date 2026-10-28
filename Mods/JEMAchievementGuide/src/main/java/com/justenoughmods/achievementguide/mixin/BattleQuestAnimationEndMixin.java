package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.api.asset.AssetAccessor; import yesman.epicfight.api.animation.types.DynamicAnimation; import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch; import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
@Mixin(value = yesman.epicfight.api.animation.types.AttackAnimation.class, remap = false)
public abstract class BattleQuestAnimationEndMixin {
    @Inject(method="end",at=@At("RETURN"))
    private void jem$end(LivingEntityPatch<?> patch, AssetAccessor<? extends DynamicAnimation> next, boolean end, CallbackInfo ci) {
        if(patch instanceof ServerPlayerPatch server) { BattleQuestEpic.animationEnd(server,(DynamicAnimation)(Object)this,end); if(net.minecraftforge.fml.ModList.get().isLoaded("efn")) BattleQuestNightfall.animationEnd(server,(DynamicAnimation)(Object)this); }
    }
}
