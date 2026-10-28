package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestWorldChanges;
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
public abstract class WildlifeQuestWorldActionMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private WildlifeQuestWorldChanges.Run jem$useOn;
    @Unique private WildlifeQuestWorldChanges.Run jem$use;
    @Unique private WildlifeQuestWorldChanges.Run jem$break;
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void jem$useOn(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { jem$useOn = WildlifeQuestWorldChanges.begin(actor, hand, null); }
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void jem$usedOn(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) { WildlifeQuestWorldChanges.end(jem$useOn, callback.getReturnValue().consumesAction()); jem$useOn = null; }
    @Inject(method = "useItem", at = @At("HEAD"))
    private void jem$use(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$use = WildlifeQuestWorldChanges.begin(actor, hand, null); }
    @Inject(method = "useItem", at = @At("RETURN"))
    private void jem$used(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { WildlifeQuestWorldChanges.end(jem$use, callback.getReturnValue().consumesAction()); jem$use = null; }
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$break(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { jem$break = WildlifeQuestWorldChanges.begin(player, InteractionHand.MAIN_HAND, pos); }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$broke(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { WildlifeQuestWorldChanges.end(jem$break, callback.getReturnValue()); jem$break = null; }
}
