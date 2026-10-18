package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.UUID;

/** Read-only city view; camera and filters never request hidden world data. */
public final class CityInspectorScreen extends Screen {
    private final Screen parent;
    private final HousingInspector inspector = new HousingInspector();
    private CompoundTag data;
    private boolean rotate, pan,house;
    private int selectedHome;
    private long nextPoll,nextHeartbeat;
    public CityInspectorScreen(Screen parent, CompoundTag data) {
        super(Component.translatable("jem.settlements.city.title")); this.parent=parent; inspector.surfaceOnly=true; accept(data);
    }
    public void accept(CompoundTag data) { this.data=data.copy(); if(!data.getBoolean("Building")&&!house)inspector.accept(data); nextPoll=System.currentTimeMillis()+(data.getBoolean("Building")?2000:10000); }
    public void acceptHouse(CompoundTag snapshot){house=true;inspector.surfaceOnly=false;inspector.accept(snapshot);inspector.resetView();}
    private void home(){
        var homes=data.getList("Homes",net.minecraft.nbt.Tag.TAG_COMPOUND);
        if(homes.isEmpty()||SettlementClientState.pending())return;
        var row=homes.getCompound(Math.floorMod(selectedHome++,homes.size()));
        CompoundTag payload=new CompoundTag();payload.putUUID("Entity",row.getUUID("Resident"));
        SettlementClientState.action("housing",data.getUUID("Town"),payload);
    }
    private static Component tr(String key,Object...args){return Component.translatable("jem.settlements.city."+key,args);}
    @Override protected void init() {
        int y=height-28, count=5, gap=4, size=Math.max(20,Math.min(126,(width-16-gap*(count-1))/count)), x=(width-(size*count+gap*(count-1)))/2;
        addRenderableWidget(Button.builder(tr("boundaries"),b->{inspector.showBounds=!inspector.showBounds;}).bounds(8,8,Math.min(96,width/4),20).build());
        addRenderableWidget(Button.builder(tr("house"),b->home()).bounds(width-Math.min(96,width/4)-8,8,Math.min(96,width/4),20).build());
        addRenderableWidget(Button.builder(tr("back"),b->onClose()).bounds(x,y,size,20).build());
        addRenderableWidget(Button.builder(tr("reset"),b->inspector.resetView()).bounds(x+(size+gap),y,size,20).build());
        addRenderableWidget(Button.builder(tr("comfort"),b->{inspector.onlyAmenities=!inspector.onlyAmenities;b.setMessage(tr(inspector.onlyAmenities?"all":"comfort"));}).bounds(x+2*(size+gap),y,size,20).build());
        addRenderableWidget(Button.builder(tr("lod"),b->{inspector.surfaceOnly=!inspector.surfaceOnly;inspector.refreshGeometry();}).bounds(x+3*(size+gap),y,size,20).build());
        addRenderableWidget(Button.builder(tr("refresh"),b->refresh()).bounds(x+4*(size+gap),y,size,20).build());
    }
    private void refresh(){house=false;inspector.surfaceOnly=true;if(data.hasUUID("Town")&&!SettlementClientState.pending())SettlementClientState.action("city",data.getUUID("Town"),new CompoundTag());}
    @Override public void tick(){super.tick();
        if(System.currentTimeMillis()>=nextHeartbeat){nextHeartbeat=System.currentTimeMillis()+5000;com.siirio.jemsettlements.network.SettlementNetwork.action("heartbeat",null,new CompoundTag(),UUID.randomUUID());}if(!house&&System.currentTimeMillis()>=nextPoll){nextPoll=System.currentTimeMillis()+(data.getBoolean("Building")?2000:10000);refresh();}}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){rotate=false;pan=false;minecraft.setScreen(parent);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);inspector.render(g,8,34,width-16,Math.max(24,height-70),mx,my);
        g.drawCenteredString(font,Component.literal(font.plainSubstrByWidth(data.getString("Name"),Math.max(20,width-224))),width/2,8,0xffe4dfca);
        g.drawCenteredString(font,tr(data.getBoolean("Building")?"building":data.getBoolean("Partial")?"partial":"surface",data.getBoolean("Building")?data.getInt("Progress"):data.getInt("Stride")),width/2,21,0xffc8c6b5);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(inspector.inside(x,y)&&(button==0||button==1)){rotate=button==0;pan=button==1;return true;}return false;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(rotate&&button==0){inspector.rotate(dx,dy);return true;}if(pan&&button==1){inspector.pan(dx,dy);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){rotate=false;pan=false;return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(inspector.inside(x,y)){inspector.zoom(delta);return true;}return super.mouseScrolled(x,y,delta);}
}
