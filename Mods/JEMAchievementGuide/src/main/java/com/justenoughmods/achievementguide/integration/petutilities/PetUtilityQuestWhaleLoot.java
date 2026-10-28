package com.justenoughmods.achievementguide.integration.petutilities;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
public final class PetUtilityQuestWhaleLoot {
 public record Source(net.minecraft.world.inventory.Slot slot,ItemStack item,CompoundTag receipt,int count,int held){}
 public record Frame(AbstractContainerMenu menu,ServerPlayer actor,List<Source> sources){}
 private static final ThreadLocal<Frame> FRAME=new ThreadLocal<>();
 private PetUtilityQuestWhaleLoot(){}
 public static boolean widget(ItemStack s){String id=PetUtilityQuestProof.id(s);return id.equals("whaleborne:cannon") || id.equals("whaleborne:anchor");}
 public static void loot(ItemStack s,String table,UUID chest){if(widget(s) && (table.equals("minecraft:chests/shipwreck_supply") || table.equals("minecraft:chests/buried_treasure"))){var t=new CompoundTag();t.putString("table",table);PetUtilityQuestStack.put(s,chest,chest,"unclaimed_widget",t);}}
 public static int held(ServerPlayer p,AbstractContainerMenu m,ItemStack s){return PetUtilityQuestProof.count(p.getInventory(),s)+(ItemStack.isSameItemSameTags(m.getCarried(),s)?m.getCarried().getCount():0);}
 public static List<ItemStack> items(ServerPlayer p,AbstractContainerMenu m){List<ItemStack> result=new ArrayList<>();for(int i=0;i<p.getInventory().getContainerSize();i++)result.add(p.getInventory().getItem(i));result.add(m.getCarried());return result;}
 public static void begin(AbstractContainerMenu m,Player p){FRAME.remove();var actor=PetUtilityQuestProof.actor(p);if(actor==null || p.containerMenu!=m || !m.stillValid(p) || m.slots.size()>256)return;List<Source> sources=new ArrayList<>();for(var slot:m.slots)if(slot.container!=p.getInventory()){var s=slot.getItem();var receipt=PetUtilityQuestStack.get(s);if(widget(s) && receipt.getString("kind").equals("unclaimed_widget"))sources.add(new Source(slot,s.copy(),receipt,s.getCount(),held(actor,m,s)));}FRAME.set(new Frame(m,actor,List.copyOf(sources)));}
 public static void finish(AbstractContainerMenu m,Player p){var f=FRAME.get();FRAME.remove();if(f==null || f.menu()!=m || f.actor()!=p || PetUtilityQuestProof.actor(p)!=p || p.containerMenu!=m || !m.stillValid(p))return;for(var row:f.sources()){int taken=row.count()-(ItemStack.isSameItemSameTags(row.slot().getItem(),row.item())?row.slot().getItem().getCount():0);if(taken<=0 || held(f.actor(),m,row.item())-row.held()!=taken)continue;var fresh=items(f.actor(),m).stream().filter(s->{var tag=PetUtilityQuestStack.get(s);return tag.getString("kind").equals("unclaimed_widget") && tag.getUUID("source").equals(row.receipt().getUUID("source")) && ItemStack.isSameItemSameTags(s,row.item());}).toList();if(fresh.size()==1 && fresh.get(0).getCount()==taken)PetUtilityQuestStack.put(fresh.get(0),f.actor().getUUID(),row.receipt().getUUID("source"),"looted_widget",row.receipt().getCompound("payload"));}}
}
