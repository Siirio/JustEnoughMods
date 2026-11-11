package dev.taste.engine.client.ui;

import dev.taste.engine.client.ClientState;
import dev.taste.engine.render.TasteRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DiagnosticsScreen extends Screen {
    private final Screen parent;
    public DiagnosticsScreen(Screen parent){super(Component.translatable("taste.ui.diagnostics"));this.parent=parent;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);graphics.drawCenteredString(font,title,width/2,12,0xFFFFFF);int y=38;var renderer=TasteRenderer.instance();String[] lines={"Renderer available: "+ClientState.rendererAvailable(),"Renderer failure: "+renderer.failure(),"Embeddium installed: "+ClientState.embeddiumInstalled(),"Owned memory: "+renderer.allocatedBytes()+" bytes","Last GPU sample: "+String.format(java.util.Locale.ROOT,"%.3f ms",renderer.lastGpuMilliseconds()),"Config: "+ClientState.store().path()};for(String line:lines){graphics.drawString(font,line,20,y,0xD8D8D8,false);y+=14;}int capabilityY=y+6;renderer.capabilityResult().ifPresent(result->{graphics.drawString(font,"GL baseline: "+result.baseline()+" drawBuffers="+result.drawBuffers()+" samplers="+result.textureUnits(),20,capabilityY,0xD8D8D8,false);});super.render(graphics,mouseX,mouseY,partialTick);}
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
