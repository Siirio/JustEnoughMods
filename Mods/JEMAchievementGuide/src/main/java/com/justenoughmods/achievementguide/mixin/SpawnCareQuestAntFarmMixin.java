package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.ServerLevel;import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.util.RandomSource;import net.minecraft.nbt.CompoundTag;
@Pseudo @Mixin(targets="com.ninni.spawn.server.block.AnthillBlock",remap=false)
public abstract class SpawnCareQuestAntFarmMixin { 
 @Redirect(method="m_213898_",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",remap=false),remap=false) private boolean jem$processed(ServerLevel level,BlockPos target,BlockState output,int flags,BlockState state,ServerLevel original,BlockPos farm,RandomSource random){var before=level.getBlockState(target);var setup=SpawnCareQuestSetups.get(level).at(level,farm);boolean accepted=level.setBlock(target,output,flags);if(accepted&&!target.equals(farm)&&before.getBlock()!=output.getBlock()&&level.getBlockState(target).equals(output)&&setup.hasUUID("actor")){var proof=new CompoundTag();proof.putString("block",SpawnCareQuestSetups.block(output));proof.putUUID("farm_actor",setup.getUUID("actor"));proof.putLong("farm",farm.asLong());SpawnCareQuestSetups.get(level).put(level,target,proof);}return accepted;}
 }
