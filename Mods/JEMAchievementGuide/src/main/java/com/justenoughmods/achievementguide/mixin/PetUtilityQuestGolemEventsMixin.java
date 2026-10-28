package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import tech.alexnijjar.golemoverhaul.common.entities.IShearable;
import tech.alexnijjar.golemoverhaul.common.entities.golems.HoneyGolem;
import java.util.List;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import dev.architectury.event.events.common.InteractionEvent.FarmlandTrample;
@Mixin(targets="tech.alexnijjar.golemoverhaul.common.events.ModEvents",remap=false)
public abstract class PetUtilityQuestGolemEventsMixin {
 @ModifyArg(method="registerHayGolemTramplePrevention",at=@At(value="INVOKE",target="Ldev/architectury/event/Event;register(Ljava/lang/Object;)V"),index=0)private static Object jem$nativeHayCallback(Object listener){if(listener instanceof FarmlandTrample callback)PetUtilityQuestHay.registered(callback);return listener;}
 @Redirect(method="lambda$registerShearInteractions$2",at=@At(value="INVOKE",target="Ltech/alexnijjar/golemoverhaul/common/entities/IShearable;onSheared()Ljava/util/List;"))private static List<ItemStack> jem$honey(IShearable target,Player p,Entity e,InteractionHand hand){var actor=PetUtilityQuestProof.actor(p);int old=target instanceof HoneyGolem honey?honey.getHoneyLevel():0;var result=target.onSheared();if(actor!=null && target instanceof HoneyGolem honey && old>=4 && honey.getHoneyLevel()==0 && !result.isEmpty())PetUtilityQuestProof.put(e,"honey",actor.getUUID());return result;}
 @Redirect(method="lambda$registerShearInteractions$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;m_5552_(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"))private static ItemEntity jem$physicalHoney(Entity e,ItemStack stack,float offset){var drop=e.spawnAtLocation(stack,offset);var actor=PetUtilityQuestProof.actor(e,"honey");if(actor!=null && e instanceof HoneyGolem && drop!=null && actor.serverLevel().getEntity(drop.getUUID())==drop && PetUtilityQuestProof.id(stack).equals("golemoverhaul:honey_blob"))PetUtilityQuestProof.put(drop,"honey",actor.getUUID());return drop;}
}
