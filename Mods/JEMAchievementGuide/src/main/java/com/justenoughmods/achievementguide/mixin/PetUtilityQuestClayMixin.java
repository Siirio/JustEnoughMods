package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="tech.alexnijjar.golemoverhaul.common.blocks.ClayGolemStatueBlock",remap=false)
public abstract class PetUtilityQuestClayMixin {
 @Redirect(method="spawnGolem",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private boolean jem$heatedBirth(Level level,Entity golem,Level l,BlockPos pos,BlockState state){var actor=l instanceof ServerLevel server?PetUtilityQuestPlacement.planter(server,pos,state):null;boolean accepted=level.addFreshEntity(golem);if(accepted)PetUtilityQuestPlacement.accepted(golem);if(accepted && actor!=null && level.getBlockState(pos).equals(state))PetUtilityQuestProof.put(golem,"clay",actor.getUUID());return accepted;}
}
