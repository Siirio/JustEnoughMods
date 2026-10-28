package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import team.recrafted.blastfromthepast.entity.PsychoBearEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PsychoBearEntity.class,remap=false)
public abstract class WorldPrehistoricQuestPsychoBearMixin {
 @Shadow private Player lastFedPlayer;
 @Unique private ItemStack jem$input;
 @Unique private int jem$count;
 @Unique private boolean jem$pacifier;
 @Unique private boolean jem$pacified;
 @Unique private java.util.UUID jem$paidMouth;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var bear=(PsychoBearEntity)(Object)this;jem$input=p.getItemInHand(hand);jem$count=jem$input.getCount();jem$pacifier=bear.isPacifier(jem$input) && WorldPrehistoricQuestProof.id(jem$input).equals("blastfromthepast:psycho_berry_ice_cream");}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$fed(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var bear=(PsychoBearEntity)(Object)this;var actor=WorldPrehistoricQuestProof.actor(p);if(actor!=null && jem$pacifier && jem$input!=null && jem$count-jem$input.getCount()==1 && lastFedPlayer==actor && bear.isEating() && bear.isPacifier(bear.getItemInMouth()) && ci.getReturnValue().consumesAction()){WorldPrehistoricQuestProof.mark(bear,"pacifier",actor);var receipt=WorldPrehistoricQuestProof.data(bear);receipt.put("mouth",bear.getItemInMouth().save(new net.minecraft.nbt.CompoundTag()));bear.getPersistentData().put(WorldPrehistoricQuestProof.ROOT,receipt);}jem$input=null;}
 @Inject(method="handleEating",at=@At("HEAD"))private void jem$chewing(CallbackInfo ci){var bear=(PsychoBearEntity)(Object)this;jem$pacified=bear.isPacified();var t=WorldPrehistoricQuestProof.data(bear);jem$paidMouth=t.hasUUID("pacifier") && t.getCompound("mouth").equals(bear.getItemInMouth().save(new net.minecraft.nbt.CompoundTag()))?t.getUUID("pacifier"):null;}
 @Inject(method="handleEating",at=@At("RETURN"))private void jem$pacified(CallbackInfo ci){var bear=(PsychoBearEntity)(Object)this;var actor=WorldPrehistoricQuestProof.actor(lastFedPlayer);if(actor!=null && !jem$pacified && bear.isPacified() && bear.getTarget()==null && actor.getUUID().equals(jem$paidMouth))WorldPrehistoricQuestProof.emit(actor,"v3_era_cool_heads");}
 @Inject(method="putItemInMouth",at=@At("HEAD"))private void jem$replaced(ItemStack stack,boolean guaranteeDrop,CallbackInfo ci){var bear=(PsychoBearEntity)(Object)this;var t=WorldPrehistoricQuestProof.data(bear);t.remove("pacifier");t.remove("mouth");bear.getPersistentData().put(WorldPrehistoricQuestProof.ROOT,t);}
}

