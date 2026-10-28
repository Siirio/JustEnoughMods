package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.MagicConchItem", remap = false)
public abstract class MagicConchQuestMixin {
    @Redirect(method = "summonDeepOne", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$allyActuallySummoned(Level level, Entity ally, EntityType<?> type, LivingEntity owner, int duration) {
        boolean accepted = level.addFreshEntity(ally);
        if (accepted && owner instanceof ServerPlayer actor && actor.level() == level && ally.level() == level
                && ally.getType() == type && duration > 0 && ally instanceof ConchAllyQuestAccess summoned
                && summoned.jem$nativeSummoned() && summoned.jem$nativeSummonTime() > 0
                && actor.getUUID().equals(summoned.jem$nativeSummoner())) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_cave_water_arsenal");
        }
        return accepted;
    }
}
