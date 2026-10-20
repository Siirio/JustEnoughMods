package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.irons.SiphonHealing;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.spells.blood.RayOfSiphoningSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.damage.DamageSources", remap = false)
public abstract class IronsSiphonHealingMixin {
    @Unique
    private static boolean jem$siphoning(LivingEntity target, DamageSource source) {
        return target.level() instanceof ServerLevel level && level.getServer().isDedicatedServer()
                && source instanceof SpellDamageSource spell && spell.spell() instanceof RayOfSiphoningSpell
                && ModList.get().getModContainerById("irons_spellbooks")
                .map(mod -> mod.getModInfo().getVersion().toString().equals("1.20.1-3.15.6")).orElse(false);
    }

    // Iron 3.15.6 heals during LivingHurtEvent, before armor, absorption and final cancellation.
    @Redirect(method = "postHitEffects(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V", remap = true),
            require = 1, expect = 1, remap = false)
    private static void jem$deferSiphoning(LivingEntity caster, float amount, LivingHurtEvent event) {
        if (!jem$siphoning(event.getEntity(), event.getSource())) caster.heal(amount);
    }

    @Redirect(method = "applyDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 1, expect = 1, remap = false)
    private static boolean jem$acceptedSiphoning(LivingEntity target, DamageSource source, float amount) {
        if (!jem$siphoning(target, source)) return target.hurt(source, amount);
        SpellDamageSource spell = (SpellDamageSource) source;
        float before = target.getHealth() + target.getAbsorptionAmount();
        boolean accepted = target.hurt(source, amount);
        float healing = SiphonHealing.acceptedHealing(accepted, before,
                target.getHealth() + target.getAbsorptionAmount(), spell.getLifestealPercent());
        // Keep native caster identity and LivingHealEvent modifiers, with no hit replay or queued state.
        if (healing > 0.0F && spell.getEntity() instanceof LivingEntity caster) caster.heal(healing);
        return accepted;
    }
}
