package com.justenoughmods.achievementguide.integration.petutilities;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
/** A native placement and its accepted spawns must finish with the actual item debit. */
public final class PetUtilityQuestPlacement {
 public record Frame(ServerPlayer actor,BlockPos pos,Block block,int count,List<Entity> spawns){}
 private static final ThreadLocal<ArrayDeque<Frame>> FRAMES=ThreadLocal.withInitial(ArrayDeque::new);
 private PetUtilityQuestPlacement(){}
 public static net.minecraft.world.InteractionResult observe(BlockPlaceContext c,Block b,java.util.function.Supplier<net.minecraft.world.InteractionResult> operation){begin(c,b);boolean accepted=false;try{var result=operation.get();accepted=result.consumesAction();return result;}finally{finish(c,accepted);}}
 public static void begin(BlockPlaceContext c,Block b){var p=PetUtilityQuestProof.actor(c.getPlayer());FRAMES.get().push(new Frame(p,c.getClickedPos().immutable(),b,c.getItemInHand().getCount(),new ArrayList<>()));}
 public static Frame current(){return FRAMES.get().peek();}
 public static void accepted(Entity e){var f=current();if(f!=null && f.actor()!=null && f.actor().level()==e.level() && f.spawns().size()<32 && (PetUtilityQuestProof.id(e).equals("golemoverhaul:hay_golem") || PetUtilityQuestProof.id(e).equals("golemoverhaul:terracotta_golem")))f.spawns().add(e);}
 public static void finish(BlockPlaceContext c,boolean accepted){var frames=FRAMES.get();if(frames.isEmpty())return;var f=frames.pop();if(frames.isEmpty())FRAMES.remove();if(!accepted || f.actor()==null || PetUtilityQuestProof.actor(c.getPlayer())!=f.actor() || f.count()-c.getItemInHand().getCount()!=1)return;
  for(var e:f.spawns())if(e.isAlive() && e.level()==f.actor().level() && f.actor().serverLevel().getEntity(e.getUUID())==e){if(PetUtilityQuestProof.id(e).endsWith(":hay_golem")){PetUtilityQuestProof.put(e,"hay",f.actor().getUUID());PetUtilityQuestHay.established(f.actor());}else if(String.valueOf(ForgeRegistries.BLOCKS.getKey(f.block())).equals("golemoverhaul:clay_golem_statue"))PetUtilityQuestProof.put(e,"clay",f.actor().getUUID());}
  var state=f.actor().serverLevel().getBlockState(f.pos());if(state.getBlock()!=f.block())return;
  if(!state.is(net.minecraft.tags.BlockTags.CROPS) && !String.valueOf(ForgeRegistries.BLOCKS.getKey(f.block())).equals("golemoverhaul:clay_golem_statue"))return;
  var tag=new CompoundTag();tag.putUUID("actor",f.actor().getUUID());tag.putString("block",String.valueOf(ForgeRegistries.BLOCKS.getKey(f.block())));
  PetUtilityQuestBlockOrigins.put(f.actor().serverLevel(),f.pos(),"Placed",tag);
  if(state.is(net.minecraft.tags.BlockTags.CROPS)){var crops=f.actor().getPersistentData().getList("jemPetUtilityCropsV4",10);var crop=new CompoundTag();crop.putString("dimension",f.actor().level().dimension().location().toString());crop.putLong("pos",f.pos().asLong());if(crops.size()>=32)crops.remove(0);crops.add(crop);f.actor().getPersistentData().put("jemPetUtilityCropsV4",crops);if(net.minecraftforge.fml.ModList.get().isLoaded("golemoverhaul"))PetUtilityQuestHay.query(f.actor().serverLevel(),f.pos().below(),f.actor());}
 }
 public static ServerPlayer planter(ServerLevel level,BlockPos pos,BlockState state){var t=PetUtilityQuestBlockOrigins.get(level,pos,"Placed");return t.hasUUID("actor") && t.getString("block").equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())))?PetUtilityQuestProof.actor(level.getServer().getPlayerList().getPlayer(t.getUUID("actor"))):null;}
}
