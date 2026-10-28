package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraProof;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCooldowns.class)
public abstract class WorldBattleExtraCooldownQuestMixin {
    @Inject(method = "addCooldown", at = @At("RETURN"))
    private void jem$nativeCooldown(Item item, int ticks, CallbackInfo ci) { WorldBattleExtraProof.cooldown((ItemCooldowns)(Object)this, item, ticks); }
}
