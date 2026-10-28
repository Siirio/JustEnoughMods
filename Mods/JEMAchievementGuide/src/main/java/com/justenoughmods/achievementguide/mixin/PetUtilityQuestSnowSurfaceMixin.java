package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="net.mehvahdjukaar.snowyspirit.common.entity.GroundStatus",remap=false)
public abstract class PetUtilityQuestSnowSurfaceMixin {
 @Redirect(method="computeFriction",at=@At(value="INVOKE",target="Lnet/mehvahdjukaar/moonlight/api/platform/ForgeHelper;getFriction(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"))private static float jem$nativeSurface(BlockState state,LevelReader level,BlockPos pos,Entity entity){float result=ForgeHelper.getFriction(state,level,pos,entity);PetUtilityQuestSnowContact.surface(entity,state);return result;}
}
