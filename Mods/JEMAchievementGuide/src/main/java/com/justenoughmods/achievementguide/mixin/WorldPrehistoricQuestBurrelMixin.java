package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import team.recrafted.blastfromthepast.entity.BurrelEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=BurrelEntity.class,remap=false)
public abstract class WorldPrehistoricQuestBurrelMixin {
 @Unique private ItemStack jem$input;
 @Unique private int jem$count;
 @Unique private boolean jem$pinecone;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$input=p.getItemInHand(hand);jem$count=jem$input.getCount();jem$pinecone=WorldPrehistoricQuestProof.id(jem$input).equals("blastfromthepast:pinecone");}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var actor=WorldPrehistoricQuestProof.actor(p);if(actor!=null && jem$pinecone && jem$input!=null && jem$count-jem$input.getCount()==1 && ((BurrelEntity)(Object)this).isAlive() && ci.getReturnValue().consumesAction())WorldPrehistoricQuestProof.emit(actor,"v3_era_forest_friends");jem$input=null;}
}
