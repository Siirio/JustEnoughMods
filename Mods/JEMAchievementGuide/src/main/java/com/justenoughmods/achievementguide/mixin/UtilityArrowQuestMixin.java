package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.ArrowQuestActor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.morearrows.lists.backend.ArrowsAPI", remap = false)
public abstract class UtilityArrowQuestMixin {
    @Redirect(method = "leashTarget", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/Mob;m_21463_(Lnet/minecraft/world/entity/Entity;Z)V"))
    private static void jem$actualLeash(Mob target, Entity holder, boolean synchronize, EntityHitResult hit, AbstractArrow arrow) {
        Entity before = target.getLeashHolder();
        float health = target.getHealth();
        target.setLeashedTo(holder, synchronize);
        var actor = ArrowQuestActor.get(arrow);
        if (actor != null && holder == actor && before != actor && hit.getEntity() == target
                && target.isAlive() && target.getLeashHolder() == actor && target.getHealth() == health) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_arrow_transport");
        }
    }

    @Redirect(method = {"torchArrowOnBlockHit", "soulTorchArrowOnBlockHit", "redstoneTorchArrowOnBlockHit",
            "lanternArrowOnBlockHit", "soulLanternArrowOnBlockHit"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 17)
    private static boolean jem$actualTorch(Level level, BlockPos position, BlockState state,
                                           BlockHitResult hit, Level nativeLevel, AbstractArrow arrow) {
        BlockState before = level.getBlockState(position);
        boolean placed = level.setBlockAndUpdate(position, state);
        var actor = ArrowQuestActor.get(arrow);
        BlockState after = level.getBlockState(position);
        if (placed && actor != null && nativeLevel == level && arrow.level() == level
                && before != after && after.equals(state) && after.getLightEmission(level, position) > 0) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_arrow_tools");
        }
        return placed;
    }
}
