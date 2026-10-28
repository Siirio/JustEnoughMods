package com.justenoughmods.achievementguide.integration.prehistoric;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class WorldPrehistoricQuestDrops {
 public record Break(ServerPlayer actor,BlockPos pos,CompoundTag origin){}
 public record Shear(ServerPlayer actor,Entity source,boolean before,List<ItemEntity> drops){}
 private static final ThreadLocal<Break> BREAK=new ThreadLocal<>();private static final ThreadLocal<Break> DROPS=new ThreadLocal<>();private static final ThreadLocal<Shear> SHEAR=new ThreadLocal<>();
 private WorldPrehistoricQuestDrops(){}
 public static void beginBreak(ServerPlayer p,BlockPos pos){BREAK.remove();if(WorldPrehistoricQuestProof.actor(p)==null)return;var origin=WorldPrehistoricQuestOrigins.get(p.serverLevel(),pos,"Dried");if(origin.hasUUID("actor") && p.serverLevel().getBlockState(pos).is(net.mcreator.unearthedjourney.init.UnearthedJourneyModBlocks.DRIED_PEAT_PIE.get()))BREAK.set(new Break(p,pos.immutable(),origin));}
 public static void endBreak(){BREAK.remove();}
 public static void harvest(Level level,BlockPos pos,Entity user,BlockState state,Runnable original){var f=BREAK.get();if(f==null || f.actor()!=user || f.actor().level()!=level || !f.pos().equals(pos) || !state.is(net.mcreator.unearthedjourney.init.UnearthedJourneyModBlocks.DRIED_PEAT_PIE.get())){original.run();return;}DROPS.set(f);try{original.run();}finally{DROPS.remove();}}
 public static void beginShear(ServerPlayer actor,Entity source,boolean before){SHEAR.remove();if(actor!=null)SHEAR.set(new Shear(actor,source,before,new ArrayList<>()));}
 public static void finishShear(boolean sheared){var f=SHEAR.get();SHEAR.remove();if(f!=null && !f.before() && sheared && f.source().isAlive() && WorldPrehistoricQuestProof.actor(f.actor())!=null && !f.drops().isEmpty())WorldPrehistoricQuestProof.emit(f.actor(),"v3_era_soft_landing");}
 public static void accepted(Entity e){WorldPrehistoricQuestAsync.accepted(e);if(!(e instanceof ItemEntity item) || item.getItem().isEmpty())return;var f=DROPS.get();if(f!=null && WorldPrehistoricQuestProof.id(item.getItem()).equals("unearthed_journey:dried_peat_pie")){var t=f.origin().copy();t.putString("kind","dried_peat");t.put("output",item.getItem().save(new CompoundTag()));item.getPersistentData().put(WorldPrehistoricQuestProof.ROOT,t);}var shear=SHEAR.get();if(shear!=null && !shear.before() && item.getItem().is(net.minecraft.world.item.Items.FEATHER) && shear.drops().size()<16)shear.drops().add(item);}
 public static void picked(ItemEntity e,ServerPlayer actor,CompoundTag receipt,ItemStack actualBefore){if(actor==null || receipt==null || !receipt.getCompound("output").equals(actualBefore.save(new CompoundTag())) || !receipt.hasUUID("actor") || !receipt.getUUID("actor").equals(actor.getUUID()))return;if(receipt.getString("kind").equals("dried_peat"))WorldPrehistoricQuestProof.emit(actor,"v3_era_fuel_for_time");if(receipt.getString("kind").equals("dig") && receipt.hasUUID("source") && WorldPrehistoricQuestProof.owned(actor.serverLevel().getEntity(receipt.getUUID("source")),actor))WorldPrehistoricQuestProof.emit(actor,"v3_era_little_excavator");}
}
