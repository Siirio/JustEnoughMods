package com.justenoughmods.achievementguide.integration.petutilities;
import com.talhanation.recruits.inventory.RecruitInventoryMenu;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
public final class PetUtilityQuestRecruitMenu {
 public record Frame(ServerPlayer actor,AbstractContainerMenu menu,AbstractRecruitEntity recruit,ItemStack[] inventory,ItemStack[] held){}
 private static final ThreadLocal<Frame> FRAME=new ThreadLocal<>();
 private PetUtilityQuestRecruitMenu(){}
 public static ItemStack[] held(ServerPlayer p,AbstractContainerMenu m){ItemStack[] stacks=new ItemStack[p.getInventory().getContainerSize()+1];for(int i=0;i<stacks.length-1;i++)stacks[i]=p.getInventory().getItem(i).copy();stacks[stacks.length-1]=m.getCarried().copy();return stacks;}
 public static int count(ItemStack[] stacks,ItemStack item){int n=0;for(var s:stacks)if(ItemStack.isSameItemSameTags(s,item))n+=s.getCount();return n;}
 public static void begin(AbstractContainerMenu menu,Player p){FRAME.remove();var actor=PetUtilityQuestProof.actor(p);if(actor==null || p.containerMenu!=menu || !menu.stillValid(p) || !(menu instanceof RecruitInventoryMenu nativeMenu))return;var recruit=nativeMenu.getRecruit();if(!PetUtilityQuestRecruit.owned(recruit,actor) || PetUtilityQuestProof.actor(recruit,"hire")!=actor)return;var inv=recruit.getInventory();ItemStack[] before=new ItemStack[inv.getContainerSize()];for(int i=0;i<before.length;i++)before[i]=inv.getItem(i).copy();FRAME.set(new Frame(actor,menu,recruit,before,held(actor,menu)));}
 public static void finish(AbstractContainerMenu menu,Player p){var f=FRAME.get();FRAME.remove();if(f==null || f.menu()!=menu || f.actor()!=p || p.containerMenu!=menu || !menu.stillValid(p) || !PetUtilityQuestRecruit.owned(f.recruit(),f.actor()) || PetUtilityQuestProof.actor(f.recruit(),"hire")!=f.actor())return;var recruit=f.recruit();var inv=recruit.getInventory();var afterHeld=held(f.actor(),menu);for(int i=6;i<inv.getContainerSize();i++){var stack=inv.getItem(i);if(stack.isEmpty() || !recruit.canEatItemStack(stack))continue;int old=count(f.inventory(),stack),now=PetUtilityQuestProof.count(inv,stack),increase=now-old;if(increase>0 && count(f.held(),stack)-count(afterHeld,stack)==increase)PetUtilityQuestProof.put(recruit,"food",p.getUUID());}
  var shield=inv.getItem(4);if(shield.getItem() instanceof ShieldItem && f.inventory()[4].isEmpty() && shield.getCount()==1 && ItemStack.matches(shield,recruit.getOffhandItem()) && count(f.held(),shield)-count(afterHeld,shield)==1)PetUtilityQuestProof.put(recruit,"shield",p.getUUID());
  if(PetUtilityQuestProof.actor(recruit,"food")==f.actor() && PetUtilityQuestProof.actor(recruit,"shield")==f.actor() && java.util.stream.IntStream.range(6,inv.getContainerSize()).anyMatch(i->!inv.getItem(i).isEmpty() && recruit.canEatItemStack(inv.getItem(i))) && recruit.getOffhandItem().getItem() instanceof ShieldItem)PetUtilityQuestProof.emit(f.actor(),"v3_pet_first_recruit");
 }
}
