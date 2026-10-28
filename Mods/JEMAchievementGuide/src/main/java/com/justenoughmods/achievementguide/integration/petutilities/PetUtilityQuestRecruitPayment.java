package com.justenoughmods.achievementguide.integration.petutilities;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
public final class PetUtilityQuestRecruitPayment {
 public record Run(PetUtilityQuestRecruit.Hire hire,ItemStack currency,int count){}
 private PetUtilityQuestRecruitPayment(){}
 public static Run begin(Player p,AbstractRecruitEntity r){var id=ResourceLocation.tryParse(RecruitsServerConfig.RecruitCurrency.get());var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);var currency=new ItemStack(item==null || item==Items.AIR?Items.EMERALD:item);return new Run(PetUtilityQuestRecruit.before(r,p),currency,PetUtilityQuestProof.count(p.getInventory(),currency));}
 public static void finish(Player p,AbstractRecruitEntity r,Run f,boolean accepted){if(accepted && f!=null)PetUtilityQuestRecruit.hired(r,f.hire(),f.count()-PetUtilityQuestProof.count(p.getInventory(),f.currency())>0);}
}
