package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastAutoHealProof;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
@Pseudo
@Mixin(targets = "org.merlin204.efsiss.skill.passive.AutoHealSkill", remap = false)
public abstract class MagicLastAutoHealQuestMixin {
    @Inject(method = "updateContainer", at = @At("HEAD"))
    private void jem$begin(SkillContainer container, CallbackInfo callback) { MagicLastAutoHealProof.begin(container.getExecutor() instanceof ServerPlayerPatch patch ? (ServerPlayer)patch.getOriginal() : null); }
    @Redirect(method = "updateContainer", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;addMana(F)V"))
    private void jem$nativeCost(MagicData data, float cost, SkillContainer container) { ServerPlayer actor = container.getExecutor() instanceof ServerPlayerPatch patch ? (ServerPlayer)patch.getOriginal() : null; float before = data.getMana(), health = actor == null ? Float.NaN : actor.getHealth(); data.addMana(cost); if (actor != null) MagicLastAutoHealProof.cost(actor, before, data.getMana(), health); }
    @Redirect(method = "updateContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;m_5634_(F)V"))
    private void jem$nativeHeal(ServerPlayer actor, float amount) { float before = actor.getHealth(); actor.heal(amount); MagicLastAutoHealProof.healed(actor, before); }
    @Inject(method = "updateContainer", at = @At("RETURN"))
    private void jem$end(SkillContainer container, CallbackInfo callback) { MagicLastAutoHealProof.end(); }
}
