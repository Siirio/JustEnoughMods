package com.siirio.jemguisandbox.mirror.jemvisualsandbox.client.races;
import net.minecraft.client.gui.GuiGraphics;
/** The installed renderer creates detached display entities and never adds them to a level. */
public final class AbilityPreviewRenderer {
    private final com.siirio.jemvisualsandbox.client.races.AbilityPreviewRenderer nativePreview = new com.siirio.jemvisualsandbox.client.races.AbilityPreviewRenderer();
    public void reset() {nativePreview.reset();}
    public void dispose() {nativePreview.dispose();}
    public void render(GuiGraphics g,int x,int y,int w,int h,int race,int slot,int tier,int bloom,float partial,double scale) {
        nativePreview.render(g,x,y,w,h,race,slot,tier,bloom,partial,scale);
    }
}
