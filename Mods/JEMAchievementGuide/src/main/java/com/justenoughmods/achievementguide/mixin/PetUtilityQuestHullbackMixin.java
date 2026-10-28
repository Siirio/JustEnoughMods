package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.fruityspikes.whaleborne.server.entities.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=HullbackEntity.class,remap=false)
public abstract class PetUtilityQuestHullbackMixin implements PetUtilityQuestVehicle {
 @Unique private boolean jem$tamed;
 @Unique private PetUtilityQuestWhale.Install jem$install;
 @Unique private PetUtilityQuestWhale.Armor jem$armor;
 @Unique private Vec3 jem$from;
 @Unique private float jem$heading;
 @Inject(method="interactClean",at=@At("HEAD"))private void jem$dirty(Player p,InteractionHand h,HullbackPartEntity part,Boolean top,CallbackInfoReturnable<InteractionResult> ci){jem$tamed=((HullbackEntity)(Object)this).isTamed();}
 @Inject(method="interactClean",at=@At("RETURN"))private void jem$clean(Player p,InteractionHand h,HullbackPartEntity part,Boolean top,CallbackInfoReturnable<InteractionResult> ci){var whale=(HullbackEntity)(Object)this;var actor=PetUtilityQuestProof.actor(p);if(actor!=null && !jem$tamed && whale.isTamed() && ci.getReturnValue().consumesAction())PetUtilityQuestProof.put(whale,"clean",actor.getUUID());}
 @Inject(method="placeEquipment",at=@At("HEAD"))private void jem$placing(Player p,InteractionHand h,int seat,EntityType<?> type,CallbackInfoReturnable<InteractionResult> ci){jem$install=PetUtilityQuestWhale.before((HullbackEntity)(Object)this,p,h);}
 @Inject(method="placeEquipment",at=@At("RETURN"))private void jem$placed(Player p,InteractionHand h,int seat,EntityType<?> type,CallbackInfoReturnable<InteractionResult> ci){var f=jem$install;jem$install=null;if(ci.getReturnValue().consumesAction())PetUtilityQuestWhale.installed((HullbackEntity)(Object)this,p,h,f);}
 @Inject(method="interactArmor",at=@At("HEAD"))private void jem$paying(Player p,InteractionHand h,HullbackPartEntity part,Boolean top,CallbackInfoReturnable<InteractionResult> ci){jem$armor=PetUtilityQuestWhale.armorBefore((HullbackEntity)(Object)this,p,h);}
 @Inject(method="interactArmor",at=@At("RETURN"))private void jem$paid(Player p,InteractionHand h,HullbackPartEntity part,Boolean top,CallbackInfoReturnable<InteractionResult> ci){var f=jem$armor;jem$armor=null;if(ci.getReturnValue().consumesAction())PetUtilityQuestWhale.armored((HullbackEntity)(Object)this,p,h,f);}
 @Inject(method="m_7023_",at=@At("HEAD"))private void jem$before(Vec3 input,CallbackInfo ci){var whale=(HullbackEntity)(Object)this;jem$from=whale.position();jem$heading=whale.getYRot();}
 @Inject(method="m_7023_",at=@At("RETURN"))private void jem$steered(Vec3 input,CallbackInfo ci){var whale=(HullbackEntity)(Object)this;if(whale.getControllingPassenger() instanceof ServerPlayer actor)PetUtilityQuestWhale.helm(whale,actor,jem$from!=null && whale.position().distanceToSqr(jem$from)>1E-8 || Float.isFinite(jem$heading) && Math.abs(whale.getYRot()-jem$heading)>1E-5);jem$from=null;}
 @Override public void jem$petAcceptedMotion(ServerPlayer actor,Vec3 before){PetUtilityQuestWhale.helm((HullbackEntity)(Object)this,actor,((HullbackEntity)(Object)this).position().distanceToSqr(before)>1E-8);}
}
