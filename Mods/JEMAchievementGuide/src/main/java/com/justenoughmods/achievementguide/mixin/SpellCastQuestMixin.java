package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class SpellCastQuestMixin {
    @Inject(method = "castSpell", at = @At("TAIL"))
    private void jem$completedCast(Level level, int spellLevel, ServerPlayer actor, CastSource source,
                                   boolean triggerCooldown, CallbackInfo callback) {
        QuestActionTrigger.INSTANCE.emit(actor, "spell_successful_cast");
    }
}
