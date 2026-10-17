package com.siirio.jemsettlements.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Shared transform for drawing and hit-testing real inclusive block rectangles. */
final class SettlementMap {
    private double centerX, centerZ, zoom = 1;
    private boolean centered, dragging;
    private int x, y, width, height;
    private Integer targetX, targetZ;
    void bounds(int x, int y, int width, int height) { this.x=x; this.y=y; this.width=width; this.height=height; dragging=false; }
    void center(BlockPos pos) { centerX=pos.getX(); centerZ=pos.getZ(); centered=true; }
    void release() { dragging=false; }
    void clearQuote() { targetX=null; targetZ=null; }
    boolean contains(double mx,double my) { return mx>=x && mx<x+width && my>=y && my<y+height; }
    void zoom(double delta,double mx,double my) {
        double wx=worldX(mx), wz=worldZ(my);
        zoom=Math.max(.25,Math.min(8,zoom*Math.pow(1.25,delta)));
        centerX=wx-(mx-x-width/2d)/zoom; centerZ=wz-(my-y-height/2d)/zoom;
    }
    void zoom(double delta) { zoom(delta,x+width/2d,y+height/2d); }
    private double worldX(double px) { return centerX+(px-x-width/2d)/zoom; }
    private double worldZ(double py) { return centerZ+(py-y-height/2d)/zoom; }
    private int sx(double wx) { return (int)Math.floor(x+width/2d+(wx-centerX)*zoom); }
    private int sy(double wz) { return (int)Math.floor(y+height/2d+(wz-centerZ)*zoom); }
    boolean click(double mx,double my,int button,CompoundTag town) {
        if (!contains(mx,my)) return false;
        if (button==1) { dragging=true; return true; }
        if (button!=0) return false;
        int wx=(int)Math.floor(worldX(mx)), wz=(int)Math.floor(worldZ(my));
        int minX=town.getInt("TerritoryMinX"), maxX=town.getInt("TerritoryMaxX"), minZ=town.getInt("TerritoryMinZ"), maxZ=town.getInt("TerritoryMaxZ");
        boolean adjacent = ((wx==minX-1 || wx==maxX+1) && wz>=minZ && wz<=maxZ)
                || ((wz==minZ-1 || wz==maxZ+1) && wx>=minX && wx<=maxX);
        if (adjacent) { targetX=wx; targetZ=wz; } else clearQuote();
        return true;
    }
    boolean drag(double mx,double my,double dx,double dy) { if (!dragging) return false; centerX-=dx/zoom; centerZ-=dy/zoom; return true; }
    boolean quoted() { return targetX!=null; }
    long added(CompoundTag town) {
        if (!quoted()) return 0;
        long minX=town.getInt("TerritoryMinX"), maxX=town.getInt("TerritoryMaxX"), minZ=town.getInt("TerritoryMinZ"), maxZ=town.getInt("TerritoryMaxZ");
        return (Math.max(maxX,targetX)-Math.min(minX,targetX)+1)*(Math.max(maxZ,targetZ)-Math.min(minZ,targetZ)+1)-(maxX-minX+1)*(maxZ-minZ+1);
    }
    long cost(CompoundTag town) { return Math.multiplyExact(added(town),town.getLong("ExpansionCostPerBlock")); }
    String invalidReason(CompoundTag town) {
        if(!quoted())return "";
        if(!town.getBoolean("SameDimension"))return "dimension";
        BlockPos center=BlockPos.of(town.getLong("Center"));
        long minX=Math.min(town.getInt("TerritoryMinX"),targetX),maxX=Math.max(town.getInt("TerritoryMaxX"),targetX);
        long minZ=Math.min(town.getInt("TerritoryMinZ"),targetZ),maxZ=Math.max(town.getInt("TerritoryMaxZ"),targetZ);
        if(minX<(long)center.getX()-256 || maxX>(long)center.getX()+256 || minZ<(long)center.getZ()-256 || maxZ>(long)center.getZ()+256)return "limit";
        for(Tag value:town.getList("PublicTerritories",Tag.TAG_COMPOUND)) {
            CompoundTag other=(CompoundTag)value;
            if(town.hasUUID("Id")&&other.hasUUID("Id")&&town.getUUID("Id").equals(other.getUUID("Id")))continue;
            if(!other.getString("Dimension").equals(town.getString("Dimension")))continue;
            if(minX<=(long)other.getInt("MaxX")+16 && maxX>=(long)other.getInt("MinX")-16 && minZ<=(long)other.getInt("MaxZ")+16 && maxZ>=(long)other.getInt("MinZ")-16)return "neighbor";
        }
        if(cost(town)>town.getLong("Spendable"))return "funds";
        return "";
    }
    CompoundTag payload(CompoundTag town) {
        CompoundTag payload=new CompoundTag();
        payload.putInt("X",targetX); payload.putInt("Z",targetZ);
        for (String axis:new String[]{"MinX","MaxX","MinZ","MaxZ"}) payload.putInt("Expected"+axis,town.getInt("Territory"+axis));
        return payload;
    }
    private void rectangle(GuiGraphics g,int minX,int minZ,int maxX,int maxZ,int fill,int edge) {
        int left=sx(minX),top=sy(minZ),right=sx((double)maxX+1),bottom=sy((double)maxZ+1);
        g.fill(left,top,right,bottom,fill); g.fill(left,top,right,top+1,edge); g.fill(left,bottom-1,right,bottom,edge);
        g.fill(left,top,left+1,bottom,edge); g.fill(right-1,top,right,bottom,edge);
    }
    void render(GuiGraphics g,CompoundTag town,CompoundTag envelope,net.minecraft.client.Minecraft mc) {
        if (!centered) center(BlockPos.of(town.getLong("Center")));
        g.enableScissor(x,y,x+width,y+height);
        g.fill(x,y,x+width,y+height,0xffc8b88b);
        int spacing=16;
        for (int gx=(int)Math.floor(worldX(x)/spacing)*spacing;gx<=worldX(x+width);gx+=spacing) g.fill(sx(gx),y,sx(gx)+1,y+height,0x20735f39);
        for (int gz=(int)Math.floor(worldZ(y)/spacing)*spacing;gz<=worldZ(y+height);gz+=spacing) g.fill(x,sy(gz),x+width,sy(gz)+1,0x20735f39);
        for (Tag value:envelope.getList("PublicTerritories",Tag.TAG_COMPOUND)) {
            var row=(CompoundTag)value;
            if (row.getString("Dimension").equals(town.getString("Dimension"))) rectangle(g,row.getInt("MinX"),row.getInt("MinZ"),row.getInt("MaxX"),row.getInt("MaxZ"),0x506b748c,0xff585a71);
        }
        int minX=town.getInt("TerritoryMinX"),maxX=town.getInt("TerritoryMaxX"),minZ=town.getInt("TerritoryMinZ"),maxZ=town.getInt("TerritoryMaxZ");
        if (quoted()) {boolean valid=invalidReason(town).isEmpty();rectangle(g,Math.min(minX,targetX),Math.min(minZ,targetZ),Math.max(maxX,targetX),Math.max(maxZ,targetZ),valid?0x8078a252:0x80c85c45,valid?0xff395f27:0xff8f3027);}
        rectangle(g,minX,minZ,maxX,maxZ,0x806d9562,0xff314e2e);
        BlockPos anchor=BlockPos.of(town.getLong("Center")); int cx=sx(anchor.getX()+.5),cy=sy(anchor.getZ()+.5);
        g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BELL),cx-8,cy-12);
        if (mc.player!=null && mc.level!=null && mc.level.dimension().location().toString().equals(town.getString("Dimension"))) {
            int px=sx(mc.player.getX()),py=sy(mc.player.getZ()); g.fill(px-2,py-2,px+3,py+3,0xfffaf2d2); g.fill(px-1,py-1,px+2,py+2,0xff244a82);
        }
        g.disableScissor();
    }
}
