package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.SpawnCareQuestWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
public abstract class SpawnCareQuestWorldActionMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private SpawnCareQuestWorld.Run jem$place;
    @Unique private SpawnCareQuestWorld.Run jem$break;
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void jem$before(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { jem$place = SpawnCareQuestWorld.begin(actor, hand, null); }
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void jem$after(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { SpawnCareQuestWorld.end(jem$place, callback.getReturnValue().consumesAction()); jem$place = null; }
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { jem$break = SpawnCareQuestWorld.begin(player, InteractionHand.MAIN_HAND, pos); }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { SpawnCareQuestWorld.end(jem$break, callback.getReturnValue()); jem$break = null; }
}
