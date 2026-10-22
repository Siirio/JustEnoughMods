package com.siirio.jemguisandbox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** A destination/item inspection sheet, never an imitation of the native map. */
public final class PreviewScreen extends Screen {
    private final Screen parent;
    private final ItemStack item;
    private final String text;
    public PreviewScreen(Screen parent,ItemStack item) {super(item.getHoverName());this.parent=parent;this.item=item.copy();text="Display sample only";}
    public PreviewScreen(Screen parent,ResourceLocation dim,BlockPos pos) {super(Component.literal("Demo destination"));this.parent=parent;item=ItemStack.EMPTY;text=dim+" | "+pos.getX()+", "+pos.getY()+", "+pos.getZ();}
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-60,height-45,120,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt) {
        renderBackground(g);g.drawCenteredString(font,title,width/2,35,0xFFFFFF);g.drawCenteredString(font,text,width/2,height/2+20,0xCCCCCC);
        if(!item.isEmpty()) {g.renderItem(item,width/2-8,height/2-15);if(mx>=width/2-8&&mx<width/2+8&&my>=height/2-15&&my<height/2+1)g.renderTooltip(font,item,mx,my);}
        super.render(g,mx,my,dt);
    }
    @Override public void onClose() {minecraft.setScreen(parent);}
}
