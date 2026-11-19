package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battlearmor.WorldBattleArmorQuestMotion;
import com.justenoughmods.achievementguide.integration.combattools.CombatPullProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneControlProof;
import java.util.ArrayDeque;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns every JEM quest observation around the large vanilla Entity.move method. */
@Mixin(value = Entity.class, priority = 500)
public abstract class EntityMovementQuestMixin {
    @Unique
    private ArrayDeque<Optional<WorldBattleArmorQuestMotion.Move>> jem$armorMoves;

    @Inject(method = "move", at = @At("HEAD"))
    private void jem$beforeMove(MoverType type, Vec3 movement, CallbackInfo callback) {
        if (!((Object) this instanceof ServerPlayer)) return;
        if (jem$armorMoves == null) jem$armorMoves = new ArrayDeque<>();
        jem$armorMoves.push(Optional.ofNullable(WorldBattleArmorQuestMotion.begin((Entity) (Object) this)));
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void jem$afterMove(MoverType type, Vec3 movement, CallbackInfo callback) {
        Entity target = (Entity) (Object) this;
        if (!(target instanceof ServerPlayer)) {
            CombatPullProof.moved(target);
            CombatRuneControlProof.moved(target);
        }
        if (jem$armorMoves != null && !jem$armorMoves.isEmpty()) {
            WorldBattleArmorQuestMotion.end(jem$armorMoves.pop().orElse(null));
        }
    }
}
