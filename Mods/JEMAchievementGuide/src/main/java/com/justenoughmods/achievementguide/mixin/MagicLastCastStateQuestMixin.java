package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = MagicData.class, remap = false)
public abstract class MagicLastCastStateQuestMixin {
    @Shadow private ServerPlayer serverPlayer;
    @Inject(method = "initiateCast", at = @At("RETURN"))
    private void jem$generation(AbstractSpell spell, int level, int duration, CastSource source, String slot, CallbackInfo callback) { MagicLastSpellProof.initiated((MagicData)(Object)this, serverPlayer, spell, level, source); }
    @Inject(method = "resetCastingState", at = @At("RETURN"))
    private void jem$reset(CallbackInfo callback) { MagicLastSpellProof.reset((MagicData)(Object)this); }
}
