package com.justenoughmods.achievementguide.integration.companions;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraft.world.item.ArmorItem;
@Mod.EventBusSubscriber(modid=com.justenoughmods.achievementguide.JemAdvancementGuide.MOD_ID)
public final class CompanionQuestEquipment {
 private CompanionQuestEquipment(){}
 @SubscribeEvent public static void changed(LivingEquipmentChangeEvent event){if(!net.minecraftforge.fml.ModList.get().isLoaded("companions") || !net.minecraftforge.fml.ModList.get().isLoaded("knightlib"))return;var p=CompanionQuestProof.actor(event.getEntity());var stack=event.getTo();if(p==null || !CompanionQuestProof.holy(stack) || !(stack.getItem() instanceof ArmorItem armor) || event.getSlot()!=armor.getEquipmentSlot())return;var proof=CompanionQuestStack.get(stack);if(!proof.isEmpty() && proof.getString("kind").equals("holy") && proof.getUUID("actor").equals(p.getUUID()))CompanionQuestProof.emit(p,"v3_pet_blood_and_halo_holy");}
}
