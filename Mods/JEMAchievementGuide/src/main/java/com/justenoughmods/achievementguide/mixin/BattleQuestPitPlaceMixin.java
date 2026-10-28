package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestPit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public abstract class BattleQuestPitPlaceMixin {
    @Inject(method="place",at=@At("HEAD"))private void jem$before(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci){BattleQuestPit.begin(context);}
    @Inject(method="place",at=@At("RETURN"))private void jem$after(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci){BattleQuestPit.end(ci.getReturnValue());}
}
