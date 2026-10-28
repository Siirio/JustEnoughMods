package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import net.minecraft.world.entity.LivingEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.ShoulderBowEntity", remap = false)
public abstract class CombatRuneEchoCompanionQuestMixin {
    @Shadow public abstract boolean isMirroringOffhand();
    @Inject(method = "firePreparedArrow", at = @At("HEAD"))
    private void jem$begin(LivingEntity actor, CallbackInfo callback) { if (isMirroringOffhand() && actor.getOffhandItem().getItem() instanceof SimplyBowItem) CombatRuneProof.deliveredBow(actor, actor.getMainHandItem(), "bounty"); else CombatRuneProof.beginBow(actor, actor.getMainHandItem()); }
    @Inject(method = "firePreparedArrow", at = @At("RETURN"))
    private void jem$end(LivingEntity actor, CallbackInfo callback) { CombatRuneProof.leave(); }
}
