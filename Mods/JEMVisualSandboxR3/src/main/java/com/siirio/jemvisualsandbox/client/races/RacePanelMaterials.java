package com.siirio.jemvisualsandbox.client.races;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Continuous wood panels with sparse metal corners; the origin's black stage stays clear. */
final class RacePanelMaterials {
    private static final ResourceLocation WOOD = new ResourceLocation("minecraft", "textures/block/stripped_spruce_log.png");
    private RacePanelMaterials() { }
    static void board(GuiGraphics g,int x,int y,int w,int h,boolean focused) {
        g.fill(x,y,x+w,y+h,0xff17120e);
        g.blit(WOOD,x+2,y+2,w-4,h-4,0,0,16,16,16,16);
        g.fill(x+2,y+2,x+w-2,y+h-2,focused?0x78362b18:0xb01f1711);
        g.fill(x+1,y+1,x+w-1,y+2,focused?0xffc6b979:0xff977b4d);
        g.fill(x+1,y+2,x+2,y+h-1,0xff735d3c);
        g.fill(x+2,y+h-2,x+w-1,y+h-1,0xff35291d);
        for(int dx:new int[]{2,w-5})for(int dy:new int[]{2,h-5}) {
            g.fill(x+dx,y+dy,x+dx+3,y+dy+3,0xff49483f);
            g.fill(x+dx,y+dy,x+dx+2,y+dy+1,0xffb1ad97);
        }
    }
}
