package com.justenoughmods.achievementguide.integration.battlefinal;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
public final class BattleFinalQuestProof {
    private BattleFinalQuestProof(){}
    public static boolean real(Entity entity){return entity instanceof ServerPlayer player&&TravelProof.eligible(player);}
    public static String item(ItemStack stack){return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
    public static boolean target(LivingEntity entity){return !(entity instanceof Player)&&!(entity instanceof TamableAnimal tame&&tame.isTame())&&!entity.getPersistentData().getBoolean("jem:event_spawned")&&!entity.getPersistentData().getCompound("jem_world_boss_tiers").getString("Provenance").equals("RAID_EVENT");}
    public static void emit(Entity actor,String action){if(real(actor))QuestActionTrigger.INSTANCE.emit((ServerPlayer)actor,action);}
}
