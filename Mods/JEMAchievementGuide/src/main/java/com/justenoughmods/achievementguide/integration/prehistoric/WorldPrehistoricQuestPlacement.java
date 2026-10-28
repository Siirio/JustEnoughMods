package com.justenoughmods.achievementguide.integration.prehistoric;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;
public final class WorldPrehistoricQuestPlacement {
 private WorldPrehistoricQuestPlacement(){}
 public static void placed(BlockItem item,BlockPlaceContext context,int count,InteractionResult result){var p=WorldPrehistoricQuestProof.actor(context.getPlayer());if(p!=null && result.consumesAction() && count-context.getItemInHand().getCount()==1 && String.valueOf(ForgeRegistries.BLOCKS.getKey(item.getBlock())).equals("unearthed_journey:peat_pie") && p.serverLevel().getBlockState(context.getClickedPos()).getBlock()==item.getBlock()){var t=new CompoundTag();t.putUUID("actor",p.getUUID());t.putString("block","unearthed_journey:peat_pie");WorldPrehistoricQuestOrigins.put(p.serverLevel(),context.getClickedPos(),"Placed",t);}}
}
