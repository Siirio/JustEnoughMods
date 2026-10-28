package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.skill.SkillContainer; import net.minecraft.network.FriendlyByteBuf; import yesman.epicfight.api.animation.Animator; import yesman.epicfight.api.asset.AssetAccessor; import yesman.epicfight.api.animation.types.StaticAnimation;
@Mixin(value = yesman.epicfight.skill.BasicAttack.class, remap = false)
public abstract class BattleQuestBasicMixin {
    @Inject(method="executeOnServer", at=@At("HEAD")) private void jem$begin(SkillContainer c, FriendlyByteBuf args, CallbackInfo ci) { BattleQuestEpic.basicBegin(c); }
    @Inject(method="executeOnServer", at=@At("RETURN")) private void jem$end(SkillContainer c, FriendlyByteBuf args, CallbackInfo ci) { BattleQuestEpic.basicEnd(); }
    @Inject(method="checkConsumption", at=@At("RETURN")) private void jem$context(SkillContainer c, boolean dash, boolean air, CallbackInfoReturnable<Boolean> ci) { BattleQuestEpic.context(c,dash,air,ci.getReturnValue()); }
    @Redirect(method="executeOnServer",at=@At(value="INVOKE",target="Lyesman/epicfight/api/animation/Animator;playAnimation(Lyesman/epicfight/api/asset/AssetAccessor;F)V"))
    private void jem$played(Animator animator, AssetAccessor<? extends StaticAnimation> animation, float transition) { animator.playAnimation(animation,transition); BattleQuestEpic.played(animation); }
}
