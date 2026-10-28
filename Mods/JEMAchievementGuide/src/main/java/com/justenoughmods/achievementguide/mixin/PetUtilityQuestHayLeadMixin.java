package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import tech.alexnijjar.golemoverhaul.common.entities.golems.HayGolem;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Mob.class)
public abstract class PetUtilityQuestHayLeadMixin {
 @Inject(method="setLeashedTo",at=@At("RETURN"))private void jem$established(Entity holder,boolean broadcast,CallbackInfo ci){if((Object)this instanceof HayGolem hay && holder instanceof ServerPlayer p && PetUtilityQuestProof.actor(p)!=null && hay.getLeashHolder()==p){PetUtilityQuestProof.put(hay,"hay",p.getUUID());PetUtilityQuestHay.established(p);}}
}
