package com.siirio.jemfood.mixin;

import com.siirio.jemfood.FoodState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class ExhaustionMixin {
    @Unique private boolean jemfood$sprint;
    @Redirect(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V", ordinal = 3))
    private void sprintExhaustion(Player player, float amount) {
        // Exact native ordinal 3 is the SPRINT_ONE_CM branch, not swimming, jumping or combat.
        jemfood$sprint = true;
        try { player.causeFoodExhaustion(amount); } finally { jemfood$sprint = false; }
    }
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float foodExhaustion(float amount) {
        if (!((Object) this instanceof ServerPlayer player)) return amount;
        var state = FoodState.get(player);
        double reduction = state.value(player, "satiety");
        if (jemfood$sprint) reduction = Math.max(reduction, state.value(player, "trailrunner"));
        return amount * (float) (1 - reduction);
    }
}
