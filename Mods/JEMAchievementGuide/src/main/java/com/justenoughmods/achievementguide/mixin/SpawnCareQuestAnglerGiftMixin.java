package com.justenoughmods.achievementguide.mixin;
import com.ninni.spawn.registry.SpawnTags;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.ninni.spawn.server.entity.mob.AnglerFish", remap = false)
public abstract class SpawnCareQuestAnglerGiftMixin {
    @Unique private SpawnCareQuestProof.Interaction jem$gift;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$gift = player.getItemInHand(hand).is(SpawnTags.ANGLER_FISH_LIKES) && !player.hasEffect(MobEffects.NIGHT_VISION) ? SpawnCareQuestProof.begin((LivingEntity)(Object)this, player, hand) : null; }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$gift; jem$gift = null;
        if (run != null && callback.getReturnValue().consumesAction() && SpawnCareQuestProof.debited(run.input(), player.getItemInHand(hand), 1) && run.actor().hasEffect(MobEffects.NIGHT_VISION) && run.actor().getEffect(MobEffects.NIGHT_VISION).getDuration() > 0) SpawnCareQuestProof.emit(run.actor(), "v3_pet_angler_light");
    }
}
