package com.justenoughmods.achievementguide.integration.petutilities;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import java.util.UUID;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;
/** Physical source attribution only. Native code remains the gameplay owner. */
public final class PetUtilityQuestProof {
 public static final String ROOT="jemPetUtilityV4";
 private PetUtilityQuestProof(){}
 public static ServerPlayer actor(Player p){return p instanceof ServerPlayer s && TravelProof.eligible(s)?s:null;}
 public static ServerPlayer actor(Entity e,String key){var t=e.getPersistentData().getCompound(ROOT);return e.level() instanceof ServerLevel l && t.hasUUID(key)?actor(l.getServer().getPlayerList().getPlayer(t.getUUID(key))):null;}
 public static CompoundTag data(Entity e){return e.getPersistentData().getCompound(ROOT);}
 public static void put(Entity e,String key,UUID actor){var t=data(e);t.putUUID(key,actor);e.getPersistentData().put(ROOT,t);}
 public static String id(ItemStack s){return String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));}
 public static String id(Entity e){return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()));}
 public static void emit(ServerPlayer p,String action){TravelProof.emit(p,action);}
 public static int count(Container c,ItemStack s){int n=0;for(int i=0;i<c.getContainerSize();i++)if(ItemStack.isSameItemSameTags(c.getItem(i),s))n+=c.getItem(i).getCount();return n;}
 public static boolean safe(Entity e){return e.isAlive() && !e.isPassenger() && e.onGround() && e.level().noCollision(e) && !e.isInWaterOrBubble() && !e.isInLava();}
}
