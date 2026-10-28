package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.prehistoric.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BlockItem.class)
public abstract class WorldPrehistoricQuestPlacementMixin {
 @Unique private int jem$prehistoryCount;
 @Inject(method="place",at=@At("HEAD"))private void jem$before(BlockPlaceContext c,CallbackInfoReturnable<InteractionResult> ci){jem$prehistoryCount=c.getItemInHand().getCount();}
 @Inject(method="place",at=@At("RETURN"))private void jem$after(BlockPlaceContext c,CallbackInfoReturnable<InteractionResult> ci){WorldPrehistoricQuestPlacement.placed((BlockItem)(Object)this,c,jem$prehistoryCount,ci.getReturnValue());}
}
