package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestHurt;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.event.ServerEvents", remap = false)
public abstract class WildlifeQuestNativeDefenseMixin {
    @Unique private final ThreadLocal<Boolean> jem$oldEffect = new ThreadLocal<>();
    @Unique private final ThreadLocal<Boolean> jem$oldCancel = new ThreadLocal<>();
    @Inject(method = "onLivingAttack", at = @At("HEAD"))
    private void jem$beforeEffect(LivingAttackEvent event, CallbackInfo callback) {
        jem$oldEffect.set(event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(AMEffectRegistry.EXSANGUINATION.get()));
    }
    @Inject(method = "onLivingAttack", at = @At("RETURN"))
    private void jem$afterEffect(LivingAttackEvent event, CallbackInfo callback) {
        boolean had = Boolean.TRUE.equals(jem$oldEffect.get()); jem$oldEffect.remove();
        if (!had && event.getEntity() instanceof Player player && WildlifeQuestProof.real(player) != null
                && WildlifeQuestProof.item(player.getUseItem()).equals("alexsmobs:shield_of_the_deep")
                && event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(AMEffectRegistry.EXSANGUINATION.get())) {
            WildlifeQuestProof.mark(attacker, "deep_shield_actor", player.getUUID());
            WildlifeQuestProof.data(attacker).putLong("deep_shield_time", player.level().getGameTime());
        }
    }
    @Inject(method = "onLivingDamageEvent", at = @At("HEAD"))
    private void jem$beforeEvade(LivingDamageEvent event, CallbackInfo callback) { jem$oldCancel.set(event.isCanceled()); }
    @Inject(method = "onLivingDamageEvent", at = @At("RETURN"))
    private void jem$afterEvade(LivingDamageEvent event, CallbackInfo callback) {
        boolean canceled = Boolean.TRUE.equals(jem$oldCancel.get()); jem$oldCancel.remove();
        if (!canceled && event.isCanceled() && event.getAmount() > 0 && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && WildlifeQuestProof.item(event.getEntity().getItemBySlot(EquipmentSlot.LEGS)).equals("alexsmobs:emu_leggings")) WildlifeQuestHurt.emu(event);
    }
}
