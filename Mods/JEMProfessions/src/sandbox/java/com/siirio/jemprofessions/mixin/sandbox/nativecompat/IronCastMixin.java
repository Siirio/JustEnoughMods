package com.siirio.jemprofessions.mixin.sandbox.nativecompat;

import com.siirio.jemprofessions.sandbox.nativecompat.SandboxMagicEffects;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iron 3.15.6 has no public successful-and-paid completion event; do not infer payment from pre-debit events. */
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class IronCastMixin {
    @Inject(method = "castSpell", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"))
    private void jem$paid(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean cooldown, CallbackInfo callback) {
        SandboxMagicEffects.beforeNativeCast(player, ((AbstractSpell) (Object) this).getSpellId());
    }

    @Inject(method = "castSpell", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V", shift = At.Shift.AFTER))
    private void jem$completed(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean cooldown, CallbackInfo callback) {
        SandboxMagicEffects.afterNativeCast(player, ((AbstractSpell) (Object) this).getSpellId());
    }
}
