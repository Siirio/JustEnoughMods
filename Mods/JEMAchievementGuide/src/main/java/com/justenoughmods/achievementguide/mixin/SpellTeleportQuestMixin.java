package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TeleportSpell.class, remap = false)
public abstract class SpellTeleportQuestMixin {
    @Redirect(method = "onCast", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/util/Utils;handleSpellTeleport(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)Z"))
    private boolean jem$committedCasterTeleport(AbstractSpell spell, Entity caster, Vec3 destination) {
        Vec3 before = caster.position();
        // Installed 3.15.6 returns cancellation, not success
        boolean cancelled = Utils.handleSpellTeleport(spell, caster, destination);
        if (!cancelled && caster instanceof ServerPlayer actor && actor.isAlive()
                && actor.position().distanceToSqr(before) > 0.0001 && actor.serverLevel().noCollision(actor)) {
            QuestActionTrigger.INSTANCE.emit(actor, "spell_self_relocation");
        }
        return cancelled;
    }
}
