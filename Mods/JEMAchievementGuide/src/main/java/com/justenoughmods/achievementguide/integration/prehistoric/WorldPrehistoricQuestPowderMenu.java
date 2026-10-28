package com.justenoughmods.achievementguide.integration.prehistoric;
import com.justenoughmods.achievementguide.mixin.WorldPrehistoricQuestFurnaceMenuAccess;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
public final class WorldPrehistoricQuestPowderMenu {
 public record Frame(ServerPlayer actor,AbstractContainerMenu menu,AbstractFurnaceBlockEntity owner,ItemStack output,CompoundTag receipt,int held){}
 private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
 private WorldPrehistoricQuestPowderMenu(){}
 public static int held(ServerPlayer p,AbstractContainerMenu m,ItemStack item){return WorldPrehistoricQuestProof.count(p.getInventory(),item)+(ItemStack.isSameItemSameTags(m.getCarried(),item)?m.getCarried().getCount():0);}
 public static void begin(AbstractContainerMenu menu,Player user){CURRENT.remove();var actor=WorldPrehistoricQuestProof.actor(user);if(actor==null || user.containerMenu!=menu || !menu.stillValid(user) || !(menu instanceof AbstractFurnaceMenu) || !(((WorldPrehistoricQuestFurnaceMenuAccess)menu).jem$prehistoricFurnace() instanceof AbstractFurnaceBlockEntity owner))return;if(owner.getLevel()!=actor.level())return;var output=owner.getItem(2);var receipt=owner.getPersistentData().getCompound(WorldPrehistoricQuestFurnace.KEY).copy();if(WorldPrehistoricQuestFurnace.matches(output,receipt) && receipt.getInt("proven")>0)CURRENT.set(new Frame(actor,menu,owner,output.copy(),receipt,held(actor,menu,output)));}
 public static void finish(AbstractContainerMenu menu,Player user){var f=CURRENT.get();CURRENT.remove();if(f==null || f.menu()!=menu || f.actor()!=user || WorldPrehistoricQuestProof.actor(user)!=f.actor() || user.containerMenu!=menu || !menu.stillValid(user))return;var now=f.owner().getItem(2);if(!now.isEmpty() && !ItemStack.isSameItemSameTags(now,f.output()))return;int taken=f.output().getCount()-now.getCount();if(taken<=0 || held(f.actor(),menu,f.output())-f.held()!=taken)return;int provenTaken=Math.max(0,taken-(f.output().getCount()-f.receipt().getInt("proven")));if(provenTaken>0)WorldPrehistoricQuestProof.emit(f.actor(),"v3_era_fuel_for_time_powder");if(now.isEmpty())f.owner().getPersistentData().remove(WorldPrehistoricQuestFurnace.KEY);else{var receipt=f.receipt().copy();receipt.put("item",now.save(new CompoundTag()));receipt.putInt("proven",Math.max(0,receipt.getInt("proven")-provenTaken));f.owner().getPersistentData().put(WorldPrehistoricQuestFurnace.KEY,receipt);}f.owner().setChanged();}
}
