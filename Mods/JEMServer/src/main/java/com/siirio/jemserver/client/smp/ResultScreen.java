package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.*;
import com.siirio.jemserver.smp.CombatNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ResultScreen extends Screen {
    private static final int MARGIN=18;
    private static final int ROW_HEIGHT=32;
    private CompoundTag result;
    private int scroll;
    private int contentHeight;
    private SmpActionButton claim;
    private boolean claiming;
    public ResultScreen(CompoundTag result) {super(Component.translatable(result.getBoolean("success")?"jem.smp.result.victory":"jem.smp.result.failure"));this.result=result.copy();}
    public UUID resultId() {return result.getUUID("id");}
    public void update(CompoundTag data) {result=data.copy();claiming=false;if(claim!=null) claim.active=!result.getList("bundles",Tag.TAG_COMPOUND).isEmpty();}
    @Override protected void init() {
        claim=addRenderableWidget(new SmpActionButton(width/2-90,height-32,180,20,Component.translatable("jem.smp.result.claim"),"primary",button->{
            claiming=true;button.active=false;CombatNetwork.claim(resultId());
        }));
        claim.active=!claiming && !result.getList("bundles",Tag.TAG_COMPOUND).isEmpty();
        addRenderableWidget(new SmpActionButton(width-30,8,20,20,Component.literal("×"),"danger",button->onClose()));
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);
        int left=MARGIN,right=width-MARGIN,top=64,bottom=height-42;
        SmpGuiAssets.panel(graphics,left,8,right-left,bottom-8,PANEL);
        SmpGuiAssets.frame(graphics,left,8,right-left,bottom-8,SmpGuiAssets.accent(result.getBoolean("blood")?"BLOOD_MOON":"BOSS_RAID"));
        graphics.drawCenteredString(font,title,width/2,19,CREAM);
        graphics.drawCenteredString(font,CombatHud.title(result),width/2,36,PEACH);
        boolean blood=result.getBoolean("blood");
        graphics.drawString(font,Component.translatable(blood?"jem.smp.result.blood_columns":"jem.smp.result.boss_columns"),left+10,top-13,MUTED,false);
        ItemStack hovered=ItemStack.EMPTY;
        graphics.enableScissor(left,top,right,bottom);
        try {
            int y=top-scroll;
            var rows=result.getList("rows",Tag.TAG_COMPOUND);
            for(int index=0;index<rows.size();index++) {
                var row=rows.getCompound(index);
                graphics.fill(left+5,y,right-5,y+ROW_HEIGHT-2,index%2==0?INSET:ROW);
                graphics.drawString(font,Integer.toString(index+1),left+10,y+10,MUTED,false);
                CombatHud.head(graphics,row.getUUID("id"),left+27,y+6,18);
                graphics.drawString(font,font.plainSubstrByWidth(row.getString("name"),Math.max(35,(right-left)/3)),left+51,y+5,CREAM,false);
                var stats=blood?Component.translatable("jem.smp.result.blood_stats",row.getInt("kills"),Math.round(row.getDouble("damage")),row.getInt("revives"),row.getInt("waves"))
                        :Component.translatable("jem.smp.result.boss_stats",Math.round(row.getDouble("damage")),row.getInt("revives"));
                graphics.drawString(font,font.plainSubstrByWidth(stats.getString(),right-left-65),left+51,y+17,MUTED,false);
                y+=ROW_HEIGHT;
            }
            y+=10;
            var bundles=result.getList("bundles",Tag.TAG_COMPOUND);
            graphics.drawString(font,Component.translatable(bundles.isEmpty()?"jem.smp.result.no_rewards":"jem.smp.result.rewards"),left+10,y,CREAM,false);
            y+=16;
            for(var tag:bundles) {
                var bundle=(CompoundTag)tag;
                graphics.drawString(font,font.plainSubstrByWidth(Component.translatable(bundle.getString("titleKey")).getString(),right-left-20),left+10,y,MUTED,false);
                y+=12;
                int x=left+10;
                for(var item:bundle.getList("items",Tag.TAG_COMPOUND)) {
                    if(x+18>right-8) {x=left+10;y+=22;}
                    var stack=ItemStack.of((CompoundTag)item);
                    graphics.renderItem(stack,x,y);graphics.renderItemDecorations(font,stack,x,y);
                    if(mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18 && mouseY>=top && mouseY<bottom) hovered=stack;
                    x+=22;
                }
                y+=28;
            }
            contentHeight=y+scroll-top;
        } finally {
            graphics.disableScissor();
        }
        super.render(graphics,mouseX,mouseY,partialTick);
        if(result.contains("error")) graphics.drawCenteredString(font,Component.translatable(result.getString("error")),width/2,height-44,RED);
        if(!hovered.isEmpty()) graphics.renderTooltip(font,hovered,mouseX,mouseY);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double delta) {
        scroll=Math.max(0,Math.min(Math.max(0,contentHeight-(height-106)),scroll-(int)(delta*ROW_HEIGHT)));
        return true;
    }
    @Override public boolean isPauseScreen() {return false;}
    @Override public void removed() {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreAfterScreen();
        super.removed();
    }
}
