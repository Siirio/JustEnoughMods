package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import team.recrafted.blastfromthepast.entity.GlacerosEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Animal.class)
public abstract class WorldPrehistoricQuestGlacerosFeedMixin {
 @Unique private ItemStack jem$input;
 @Unique private int jem$count;
 @Inject(method="mobInteract",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$input=null;if((Object)this instanceof GlacerosEntity glaceros && p.getItemInHand(hand).is(glaceros.getVariant().getDelphinium().asItem())){jem$input=p.getItemInHand(hand);jem$count=jem$input.getCount();}}
 @Inject(method="mobInteract",at=@At("RETURN"))private void jem$after(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var actor=WorldPrehistoricQuestProof.actor(p);if(actor!=null && jem$input!=null && jem$count-jem$input.getCount()==1 && ((Animal)(Object)this).isAlive() && ci.getReturnValue().consumesAction())WorldPrehistoricQuestProof.emit(actor,"v3_era_forest_friends_glaceros");jem$input=null;}
}
