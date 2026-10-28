package com.justenoughmods.achievementguide.integration.companions;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.api.interop.GreatChaliceState;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class CompanionQuestGrail {
 public static final class Run {public final ServerPlayer actor;public final GreatChaliceBlockEntity owner;public final ItemStack empty;public final int filled;public boolean rewardScope;public final List<ItemEntity> drops=new ArrayList<>();Run(ServerPlayer actor,GreatChaliceBlockEntity owner,ItemStack empty,int filled){this.actor=actor;this.owner=owner;this.empty=empty;this.filled=filled;}}
 private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();private CompanionQuestGrail(){}
 public static void begin(Player p,InteractionHand hand,GreatChaliceBlockEntity owner){CURRENT.remove();var actor=CompanionQuestProof.actor(p);var stack=p.getItemInHand(hand);if(actor!=null && owner!=null && owner.getLevel()==actor.level() && owner.isFull() && owner.getState()==GreatChaliceState.NORMAL && stack.is(KnightLibItems.EMPTY_GRAIL.get()))CURRENT.set(new Run(actor,owner,stack.copy(),CompanionQuestProof.count(actor.getInventory(),new ItemStack(KnightLibItems.FILLED_GRAIL.get()))));}
 public static void reward(Runnable nativeCall){var run=CURRENT.get();if(run!=null)run.rewardScope=true;try{nativeCall.run();}finally{if(run!=null)run.rewardScope=false;}}
 public static void accepted(Entity entity,boolean accepted){var run=CURRENT.get();if(run!=null && run.rewardScope && accepted && entity instanceof ItemEntity item && item.getItem().is(KnightLibItems.FILLED_GRAIL.get()) && run.drops.size()<8)run.drops.add(item);}
 public static void end(Player p,InteractionHand hand){var run=CURRENT.get();CURRENT.remove();if(run==null || run.actor!=p || run.owner.getCharges()!=0)return;var now=p.getItemInHand(hand);int remaining=now.is(KnightLibItems.EMPTY_GRAIL.get())?now.getCount():0;if(run.empty.getCount()-remaining!=1)return;
  boolean output=CompanionQuestProof.count(run.actor.getInventory(),new ItemStack(KnightLibItems.FILLED_GRAIL.get()))-run.filled==1;
  for(var item:run.drops)if(!item.isRemoved() && run.actor.serverLevel().getEntity(item.getUUID())==item && item.getItem().is(KnightLibItems.FILLED_GRAIL.get()) && item.getItem().getCount()==1)output=true;
  if(output)CompanionQuestProof.emit(run.actor,"v3_pet_grail_filling");
 }
}
