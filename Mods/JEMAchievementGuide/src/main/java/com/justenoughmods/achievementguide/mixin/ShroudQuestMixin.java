package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.spells.eldritch.AbyssalShroudSpell", remap = false)
public abstract class ShroudQuestMixin {
    @Redirect(method = "onCast", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$nativeLearnedShroud(LivingEntity target, MobEffectInstance requested, Level level, int spellLevel,
                                           LivingEntity caster, CastSource source, MagicData magic) {
        var previous = target.getEffect(requested.getEffect());
        int duration = previous == null ? 0 : previous.getDuration();
        int amplifier = previous == null ? -1 : previous.getAmplifier();
        boolean learned = caster instanceof ServerPlayer actor && MagicData.getPlayerMagicData(actor) == magic
                && magic.getSyncedData().isSpellLearned((AbstractSpell) (Object) this);
        boolean accepted = target.addEffect(requested);
        var actual = target.getEffect(requested.getEffect());
        if (accepted && learned && target == caster && caster instanceof ServerPlayer actor && actor.level() == level
                && requested.getEffect() == MobEffectRegistry.ABYSSAL_SHROUD.get() && actual != null && actual.getDuration() > 0
                && (actual.getDuration() > duration || actual.getAmplifier() > amplifier)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_magic_shroud");
        }
        return accepted;
    }
}
