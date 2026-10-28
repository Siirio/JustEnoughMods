package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityRhinoceros;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityRhinoceros", remap = false)
public abstract class WildlifeQuestRhinoCoatMixin {
    @Shadow private boolean trusts(java.util.UUID actor) { throw new AssertionError(); }
    @Unique private ItemStack jem$potion;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$potion = player.getItemInHand(hand).copy(); }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$coated(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EntityRhinoceros self = (EntityRhinoceros)(Object)this; var actor = WildlifeQuestProof.real(player); var effects = PotionUtils.getPotion(jem$potion).getEffects();
        if (actor != null && trusts(actor.getUUID()) && callback.getReturnValue().consumesAction() && !effects.isEmpty()
                && (jem$potion.is(Items.POTION) || jem$potion.is(Items.SPLASH_POTION) || jem$potion.is(Items.LINGERING_POTION)) && WildlifeQuestProof.debited(jem$potion, player.getItemInHand(hand), 1)
                && self.getPotionEffect() == effects.get(0).getEffect() && self.getPotionDuration() == effects.get(0).getDuration()) WildlifeQuestProof.mark(self, "rhino_coater", actor.getUUID());
    }
    @Redirect(method = "attackWithPotion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$horn(LivingEntity target, MobEffectInstance effect) {
        boolean absent = !target.hasEffect(effect.getEffect()); boolean accepted = target.addEffect(effect); EntityRhinoceros self = (EntityRhinoceros)(Object)this;
        var actor = WildlifeQuestProof.actor(self, "rhino_coater");
        if (actor != null && trusts(actor.getUUID()) && absent && accepted && target.hasEffect(effect.getEffect()) && WildlifeQuestProof.hostile(target, actor)) WildlifeQuestProof.emit(actor, "v3_fauna_companion_powers");
        return accepted;
    }
}
