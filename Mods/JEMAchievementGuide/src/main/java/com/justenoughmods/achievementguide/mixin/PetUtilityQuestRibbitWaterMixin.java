package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.yungnickyoung.minecraft.ribbits.entity.goal.RibbitWaterCropsGoal",remap=false)
public abstract class PetUtilityQuestRibbitWaterMixin {
 @Redirect(method="tryGrowCropAtPos",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/BonemealableBlock;m_214148_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))private static void jem$nativeGrowth(BonemealableBlock crop,ServerLevel level,RandomSource random,BlockPos pos,BlockState state){var actor=PetUtilityQuestPlacement.planter(level,pos,state);crop.performBonemeal(level,random,pos,state);var grown=level.getBlockState(pos);if(actor!=null && state.getBlock() instanceof CropBlock plant && grown.getBlock()==state.getBlock() && plant.getAge(grown)>plant.getAge(state))PetUtilityQuestProof.emit(actor,"v3_pet_swamp_neighbors");}
}
