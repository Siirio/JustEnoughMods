package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class FamilyHomeScreen extends Screen {
    private final Screen parent; private final CompoundTag data; private final HousingInspector inspector = new HousingInspector(); private boolean rotate, pan;
    public FamilyHomeScreen(Screen parent, CompoundTag data) { super(Component.translatable("jem.settlements.family.home_tab")); this.parent = parent; this.data = data.copy(); inspector.accept(data); }
    @Override protected void init() {
        if (!data.getBoolean("NoHome")) {
            Button map=Button.builder(Component.translatable("jem.settlements.family.map"), b -> com.siirio.jemsettlements.compat.xaero.XaeroHomeMap.open(this, data)).bounds(width/2-102,height-26,100,20).build();map.active=data.contains("Anchor");addRenderableWidget(map);
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(width/2+2,height-26,100,20).build());
        } else addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(width/2-50,height/2+18,100,20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics); graphics.drawCenteredString(font, Component.literal(data.getString("Name")), width/2, 8, 0xfff0e4cf);
        if (data.getBoolean("NoHome")) graphics.drawCenteredString(font, Component.translatable("jem.settlements.family.no_home"), width/2, height/2-8, 0xffffffff);
        else inspector.render(graphics, 8, 24, width-16, Math.max(30,height-58), mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partial);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(!data.getBoolean("NoHome")&&inspector.inside(x,y)&&(button==0||button==1)){rotate=button==0;pan=button==1;return true;}return false;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(rotate&&button==0){inspector.rotate(dx,dy);return true;}if(pan&&button==1){inspector.pan(dx,dy);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){rotate=false;pan=false;return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(inspector.inside(x,y)){inspector.zoom(delta);return true;}return super.mouseScrolled(x,y,delta);}
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
