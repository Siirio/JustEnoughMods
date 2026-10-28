package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Block.class)
public abstract class WorldPrehistoricQuestHarvestMixin {
 @Redirect(method="playerDestroy",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",remap=false))private void jem$nativeDrops(BlockState state,Level level,BlockPos pos,BlockEntity owner,Entity actor,ItemStack tool,boolean dropExperience){WorldPrehistoricQuestDrops.harvest(level,pos,actor,state,()->Block.dropResources(state,level,pos,owner,actor,tool,dropExperience));}
}
