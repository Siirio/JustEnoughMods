package com.siirio.jemserver.mixin.death;

import com.siirio.jemserver.smp.events.BloodMoonDeaths;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class BloodMoonPlayerCloneMixin {
    @Redirect(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean jem$ordinaryRespawn(GameRules rules, GameRules.Key<GameRules.BooleanValue> key, ServerPlayer original, boolean keepEverything) {
        return !(key == GameRules.RULE_KEEPINVENTORY && BloodMoonDeaths.ordinaryDeath(original)) && rules.getBoolean(key);
    }
}
