package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBuckets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class WildlifeQuestBucketCaptureMixin {
    @Inject(method = "interactOn", at = @At("HEAD"))
    private void jem$before(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (target instanceof LivingEntity entity) WildlifeQuestBuckets.before((Player)(Object)this, hand, entity);
    }
    @Inject(method = "interactOn", at = @At("RETURN"))
    private void jem$after(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (target instanceof LivingEntity entity) WildlifeQuestBuckets.captured((Player)(Object)this, hand, entity, callback.getReturnValue().consumesAction());
    }
}
