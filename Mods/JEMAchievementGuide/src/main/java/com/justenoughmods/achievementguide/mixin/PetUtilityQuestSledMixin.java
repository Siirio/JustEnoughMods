package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.mehvahdjukaar.snowyspirit.common.entity.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=SledEntity.class,remap=false)
public abstract class PetUtilityQuestSledMixin implements PetUtilityQuestVehicle {
 @Unique private Entity jem$oldPuller;
 @Unique private PetUtilityQuestJourney jem$journey;
 @Inject(method="m_6096_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){jem$oldPuller=((SledEntity)(Object)this).getSledPuller();}
 @Inject(method="m_6096_",at=@At("RETURN"))private void jem$hitch(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){var sled=(SledEntity)(Object)this;var actor=PetUtilityQuestProof.actor(p);if(actor!=null && sled.getSledPuller() instanceof Wolf wolf && wolf!=jem$oldPuller && actor.getUUID().equals(wolf.getOwnerUUID()) && sled.isMyPuller(wolf) && wolf.getVehicle()==sled && ci.getReturnValue().consumesAction())PetUtilityQuestProof.put(sled,"wolf",actor.getUUID());jem$oldPuller=null;}
 @Override public void jem$petAcceptedMotion(ServerPlayer actor,Vec3 before){var sled=(SledEntity)(Object)this;if(PetUtilityQuestProof.actor(sled,"wolf")!=actor || !(sled.getSledPuller() instanceof Wolf wolf) || !wolf.isAlive() || !actor.getUUID().equals(wolf.getOwnerUUID()) || !sled.isMyPuller(wolf) || !sled.getPassengers().stream().anyMatch(e->e instanceof ContainerHolderEntity cargo && sled.isChestEntity(cargo) && !cargo.isEmpty()) || actor.zza==0 && actor.xxa==0 || !PetUtilityQuestSnowContact.contact(sled)){jem$journey=null;return;}if(jem$journey==null || jem$journey.actor!=actor || jem$journey.last.distanceToSqr(before)>1E-8)jem$journey=new PetUtilityQuestJourney(actor,sled,before);if(jem$journey.move(actor,sled,before))PetUtilityQuestProof.emit(actor,"v3_pet_sled_team");}
}
