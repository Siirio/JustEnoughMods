package com.siirio.jemguisandbox;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Navigation chrome uses native widgets; domain pages retain their original art. */
public final class GalleryScreen extends Screen {
    private static final String[][] ROUTES={
        {"smp","SMP / Events"},{"profile","Player profile"},{"players","Players"},{"parties","Parties / Lobbies"},
        {"settlements","Settlement dashboard"},{"settlement-directory","Settlement directory"},{"founding","Found settlement"},{"settlement-guide","Settlement guide"},
        {"death","Death history"},{"market","Market"},{"shops","Player shops"},{"prizes","Prizes"},
        {"encounter","Boss / Raid preparation"},{"busy-arena","Occupied arena"},{"result","Victory / Rewards"},{"defeat","Defeat"},
        {"bosses","Boss directory"},{"landmarks","Landmarks"},{"homes","Homes"},{"empty-events","Empty event list"},
        {"races","Race selection"},{"race-progression","Race progression / Powers"},{"inventory","Native mod inventory"},{"reset","Reset demo data"}
    };
    private int page;
    public GalleryScreen() { super(Component.literal("JEM GUI Sandbox")); }
    @Override protected void init() {
        clearWidgets();
        int columns=width>=480?3:2,rows=Math.max(1,(height-112)/25),capacity=columns*rows;
        int pages=(ROUTES.length+capacity-1)/capacity;page=Math.min(page,pages-1);
        int w=Math.min(190,(width-32)/columns),start=(width-columns*w-(columns-1)*4)/2;
        for(int index=page*capacity;index<Math.min(ROUTES.length,(page+1)*capacity);index++) {
            int local=index-page*capacity;String[] route=ROUTES[index];
            addRenderableWidget(Button.builder(Component.literal(route[1]),b->Demo.open(route[0]))
                    .bounds(start+(local%columns)*(w+4),56+(local/columns)*25,w,21).build());
        }
        if(pages>1) {
            var prev=addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;init();}).bounds(width/2-78,height-48,45,20).build());prev.active=page>0;
            var next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;init();}).bounds(width/2+33,height-48,45,20).build());next.active=page<pages-1;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width/2-45,height-25,90,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt) {
        renderBackground(g);g.drawCenteredString(font,title,width/2,12,0xFFFFFF);
        g.drawCenteredString(font,"Local demo data | F8 / /jemgui to return",width/2,29,0xCCCCCC);
        g.drawCenteredString(font,"Native mod GUIs: inventory keys / JEI / map keys",width/2,height-61,0xBBBBBB);
        super.render(g,mx,my,dt);
    }
    @Override public boolean isPauseScreen() { return false; }
}
