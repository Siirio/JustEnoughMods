package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import tech.alexnijjar.golemoverhaul.common.entities.golems.BarrelGolem;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=BarrelGolem.class,remap=false)
public abstract class PetUtilityQuestBarrelMixin {
 @Shadow private Player barteringTarget;
 @Unique private int jem$count;
 @Unique private boolean jem$emerald;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){jem$count=p.getItemInHand(h).getCount();jem$emerald=p.getItemInHand(h).is(Items.EMERALD);}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$paid(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){var actor=PetUtilityQuestProof.actor(p);var e=(BarrelGolem)(Object)this;if(actor!=null && barteringTarget==actor && jem$emerald && jem$count-p.getItemInHand(h).getCount()==1 && e.getMainHandItem().is(Items.EMERALD) && ci.getReturnValue().consumesAction())PetUtilityQuestProof.put(e,"barter",actor.getUUID());}
 @Redirect(method="throwItemsTowardPos",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/behavior/BehaviorUtils;m_22613_(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)V"))private void jem$reward(LivingEntity source,ItemStack stack,Vec3 pos){var actor=PetUtilityQuestProof.actor(source,"barter");PetUtilityQuestDrops.observe(()->{BehaviorUtils.throwItem(source,stack,pos);return true;},drops->{if(actor!=null && barteringTarget==actor && !drops.isEmpty() && drops.stream().anyMatch(d->ItemStack.isSameItemSameTags(d.getItem(),stack)))PetUtilityQuestProof.emit(actor,"v3_pet_golem_jobs");});}
}
