package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class MagicLastCastResultQuestMixin {
    @Inject(method = "castSpell", at = @At("HEAD"))
    private void jem$begin(Level level, int spellLevel, ServerPlayer actor, CastSource source, boolean cooldown, CallbackInfo callback) { MagicLastSpellProof.castBegin((AbstractSpell)(Object)this, level, spellLevel, actor, source); }
    @Inject(method = "castSpell", at = @At("RETURN"))
    private void jem$end(Level level, int spellLevel, ServerPlayer actor, CastSource source, boolean cooldown, CallbackInfo callback) { MagicLastSpellProof.castEnd(); }
}
