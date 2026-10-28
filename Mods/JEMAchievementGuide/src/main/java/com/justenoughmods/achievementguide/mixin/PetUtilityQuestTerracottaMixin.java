package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import tech.alexnijjar.golemoverhaul.common.entities.golems.TerracottaGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=TerracottaGolem.class,remap=false)
public abstract class PetUtilityQuestTerracottaMixin {
 @Unique private int jem$count;
 @Unique private TerracottaGolem.Type jem$type;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){jem$count=p.getItemInHand(h).getCount();jem$type=((TerracottaGolem)(Object)this).getTerracottaType();}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player p,InteractionHand h,CallbackInfoReturnable<InteractionResult> ci){var e=(TerracottaGolem)(Object)this;var actor=PetUtilityQuestProof.actor(p);var type=e.getTerracottaType();if(actor!=null && PetUtilityQuestProof.actor(e,"clay")==actor && jem$type==TerracottaGolem.Type.NORMAL && (type==TerracottaGolem.Type.CACTUS || type==TerracottaGolem.Type.DEAD_BUSH) && jem$count-p.getItemInHand(h).getCount()==1 && ci.getReturnValue().consumesAction())PetUtilityQuestProof.emit(actor,"v3_pet_warm_clay");}
}
