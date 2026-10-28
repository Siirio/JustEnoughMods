package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.ninni.spawn.server.entity.mob.Flukeshroom", remap = false)
public abstract class SpawnCareQuestSoupMixin {
    @Unique private SpawnCareQuestProof.Interaction jem$bowl;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$bowl = player.getItemInHand(hand).is(Items.BOWL) ? SpawnCareQuestProof.begin((LivingEntity)(Object)this, player, hand) : null; }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$bowl; jem$bowl = null; if (run == null || !callback.getReturnValue().consumesAction()) return; var after = SpawnCareQuestProof.held(run.actor());
        if (SpawnCareQuestProof.countItem(run.inventory(), Items.BOWL) - SpawnCareQuestProof.countItem(after, Items.BOWL) == 1 && SpawnCareQuestProof.countItem(after, Items.MUSHROOM_STEW) - SpawnCareQuestProof.countItem(run.inventory(), Items.MUSHROOM_STEW) == 1) SpawnCareQuestProof.emit(run.actor(), "v3_pet_mushroom_soup");
    }
}
