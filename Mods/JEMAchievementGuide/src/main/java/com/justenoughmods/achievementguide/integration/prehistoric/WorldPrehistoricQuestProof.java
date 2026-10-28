package com.justenoughmods.achievementguide.integration.prehistoric;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;
public final class WorldPrehistoricQuestProof {
 public static final String ROOT="jemWorldPrehistoricV4";
 private WorldPrehistoricQuestProof(){}
 public static ServerPlayer actor(Entity e){return e instanceof ServerPlayer p && TravelProof.eligible(p)?p:null;}
 public static ServerPlayer actor(ServerLevel l,java.util.UUID uuid){return actor(l.getServer().getPlayerList().getPlayer(uuid));}
 public static boolean owned(Entity e,ServerPlayer p){return actor(p)!=null && e!=null && e.isAlive() && e.level()==p.level() && e instanceof TamableAnimal animal && animal.isTame() && p.getUUID().equals(animal.getOwnerUUID());}
 public static boolean hostile(Entity e,ServerPlayer p){return CaveQuestProof.hostile(e,p) && !(e instanceof TamableAnimal tame && tame.isTame());}
 public static void emit(Entity actor,String action){TravelProof.emit(actor(actor),action);}
 public static String id(ItemStack s){return String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));}
 public static String id(Entity e){return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()));}
 public static CompoundTag data(Entity e){return e.getPersistentData().getCompound(ROOT);}
 public static void mark(Entity e,String key,ServerPlayer p){var t=data(e);t.putUUID(key,p.getUUID());e.getPersistentData().put(ROOT,t);}
 public static int count(Container c,ItemStack s){int n=0;for(int i=0;i<c.getContainerSize();i++)if(ItemStack.isSameItemSameTags(c.getItem(i),s))n+=c.getItem(i).getCount();return n;}
 public static int countType(Container c,net.minecraft.world.item.Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
}
