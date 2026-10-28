package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;import net.minecraft.server.level.*;import net.minecraft.world.*;import net.minecraft.world.item.ItemStack;import net.minecraft.world.level.Level;import net.minecraft.world.phys.BlockHitResult;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class) public abstract class SpawnCareQuestContainerReleaseMixin {
 @Unique private SpawnCareQuestContainers.Release jem$item;@Unique private SpawnCareQuestContainers.Release jem$block;
 @Inject(method="useItem",at=@At("HEAD")) private void jem$before(ServerPlayer actor,Level level,ItemStack stack,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$item=SpawnCareQuestContainers.begin(actor,hand);}
 @Inject(method="useItem",at=@At("RETURN")) private void jem$after(ServerPlayer actor,Level level,ItemStack stack,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var run=jem$item;jem$item=null;SpawnCareQuestContainers.released(run,ci.getReturnValue().consumesAction());}
 @Inject(method="useItemOn",at=@At("HEAD")) private void jem$beforeBlock(ServerPlayer actor,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){jem$block=SpawnCareQuestContainers.begin(actor,hand);}
 @Inject(method="useItemOn",at=@At("RETURN")) private void jem$afterBlock(ServerPlayer actor,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){var run=jem$block;jem$block=null;SpawnCareQuestContainers.released(run,ci.getReturnValue().consumesAction());}
}
