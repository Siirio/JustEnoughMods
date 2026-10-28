package com.justenoughmods.achievementguide.integration.prehistoric;
import com.justenoughmods.achievementguide.mixin.WorldPrehistoricQuestSatchelAccess;
import net.mcreator.unearthedjourney.entity.CarbonemysEntity;
import net.mcreator.unearthedjourney.world.inventory.LargeHideSatchelGUIMenu;
import net.mcreator.unearthedjourney.init.UnearthedJourneyModMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
public final class WorldPrehistoricQuestTurtle {
 public record Equip(ServerPlayer actor,Entity target,ItemStack input,int count,boolean satchel){}
 public record Menu(ServerPlayer actor,AbstractContainerMenu menu,CarbonemysEntity turtle,IItemHandler storage,ItemStack[] before,ItemStack[] held){}
 private static final ThreadLocal<Equip> EQUIP=new ThreadLocal<>();private static final ThreadLocal<Menu> MENU=new ThreadLocal<>();
 private WorldPrehistoricQuestTurtle(){}
 public static boolean satchel(CarbonemysEntity turtle){var effect=turtle.getEffect(UnearthedJourneyModMobEffects.LARGE_SATCHEL_EFFECT.get());return effect!=null && effect.getAmplifier()>=1;}
 public static void equipBefore(Entity entity,Entity source){EQUIP.remove();var actor=WorldPrehistoricQuestProof.actor(source);if(actor!=null && entity instanceof CarbonemysEntity turtle && WorldPrehistoricQuestProof.owned(turtle,actor)){var input=actor.getMainHandItem();if(WorldPrehistoricQuestProof.id(input).equals("unearthed_journey:hide_satchel"))EQUIP.set(new Equip(actor,turtle,input,input.getCount(),satchel(turtle)));}}
 public static void equipAfter(){var f=EQUIP.get();EQUIP.remove();if(f!=null && f.target() instanceof CarbonemysEntity turtle && !f.satchel() && satchel(turtle) && WorldPrehistoricQuestProof.owned(turtle,f.actor()) && f.count()-f.input().getCount()==1)WorldPrehistoricQuestProof.mark(turtle,"satchel",f.actor());}
 public static ItemStack[] held(ServerPlayer p,AbstractContainerMenu m){ItemStack[] items=new ItemStack[p.getInventory().getContainerSize()+1];for(int i=0;i<items.length-1;i++)items[i]=p.getInventory().getItem(i).copy();items[items.length-1]=m.getCarried().copy();return items;}
 public static int count(ItemStack[] array,ItemStack item){int n=0;for(var s:array)if(ItemStack.isSameItemSameTags(s,item))n+=s.getCount();return n;}
 public static int count(IItemHandler storage,ItemStack item){int n=0;for(int i=0;i<storage.getSlots();i++)if(ItemStack.isSameItemSameTags(storage.getStackInSlot(i),item))n+=storage.getStackInSlot(i).getCount();return n;}
 public static void begin(AbstractContainerMenu menu,Player user){MENU.remove();var actor=WorldPrehistoricQuestProof.actor(user);if(actor==null || user.containerMenu!=menu || !menu.stillValid(user) || !(menu instanceof LargeHideSatchelGUIMenu))return;var access=(WorldPrehistoricQuestSatchelAccess)menu;if(!(access.jem$prehistoricBound() instanceof CarbonemysEntity turtle) || !WorldPrehistoricQuestProof.owned(turtle,actor) || !satchel(turtle))return;var receipt=WorldPrehistoricQuestProof.data(turtle);if(!receipt.hasUUID("satchel"))return;var storage=access.jem$prehistoricStorage();if(storage==null || storage.getSlots()!=36 || turtle.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null)!=storage)return;ItemStack[] before=new ItemStack[storage.getSlots()];for(int i=0;i<before.length;i++)before[i]=storage.getStackInSlot(i).copy();MENU.set(new Menu(actor,menu,turtle,storage,before,held(actor,menu)));}
 public static void finish(AbstractContainerMenu menu,Player user){var f=MENU.get();MENU.remove();if(f==null || f.menu()!=menu || f.actor()!=user || user.containerMenu!=menu || !menu.stillValid(user) || !WorldPrehistoricQuestProof.owned(f.turtle(),f.actor()) || !satchel(f.turtle()) || ((WorldPrehistoricQuestSatchelAccess)menu).jem$prehistoricBound()!=f.turtle() || ((WorldPrehistoricQuestSatchelAccess)menu).jem$prehistoricStorage()!=f.storage())return;var afterHeld=held(f.actor(),menu);for(var item:f.before())if(!item.isEmpty()){int taken=count(f.before(),item)-count(f.storage(),item);if(taken>0 && count(afterHeld,item)-count(f.held(),item)==taken){WorldPrehistoricQuestProof.emit(f.actor(),"v3_era_turtle_cargo");return;}}}
}
