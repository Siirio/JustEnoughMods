package com.siirio.jemprofessions.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** A quiet, continuous wood board; no repeated block-tile wallpaper behind the graph. */
final class ProfessionMaterials {
    private static final ResourceLocation WOOD = new ResourceLocation("minecraft", "textures/block/stripped_spruce_log.png");
    private ProfessionMaterials() { }
    static void board(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        g.fill(x, y, x+w, y+h, 0xff17130f);
        g.blit(WOOD, x+2, y+2, w-4, h-4, 0, 0, 16, 16, 16, 16);
        g.fill(x+2, y+2, x+w-2, y+h-2, selected ? 0x80352b17 : 0xa0261b14);
        g.fill(x+1, y+1, x+w-1, y+2, selected ? 0xffc7b777 : 0xff9b7c4d);
        g.fill(x+1, y+2, x+2, y+h-1, 0xff79613d);
        g.fill(x+2, y+h-2, x+w-1, y+h-1, 0xff33271c);
        for (int dx : new int[]{2,w-5}) for (int dy : new int[]{2,h-5}) {
            g.fill(x+dx,y+dy,x+dx+3,y+dy+3,0xff494940);
            g.fill(x+dx,y+dy,x+dx+2,y+dy+1,0xffb0ad96);
        }
    }
    static void recess(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x,y,x+w,y+h,0xff100e0c);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xff211b16);
        g.fill(x+1,y+1,x+w-1,y+2,0xff090807);
    }
}
