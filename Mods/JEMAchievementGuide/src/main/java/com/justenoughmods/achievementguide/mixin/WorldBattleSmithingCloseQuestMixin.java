package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.BattleQuestSmithing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCombinerMenu.class)
public abstract class WorldBattleSmithingCloseQuestMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void jem$discardPreview(Player actor, CallbackInfo ci) {
        if ((Object)this instanceof SmithingMenu menu) BattleQuestSmithing.closed(menu);
    }
}
