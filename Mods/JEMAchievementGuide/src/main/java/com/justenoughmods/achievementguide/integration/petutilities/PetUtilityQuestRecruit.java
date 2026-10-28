package com.justenoughmods.achievementguide.integration.petutilities;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.*;
import com.talhanation.recruits.RecruitEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.*;
public final class PetUtilityQuestRecruit {
 public record Hire(ServerPlayer actor,boolean owned,long generation){}
 public record Order(AbstractRecruitEntity recruit,int follow,net.minecraft.core.BlockPos move,boolean shouldMove){}
 private PetUtilityQuestRecruit(){}
 public static boolean owned(AbstractRecruitEntity recruit,ServerPlayer p){return PetUtilityQuestProof.actor(p)!=null && recruit.isAlive() && recruit.isOwned() && p.getUUID().equals(recruit.getOwnerUUID()) && p.level()==recruit.level();}
 public static Hire before(AbstractRecruitEntity r,Player p){return new Hire(PetUtilityQuestProof.actor(p),r.isOwned(),r.getPersistentData().getLong("JemCoinHireGeneration"));}
 public static void hired(AbstractRecruitEntity r,Hire f,boolean paid){if(f!=null && !f.owned() && owned(r,f.actor()) && paid){PetUtilityQuestProof.put(r,"hire",f.actor().getUUID());PetUtilityQuestProof.emit(f.actor(),"v4_pet_recruit_hired");}}
 public static void group(ServerPlayer p,RecruitsGroupsManager manager,RecruitsGroup incoming){if(PetUtilityQuestProof.actor(p)==null || incoming==null || incoming.removed || !p.getUUID().equals(incoming.getPlayerUUID()) || manager.getGroup(incoming.getUUID())!=incoming || incoming.members.size()>4096)return;for(var uuid:incoming.members)if(p.serverLevel().getEntity(uuid) instanceof AbstractRecruitEntity recruit && owned(recruit,p) && incoming.getUUID().equals(recruit.getGroup())){PetUtilityQuestProof.emit(p,"v4_pet_recruit_group_saved");return;}}
 public static List<Order> orders(ServerPlayer p,List<AbstractRecruitEntity> recruits){List<Order> rows=new ArrayList<>();if(PetUtilityQuestProof.actor(p)==null || recruits.size()>4096)return rows;for(var r:recruits)if(owned(r,p) && r.isEffectedByCommand(p.getUUID()))rows.add(new Order(r,r.getFollowState(),r.getMovePos(),r.getShouldMovePos()));return rows;}
 public static void ordered(ServerPlayer p,List<Order> before){if(PetUtilityQuestProof.actor(p)==null || before==null)return;for(var row:before)if(owned(row.recruit(),p) && row.recruit().isEffectedByCommand(p.getUUID()) && (row.follow()!=row.recruit().getFollowState() || !Objects.equals(row.move(),row.recruit().getMovePos()) || row.shouldMove()!=row.recruit().getShouldMovePos())){PetUtilityQuestProof.emit(p,"v3_pet_clear_orders");return;}}
}
