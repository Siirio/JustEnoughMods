package com.siirio.jemserver.mixin.death;

import com.siirio.jemserver.smp.events.BloodMoonDeaths;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class BloodMoonPlayerDropsMixin {
    @Redirect(method = {"dropEquipment", "getExperienceReward"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean jem$ordinaryDrops(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return !(key == GameRules.RULE_KEEPINVENTORY && BloodMoonDeaths.ordinaryDeath((Player) (Object) this)) && rules.getBoolean(key);
    }
}
