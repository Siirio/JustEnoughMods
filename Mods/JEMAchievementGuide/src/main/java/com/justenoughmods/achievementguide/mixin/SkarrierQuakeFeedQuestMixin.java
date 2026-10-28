package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.entity.QuakeEntity", remap = false)
public abstract class SkarrierQuakeFeedQuestMixin {
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$begin(Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        SkarrierWorkshopProof.begin("quake_feed", actor, (Entity)(Object)this, null, actor.getItemInHand(hand));
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$end(Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { SkarrierWorkshopProof.end(); }
}
