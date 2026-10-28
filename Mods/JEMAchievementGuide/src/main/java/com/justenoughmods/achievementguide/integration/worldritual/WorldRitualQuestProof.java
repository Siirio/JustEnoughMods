package com.justenoughmods.achievementguide.integration.worldritual;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
public final class WorldRitualQuestProof {
    private WorldRitualQuestProof(){}
    public static boolean real(Entity actor){return actor instanceof ServerPlayer player&&TravelProof.eligible(player);}
    public static String item(ItemStack stack){return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
    public static boolean natural(Entity entity){return !entity.getPersistentData().getBoolean("jem:event_spawned")&&!entity.getPersistentData().getCompound("jem_world_boss_tiers").getString("Provenance").equals("RAID_EVENT");}
    public static void emit(Entity actor,String action){if(real(actor))QuestActionTrigger.INSTANCE.emit((ServerPlayer)actor,action);}
    public static int held(ServerPlayer actor,ItemStack sample){int n=0;for(int i=0;i<actor.getInventory().getContainerSize();i++){ItemStack stack=actor.getInventory().getItem(i);if(ItemStack.isSameItemSameTags(sample,stack))n+=stack.getCount();}if(ItemStack.isSameItemSameTags(sample,actor.containerMenu.getCarried()))n+=actor.containerMenu.getCarried().getCount();return n;}
}
