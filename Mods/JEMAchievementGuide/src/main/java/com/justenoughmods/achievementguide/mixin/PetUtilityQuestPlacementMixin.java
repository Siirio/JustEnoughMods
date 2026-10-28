package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BlockItem.class)
public abstract class PetUtilityQuestPlacementMixin {
 @Shadow public abstract Block getBlock();
 @Redirect(method="useOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/BlockItem;place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;"))private InteractionResult jem$nativePlacement(BlockItem item,BlockPlaceContext c){return PetUtilityQuestPlacement.observe(c,getBlock(),()->item.place(c));}
}
