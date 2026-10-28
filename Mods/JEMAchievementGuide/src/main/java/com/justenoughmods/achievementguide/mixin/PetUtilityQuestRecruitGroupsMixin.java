package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.talhanation.recruits.world.*;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=RecruitsGroupsManager.class,remap=false)
public abstract class PetUtilityQuestRecruitGroupsMixin {
 @Inject(method="addOrUpdateGroup",at=@At("RETURN"))private void jem$saved(ServerLevel level,ServerPlayer actor,RecruitsGroup incoming,CallbackInfo ci){PetUtilityQuestRecruit.group(actor,(RecruitsGroupsManager)(Object)this,incoming);}
}
