package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="com.talhanation.recruits.CommandEvents",remap=false,priority=900)
public abstract class PetUtilityQuestRecruitCommandMixin {
 @Unique private static final ThreadLocal<PetUtilityQuestRecruitPayment.Run> JEM_PAYMENT=new ThreadLocal<>();
 @Unique private static final ThreadLocal<List<PetUtilityQuestRecruit.Order>> JEM_ORDERS=new ThreadLocal<>();
 @Inject(method="handleRecruiting",at=@At("HEAD"))private static void jem$payBefore(Player p,RecruitsGroup group,AbstractRecruitEntity recruit,boolean message,CallbackInfoReturnable<Boolean> ci){JEM_PAYMENT.set(PetUtilityQuestRecruitPayment.begin(p,recruit));}
 @Inject(method="handleRecruiting",at=@At("RETURN"))private static void jem$payAfter(Player p,RecruitsGroup group,AbstractRecruitEntity recruit,boolean message,CallbackInfoReturnable<Boolean> ci){var f=JEM_PAYMENT.get();JEM_PAYMENT.remove();PetUtilityQuestRecruitPayment.finish(p,recruit,f,ci.getReturnValueZ());}
 @Inject(method="onMovementCommand(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/List;IIZZ)V",at=@At("HEAD"))private static void jem$ordersBefore(ServerPlayer p,List<AbstractRecruitEntity> recruits,int state,int formation,boolean tight,boolean hold,CallbackInfo ci){JEM_ORDERS.set(PetUtilityQuestRecruit.orders(p,recruits));}
 @Inject(method="onMovementCommand(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/List;IIZZ)V",at=@At("RETURN"))private static void jem$ordersAfter(ServerPlayer p,List<AbstractRecruitEntity> recruits,int state,int formation,boolean tight,boolean hold,CallbackInfo ci){var f=JEM_ORDERS.get();JEM_ORDERS.remove();PetUtilityQuestRecruit.ordered(p,f);}
}
