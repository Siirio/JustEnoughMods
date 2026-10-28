package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import io.github.mortuusars.horseman.horse.HitchableHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=LeashFenceKnotEntity.class,priority=1100)
public abstract class PetUtilityQuestKnotMixin {
 @Unique private PetUtilityQuestHorseman.Hitch jem$hitch;
 @Inject(method="interact",at=@At("HEAD"))private void jem$before(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){jem$hitch=null;if(p instanceof ServerPlayer actor && PetUtilityQuestProof.actor(actor)!=null && p.getRootVehicle() instanceof AbstractHorse horse && horse instanceof HitchableHorse hitched && HitchableHorse.canHitch(hitched))jem$hitch=new PetUtilityQuestHorseman.Hitch(actor,horse,HitchableHorse.getLead(hitched).copy());}
 @Inject(method="interact",at=@At("RETURN"))private void jem$hitch(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){var f=jem$hitch;jem$hitch=null;if(f!=null && f.actor()==p && f.horse().getLeashHolder()==(Object)this && ci.getReturnValue().consumesAction())PetUtilityQuestHorseman.hitch(f.actor(),f.horse(),f.lead());}
}
