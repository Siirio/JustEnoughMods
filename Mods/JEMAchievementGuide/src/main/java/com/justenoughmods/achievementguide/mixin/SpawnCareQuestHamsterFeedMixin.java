package com.justenoughmods.achievementguide.mixin;
import com.ninni.spawn.server.entity.mob.Hamster;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.ninni.spawn.server.entity.mob.Hamster", remap = false)
public abstract class SpawnCareQuestHamsterFeedMixin {
    @Unique private SpawnCareQuestProof.Interaction jem$feed;
    @Unique private boolean jem$food;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { Hamster self = (Hamster)(Object)this; jem$feed = SpawnCareQuestProof.begin(self, player, hand); jem$food = self.isFood(player.getItemInHand(hand)); }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$feed; jem$feed = null;
        if (run != null && jem$food && run.owned() && ((Hamster)(Object)this).isOwnedBy(run.actor()) && callback.getReturnValue().consumesAction()
                && (SpawnCareQuestProof.debited(run.input(), player.getItemInHand(hand), 1) || run.entity().getHealth() > run.health())) SpawnCareQuestProof.fed(run);
    }
}
