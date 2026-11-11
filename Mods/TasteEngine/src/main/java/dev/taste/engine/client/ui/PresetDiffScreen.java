package dev.taste.engine.client.ui;

import com.google.gson.JsonPrimitive;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PresetDiffScreen extends Screen {
    private final Screen parent;
    private final Map<String, JsonPrimitive> differences;
    public PresetDiffScreen(Screen parent, Map<String, JsonPrimitive> differences) { super(Component.translatable("taste.ui.presetDiff")); this.parent=parent; this.differences=Map.copyOf(differences); }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);graphics.drawCenteredString(font,title,width/2,12,0xFFFFFF);int y=34;for(var entry:differences.entrySet()){if(y>height-24)break;graphics.drawString(font,entry.getKey()+" = "+entry.getValue(),20,y,0xD8D8D8,false);y+=11;}super.render(graphics,mouseX,mouseY,partialTick);}
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
