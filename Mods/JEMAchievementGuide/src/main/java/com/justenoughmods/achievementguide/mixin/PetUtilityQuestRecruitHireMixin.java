package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=AbstractRecruitEntity.class,remap=false,priority=900)
public abstract class PetUtilityQuestRecruitHireMixin {
 @Unique private PetUtilityQuestRecruit.Hire jem$hire;
 @Inject(method="hire",at=@At("HEAD"))private void jem$before(Player p,RecruitsGroup group,boolean message,CallbackInfoReturnable<Boolean> ci){jem$hire=PetUtilityQuestRecruit.before((AbstractRecruitEntity)(Object)this,p);}
 @Inject(method="hire",at=@At("RETURN"))private void jem$after(Player p,RecruitsGroup group,boolean message,CallbackInfoReturnable<Boolean> ci){var f=jem$hire;jem$hire=null;var recruit=(AbstractRecruitEntity)(Object)this;if(ci.getReturnValueZ() && f!=null)PetUtilityQuestRecruit.hired(recruit,f,recruit.getPersistentData().getLong("JemCoinHireGeneration")==f.generation()+1);}
}
