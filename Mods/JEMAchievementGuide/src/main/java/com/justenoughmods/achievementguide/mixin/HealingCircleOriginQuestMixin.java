package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.HealingCircleProof;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.HealingAoe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.spells.holy.HealingCircleSpell", remap = false)
public abstract class HealingCircleOriginQuestMixin {
    @Redirect(method = "onCast", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeCircle(Level world, Entity circle, Level level, int spellLevel,
                                     LivingEntity caster, CastSource source, MagicData magic) {
        boolean accepted = world.addFreshEntity(circle);
        if (accepted && circle instanceof HealingAoe && caster instanceof ServerPlayer actor
                && world == level && MagicData.getPlayerMagicData(actor) == magic) HealingCircleProof.spawned(circle, actor);
        return accepted;
    }
}
