package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBuckets;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class WildlifeQuestBucketReleaseMixin {
    @Unique private WildlifeQuestRun jem$bucket;
    @Inject(method = "useItem", at = @At("HEAD"))
    private void jem$before(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$bucket = WildlifeQuestBuckets.beforeRelease(actor, stack); }
    @Inject(method = "useItem", at = @At("RETURN"))
    private void jem$after(ServerPlayer actor, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$bucket; jem$bucket = null; WildlifeQuestBuckets.released(run, actor.getItemInHand(hand), callback.getReturnValue().consumesAction());
    }
}
