package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=AbstractWagon.class,remap=false)
public abstract class PetUtilityQuestWagonMixin implements PetUtilityQuestWagonAccess,PetUtilityQuestVehicle {
 @Shadow private Mob[] horses;
 @Shadow private int speedLevel;
 @Unique private PetUtilityQuestWagon.Interaction jem$interaction;
 @Unique private PetUtilityQuestJourney jem$journey;
 @Unique private Vec3 jem$from;
 @Override public Mob[] jem$petHorses(){return java.util.Arrays.copyOf(horses,horses.length);}
 @Override public int jem$petSpeed(){return speedLevel;}
 @Inject(method="m_6096_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){jem$interaction=PetUtilityQuestWagon.before((AbstractWagon)(Object)this,p,h);}
 @Inject(method="m_6096_",at=@At("RETURN"))private void jem$after(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){var frame=jem$interaction;jem$interaction=null;if(ci.getReturnValue().consumesAction())PetUtilityQuestWagon.after((AbstractWagon)(Object)this,p,h,frame);}
 @Redirect(method="tryHitchHorse",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Mob;m_21455_(ZZ)V"))private void jem$hitch(Mob horse,boolean broadcast,boolean drop,Player p){var actor=PetUtilityQuestProof.actor(p);boolean nativeHitch=actor!=null && horse.getLeashHolder()==actor && java.util.Arrays.stream(horses).anyMatch(e->e==horse);horse.dropLeash(broadcast,drop);if(nativeHitch)PetUtilityQuestProof.put((Entity)(Object)this,"hitch",actor.getUUID());}
 @Inject(method="moveTick",at=@At("HEAD"))private void jem$from(CallbackInfo ci){jem$from=((Entity)(Object)this).position();}
 @Inject(method="moveTick",at=@At("RETURN"))private void jem$moved(CallbackInfo ci){var e=(Entity)(Object)this;if(e.getControllingPassenger() instanceof ServerPlayer actor && jem$from!=null && e.position().distanceToSqr(jem$from)>1E-8)jem$petAcceptedMotion(actor,jem$from);jem$from=null;}
 @Override public void jem$petAcceptedMotion(ServerPlayer actor,Vec3 before){var e=(Entity)(Object)this;var tag=PetUtilityQuestProof.data(e);if(PetUtilityQuestProof.actor(e,"hitch")!=actor || PetUtilityQuestProof.actor(e,"carrier")!=actor || speedLevel<=0 || !tag.hasUUID("cargo") || java.util.Arrays.stream(horses).noneMatch(h->h!=null && h.isAlive()) || e.getPassengers().stream().noneMatch(c->c.isAlive() && c.getUUID().equals(tag.getUUID("cargo")))){jem$journey=null;return;}if(jem$journey==null || jem$journey.actor!=actor || jem$journey.last.distanceToSqr(before)>1E-8)jem$journey=new PetUtilityQuestJourney(actor,e,before);if(jem$journey.move(actor,e,before)){tag.putBoolean("moved",true);e.getPersistentData().put(PetUtilityQuestProof.ROOT,tag);}}
}
