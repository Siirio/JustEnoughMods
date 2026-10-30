package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import io.github.mortuusars.horseman.horse.HitchableHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value = LeadItem.class, priority = 1100)
public abstract class PetUtilityQuestFenceMixin {
 @Unique private static final ThreadLocal<PetUtilityQuestHorseman.Hitch> JEM_HITCH=new ThreadLocal<>();
 @Inject(method="bindPlayerMobs",at=@At("HEAD"))private static void jem$before(Player p,Level l,BlockPos pos,CallbackInfoReturnable<InteractionResult> ci){JEM_HITCH.remove();if(p instanceof ServerPlayer actor && PetUtilityQuestProof.actor(actor)!=null && p.getRootVehicle() instanceof AbstractHorse horse && horse instanceof HitchableHorse h && horse.getLeashHolder()==actor)JEM_HITCH.set(new PetUtilityQuestHorseman.Hitch(actor,horse,HitchableHorse.getLead(h).copy()));}

 @Inject(method="bindPlayerMobs",at=@At("RETURN"))private static void jem$nativeHitch(Player p,Level l,BlockPos pos,CallbackInfoReturnable<InteractionResult> ci){var f=JEM_HITCH.get();JEM_HITCH.remove();if(f!=null && f.actor()==p && f.horse().getLeashHolder() instanceof net.minecraft.world.entity.decoration.LeashFenceKnotEntity knot && knot.getPos().equals(pos) && ci.getReturnValue().consumesAction())PetUtilityQuestHorseman.hitch(f.actor(),f.horse(),f.lead());}
}
