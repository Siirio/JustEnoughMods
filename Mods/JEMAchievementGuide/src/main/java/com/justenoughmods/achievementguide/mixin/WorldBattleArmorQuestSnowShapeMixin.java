package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlearmor.*;import net.minecraft.world.level.block.PowderSnowBlock;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.BlockGetter;import net.minecraft.core.BlockPos;import net.minecraft.world.phys.shapes.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PowderSnowBlock.class,priority=500) public abstract class WorldBattleArmorQuestSnowShapeMixin {
 @Inject(method="canEntityWalkOnPowderSnow",at=@At("RETURN")) private static void jem$final(net.minecraft.world.entity.Entity entity,CallbackInfoReturnable<Boolean> ci){WorldBattleArmorQuestMotion.snowFinal(entity,ci.getReturnValue());}
 @Inject(method="getCollisionShape",at=@At("RETURN")) private void jem$shape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> ci){if(context instanceof EntityCollisionContext entity)WorldBattleArmorQuestMotion.snowShape(entity.getEntity(),pos,ci.getReturnValue());}
}
