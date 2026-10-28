package com.justenoughmods.achievementguide.integration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Optional native observers around the single real player-harvest invocation. */
public final class NativeHarvestWitness {
 private NativeHarvestWitness() {}
 public interface Nature {
  void jem$natureNativeHarvest(Block block,Level world,Player actor,BlockPos pos,BlockState state,BlockEntity owner,ItemStack tool,Runnable original);
 }
 public interface Caves {
  void jem$caveNativeHarvest(Block block,Level world,Player actor,BlockPos pos,BlockState state,BlockEntity owner,ItemStack tool,Runnable original);
 }
}
