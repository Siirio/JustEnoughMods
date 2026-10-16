package com.siirio.jemsettlements.mixin;

import com.siirio.jemeconomy.client.CoinPriceRenderer;
import com.siirio.jemsettlements.client.NobleQuoteClient;
import com.talhanation.recruits.client.gui.NobleTradeScreen;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NobleTradeScreen.class)
public abstract class NobleTradeScreenMixin extends Screen implements NobleQuoteClient.ScreenAccess {
    @Shadow(remap = false) @Final private VillagerNobleEntity villagerNoble;
    @Shadow(remap = false) private RecruitsHireTrade selection;
    protected NobleTradeScreenMixin(Component title) { super(title); }
    @Override public int jem$nobleId() { return villagerNoble.getId(); }
    @Inject(method = "getPlayerCurrencyAmount", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$coinAffordability(CallbackInfoReturnable<Integer> ci) {
        if (selection == null) return;
        var quote = NobleQuoteClient.quote(villagerNoble.getId(), selection.resourceLocation);
        if (quote != null) ci.setReturnValue(NobleQuoteClient.affordable(quote) ? Integer.MAX_VALUE : -1);
    }
    @Inject(method = "renderForeground", at = @At("TAIL"), remap = false)
    private void jem$wage(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var priceHover = NobleQuoteClient.takePriceHover();
        if (priceHover != null) CoinPriceRenderer.tooltip(g, font, priceHover.cost(), mouseX, mouseY, width, height);
        if (selection == null) return;
        var quote = NobleQuoteClient.quote(villagerNoble.getId(), selection.resourceLocation);
        if (quote == null) return;
        var nativeScreen = (RecruitsScreenBase) (Object) this;
        int x = nativeScreen.getGuiLeft() + 97, y = nativeScreen.getGuiTop() + 53;
        if (mouseX >= x && mouseX < x + 75 && mouseY >= y && mouseY < y + 20)
            g.renderTooltip(font, Component.translatable("jem.settlements.hire.wage").append(": ")
                    .append(com.siirio.jemeconomy.physical.CoinFormatter.format(quote.wage())), mouseX, mouseY);
    }
}
