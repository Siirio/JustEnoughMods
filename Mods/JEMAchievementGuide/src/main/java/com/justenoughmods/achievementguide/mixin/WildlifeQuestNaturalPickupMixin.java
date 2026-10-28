package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class WildlifeQuestNaturalPickupMixin {
    @Unique private WildlifeQuestSources.Pickup jem$wildlifePickup;
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void jem$before(Player player, CallbackInfo callback) {
        var actor = WildlifeQuestProof.real(player);
        jem$wildlifePickup = actor == null ? null : new WildlifeQuestSources.Pickup(actor, ((ItemEntity)(Object)this).getItem());
    }
    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void jem$after(Player player, CallbackInfo callback) {
        var run = jem$wildlifePickup; jem$wildlifePickup = null; WildlifeQuestSources.picked(run, (ItemEntity)(Object)this);
    }
}
