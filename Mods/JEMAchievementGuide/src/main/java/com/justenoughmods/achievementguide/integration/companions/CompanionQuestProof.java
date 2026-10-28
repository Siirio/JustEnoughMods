package com.justenoughmods.achievementguide.integration.companions;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;
public final class CompanionQuestProof {
 private CompanionQuestProof(){}
 public static ServerPlayer actor(Entity e){return e instanceof ServerPlayer p && TravelProof.eligible(p)?p:null;}
 public static ServerPlayer actor(ServerLevel level,UUID id){return id==null?null:actor(level.getServer().getPlayerList().getPlayer(id));}
 public static void emit(Entity e,String action){var p=actor(e);if(p!=null)QuestActionTrigger.INSTANCE.emit(p,action);}
 public static String id(ItemStack s){return String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));}
 public static boolean item(ItemStack s,String id){return !s.isEmpty() && id(s).equals(id);}
 public static boolean owned(net.minecraft.world.entity.TamableAnimal pet,Player actor){return pet.isTame() && actor.getUUID().equals(pet.getOwnerUUID());}
 public static boolean hostile(Entity target,Entity actor){return actor!=null && target instanceof LivingEntity && !(target instanceof Player) && !target.isAlliedTo(actor) && (target instanceof Enemy || target.getType().getCategory()==MobCategory.MONSTER);}
 public static boolean mage(ServerPlayer p){return p!=null && dev.xylonity.companions.common.util.Util.hasFullSetOn(p,dev.xylonity.companions.common.material.ArmorMaterials.MAGE.get())>0;}
 public static boolean loaded(ServerLevel l,BlockPos p){return l.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
 public static boolean permitted(ServerPlayer p,BlockPos pos){return p!=null && loaded(p.serverLevel(),pos) && p.mayBuild() && p.serverLevel().mayInteract(p,pos);}
 public static int count(net.minecraft.world.Container inv,ItemStack kind){if(inv.getContainerSize()>256)return -1;int n=0;for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(ItemStack.isSameItemSameTags(s,kind))n+=s.getCount();}return n;}
 public static boolean holy(ItemStack stack){return stack.getItem() instanceof ArmorItem armor && armor.getMaterial()==dev.xylonity.companions.common.material.ArmorMaterials.HOLY_ROBE.get();}
}
