package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.irons.SiphonHealing;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.spells.blood.RayOfSiphoningSpell;
import io.redspace.ironsspellbooks.spells.blood.BloodNeedlesSpell;
import io.redspace.ironsspellbooks.spells.blood.BloodSlashSpell;
import io.redspace.ironsspellbooks.spells.blood.DevourSpell;
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
    private static boolean jem$supportedLifesteal(LivingEntity target, DamageSource source) {
        return target.level() instanceof ServerLevel
                && source instanceof SpellDamageSource spell
                && (spell.spell() instanceof RayOfSiphoningSpell || spell.spell() instanceof BloodNeedlesSpell
                || spell.spell() instanceof BloodSlashSpell || spell.spell() instanceof DevourSpell)
                && ModList.get().getModContainerById("irons_spellbooks")
                .map(mod -> mod.getModInfo().getVersion().toString().equals("1.20.1-3.15.6")).orElse(false);
    }

    // Iron 3.15.6 heals during LivingHurtEvent, before armor, absorption and final cancellation.
    @Redirect(method = "postHitEffects(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V", remap = true),
            require = 1, expect = 1, remap = false)
    private static void jem$deferSiphoning(LivingEntity caster, float amount, LivingHurtEvent event) {
        if (!jem$supportedLifesteal(event.getEntity(), event.getSource())
                || !SiphonHealing.defer(event.getEntity(), event.getSource(), caster,
                ((SpellDamageSource) event.getSource()).getLifestealPercent())) caster.heal(amount);
    }
}
