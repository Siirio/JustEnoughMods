package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=RibbitEntity.class,remap=false)
public abstract class PetUtilityQuestRibbitHomeMixin {
 @Unique private BlockPos jem$home;
 @Unique private int jem$count;
 @Unique private boolean jem$amethyst;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$home=((RibbitEntity)(Object)this).getHomePosition();jem$count=p.getItemInHand(hand).getCount();jem$amethyst=p.getItemInHand(hand).is(Items.AMETHYST_SHARD);}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var ribbit=(RibbitEntity)(Object)this;var actor=PetUtilityQuestProof.actor(p);var home=ribbit.getHomePosition();if(actor!=null && jem$amethyst && jem$count-p.getItemInHand(hand).getCount()==1 && home!=null && !java.util.Objects.equals(home,jem$home) && home.equals(ribbit.blockPosition()) && actor.level()==ribbit.level() && !actor.level().isOutsideBuildHeight(home))PetUtilityQuestProof.emit(actor,"v4_pet_ribbit_home");}
}
