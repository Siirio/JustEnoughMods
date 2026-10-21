package com.siirio.jemsettlements.mixin.xaero;

import com.siirio.jemsettlements.compat.xaero.SettlementMenuAccess;
import com.siirio.jemsettlements.compat.xaero.SettlementMapClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="xaero.lib.client.gui.widget.dropdown.DropDownWidget",remap=false)
public abstract class SettlementDropdownMixin {
    @Shadow protected boolean openingUp;
    private boolean jem$confirmation() { return (Object)this instanceof SettlementMenuAccess access && access.jem$expansionOption()!=null; }
    @ModifyConstant(method={"drawSlot","drawMenu","getHoveredId","getRenderY","onDropDown","optionLimit","scrolling"},constant=@Constant(intValue=11),require=0)
    private int jem$height(int original) { return jem$confirmation()?32:original; }
    @Inject(method="drawSlot",at=@At("HEAD"),cancellable=true)
    private void jem$twoLines(GuiGraphics g,Component label,int index,int row,int mouseX,int mouseY,boolean scrolling,int limit,int x,int y,int background,CallbackInfo callback) {
        if (!jem$confirmation()) return;
        var option=((SettlementMenuAccess)(Object)this).jem$expansionOption();
        int top=y+(openingUp?-row-1:row)*32, width=((AbstractWidget)(Object)this).getWidth();
        boolean hover=mouseX>=x&&mouseX<x+width&&mouseY>=top&&mouseY<top+32;
        g.fill(x,top,x+width,top+32,hover?0xFF323232:0xD0101010);
        var font=Minecraft.getInstance().font;
        g.drawString(font,Component.translatable("gui.jem_settlements.buy_for"),x+6,top+3,0xFFFFFFFF,false);
        com.siirio.jemsettlements.client.SettlementPriceRenderer.draw(g,font,option.draft.quote().addedCopper(),x+6,top+14,width-12,0xFFFFFFFF,SettlementMapClient.coins());
        callback.cancel();
    }
}
