package com.justenoughmods.achievementguide.integration.petutilities;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent.FarmlandTrample;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import java.util.List;
public final class PetUtilityQuestHay {
 public static final class Query {final ServerPlayer actor;List<? extends Entity> result=List.of();Query(ServerPlayer actor){this.actor=actor;}}
 private static final ThreadLocal<Query> QUERY=new ThreadLocal<>();
 private static FarmlandTrample protection;
 private PetUtilityQuestHay(){}
 public static void registered(FarmlandTrample callback){protection=callback;}
 public static void query(ServerLevel level,BlockPos farmland,ServerPlayer actor){if(protection==null || PetUtilityQuestProof.actor(actor)==null || actor.level()!=level || !level.getBlockState(farmland).is(net.minecraft.world.level.block.Blocks.FARMLAND))return;Query previous=QUERY.get(),query=new Query(actor);QUERY.set(query);EventResult result;try{result=protection.trample(level,farmland,level.getBlockState(farmland),0,actor);}finally{if(previous==null)QUERY.remove();else QUERY.set(previous);}if(result.interruptsFurtherEvaluation() && result.isFalse())for(var hay:query.result)if(hay.isAlive() && PetUtilityQuestProof.actor(hay,"hay")==actor){PetUtilityQuestProof.emit(actor,"v4_pet_hay_protection");break;}}
 public static void established(ServerPlayer p){var list=p.getPersistentData().getList("jemPetUtilityCropsV4",10);for(int i=0;i<list.size();i++){var t=list.getCompound(i);if(!t.getString("dimension").equals(p.level().dimension().location().toString()))continue;var crop=BlockPos.of(t.getLong("pos"));if(!p.serverLevel().hasChunkAt(crop))continue;var state=p.serverLevel().getBlockState(crop);if(state.is(net.minecraft.tags.BlockTags.CROPS) && PetUtilityQuestPlacement.planter(p.serverLevel(),crop,state)==p)query(p.serverLevel(),crop.below(),p);}}
 public static void found(List<? extends Entity> nativeResult){var q=QUERY.get();if(q!=null)q.result=nativeResult;}
}
