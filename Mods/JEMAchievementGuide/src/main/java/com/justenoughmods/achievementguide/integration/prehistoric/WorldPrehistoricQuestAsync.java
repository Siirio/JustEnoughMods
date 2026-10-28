package com.justenoughmods.achievementguide.integration.prehistoric;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
/** Wraps the native scheduled work without adding a task, deadline, random roll, or gameplay decision. */
public final class WorldPrehistoricQuestAsync {
 public static final class Job {public final ServerPlayer actor;public final Entity source;public final String kind;public final net.minecraft.world.item.Item paidItem;public final int paidBefore;public final double digBefore;public boolean committed;public Job(ServerPlayer actor,Entity source,String kind,net.minecraft.world.item.Item paidItem,int paidBefore,double digBefore){this.actor=actor;this.source=source;this.kind=kind;this.paidItem=paidItem;this.paidBefore=paidBefore;this.digBefore=digBefore;}}
 private static final ThreadLocal<Job> SCHEDULE=new ThreadLocal<>();private static final ThreadLocal<Job> EXECUTING=new ThreadLocal<>();
 private static final ThreadLocal<Integer> GROWTH=new ThreadLocal<>();
 private WorldPrehistoricQuestAsync(){}
 public static void beginDig(Entity source,Entity actor){SCHEDULE.remove();var p=WorldPrehistoricQuestProof.actor(actor);if(p!=null && WorldPrehistoricQuestProof.owned(source,p))SCHEDULE.set(new Job(p,source,"dig",null,0,source.getPersistentData().getDouble("DigTimer")));}
 public static void beginSeed(Entity actor,ItemStack stack){SCHEDULE.remove();var p=WorldPrehistoricQuestProof.actor(actor);if(p!=null && WorldPrehistoricQuestProof.id(stack).equals("unearthed_journey:platypodium_seed_pods"))SCHEDULE.set(new Job(p,p,"seed",stack.getItem(),WorldPrehistoricQuestProof.countType(p.getInventory(),stack.getItem()),0));}
 public static void finishDig(){var j=SCHEDULE.get();SCHEDULE.remove();if(j!=null && WorldPrehistoricQuestProof.owned(j.source,j.actor) && j.digBefore<=0 && j.source.getPersistentData().getDouble("DigTimer")>0)j.committed=true;}
 public static void finishSeed(ItemStack stack){var j=SCHEDULE.get();SCHEDULE.remove();if(j!=null && WorldPrehistoricQuestProof.actor(j.actor)!=null && j.paidBefore-WorldPrehistoricQuestProof.countType(j.actor.getInventory(),j.paidItem)==1)j.committed=true;}
 public static Runnable nativeWork(Runnable original){var job=SCHEDULE.get();if(job==null)job=EXECUTING.get();if(job==null)return original;final Job captured=job;return ()->{var previous=EXECUTING.get();EXECUTING.set(captured);try{original.run();}finally{if(previous==null)EXECUTING.remove();else EXECUTING.set(previous);}};}
 public static void accepted(Entity entity){var j=EXECUTING.get();if(j==null || !j.committed || !j.kind.equals("dig") || !(entity instanceof ItemEntity drop) || drop.getItem().isEmpty() || !WorldPrehistoricQuestProof.owned(j.source,j.actor) || entity.level()!=j.actor.level())return;var t=new CompoundTag();t.putUUID("actor",j.actor.getUUID());t.putUUID("source",j.source.getUUID());t.putString("kind","dig");t.put("output",drop.getItem().save(new CompoundTag()));drop.getPersistentData().put(WorldPrehistoricQuestProof.ROOT,t);}
 public static void beginGrowth(){GROWTH.set(0);}
 public static void changed(net.minecraft.world.level.block.state.BlockState before,net.minecraft.world.level.block.state.BlockState after){var n=GROWTH.get();if(n!=null && before.getBlock()!=after.getBlock() && !after.isAir())GROWTH.set(n+1);}
 public static void finishGrowth(boolean accepted){var n=GROWTH.get();GROWTH.remove();var j=EXECUTING.get();if(accepted && n!=null && n>0 && j!=null && j.committed && j.kind.equals("seed") && WorldPrehistoricQuestProof.actor(j.actor)!=null)WorldPrehistoricQuestProof.emit(j.actor,"v3_era_fuel_for_time_seed");}
}
