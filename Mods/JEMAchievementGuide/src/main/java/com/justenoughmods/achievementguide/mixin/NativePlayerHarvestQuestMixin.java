package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.NativeHarvestWitness;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** Owns one original native call so optional observers do not redirect each other. */
@Mixin(ServerPlayerGameMode.class)
public abstract class NativePlayerHarvestQuestMixin {
 @Redirect(method="destroyBlock",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
 private void jem$nativeHarvest(Block block,Level world,Player actor,BlockPos pos,BlockState state,BlockEntity owner,ItemStack tool){
  Runnable operation=()->block.playerDestroy(world,actor,pos,state,owner,tool);
  if((Object)this instanceof NativeHarvestWitness.Nature witness){Runnable nativeCall=operation;operation=()->witness.jem$natureNativeHarvest(block,world,actor,pos,state,owner,tool,nativeCall);}
  if((Object)this instanceof NativeHarvestWitness.Caves witness){Runnable nativeCall=operation;operation=()->witness.jem$caveNativeHarvest(block,world,actor,pos,state,owner,tool,nativeCall);}
  operation.run();
 }
}
