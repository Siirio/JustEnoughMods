package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.LogbookTradeProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class LogbookTradeMenuQuestMixin {
    @Unique private LogbookTradeProof.Run jem$logbookRun;
    @Inject(method = "clicked", at = @At("HEAD"))
    private void jem$beginTrade(int slot, int button, ClickType action, Player player, CallbackInfo callback) {
        jem$logbookRun = (Object) this instanceof MerchantMenu menu && player instanceof ServerPlayer actor
                ? LogbookTradeProof.begin(menu, actor) : null;
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void jem$finishTrade(int slot, int button, ClickType action, Player player, CallbackInfo callback) {
        var run = jem$logbookRun;
        jem$logbookRun = null;
        if (run != null) LogbookTradeProof.finish(run);
    }
}
