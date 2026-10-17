package com.siirio.jemsettlements.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.*;
import org.joml.Vector3f;

/** Detached native block render; no world mutation, ticking entities or client-side housing authority. */
public final class HousingInspector {
    private record Cell(BlockPos pos,BlockState state,CompoundTag facts) {}
    private CompoundTag snapshot=new CompoundTag();
    private final java.util.List<Cell> actual=new java.util.ArrayList<>(),sample=new java.util.ArrayList<>();
    private float yaw=135,pitch=28,zoom=7;
    public boolean example,cutaway=true,exploded;
    private int x,y,w,h;
    public void accept(CompoundTag data){
        snapshot=data.copy();actual.clear();var mc=Minecraft.getInstance();if(mc.level==null)return;
        var palette=data.getList("Palette",Tag.TAG_COMPOUND);var rows=data.getList("Blocks",Tag.TAG_COMPOUND);BlockPos anchor=BlockPos.of(data.getLong("Anchor"));
        for(int i=0;i<Math.min(3179,rows.size());i++){var row=rows.getCompound(i);int id=row.getInt("State");if(id>=0&&id<palette.size())actual.add(new Cell(BlockPos.of(row.getLong("Position")).subtract(anchor),NbtUtils.readBlockState(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK),palette.getCompound(id)),row));}
        sample.clear();
        for(int sx=-3;sx<=3;sx++)for(int sz=-3;sz<=3;sz++){
            sample.add(new Cell(new BlockPos(sx,-1,sz),Blocks.OAK_PLANKS.defaultBlockState(),new CompoundTag()));
            if(Math.abs(sx)==3||Math.abs(sz)==3)for(int sy=0;sy<=3;sy++)sample.add(new Cell(new BlockPos(sx,sy,sz),Blocks.OAK_PLANKS.defaultBlockState(),new CompoundTag()));
        }
        CompoundTag bed=new CompoundTag();bed.putBoolean("Bed",true);sample.add(new Cell(BlockPos.ZERO,Blocks.RED_BED.defaultBlockState(),bed));
        ListTag examples=data.getList("ExampleAmenities",Tag.TAG_COMPOUND);
        for(int i=0;i<examples.size();i++){
            CompoundTag facts=examples.getCompound(i);var id=net.minecraft.resources.ResourceLocation.tryParse(facts.getString("Block"));
            if(id!=null&&net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id))sample.add(new Cell(new BlockPos(i-1,0,2),net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState(),facts));
        }
    }
    public boolean inside(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    public void rotate(double dx,double dy){yaw+=(float)dx*.7F;pitch=Math.max(-10,Math.min(80,pitch+(float)dy*.5F));}
    public void zoom(double delta){zoom=Math.max(2,Math.min(22,zoom+(float)delta));}
    public void render(GuiGraphics g,int x,int y,int w,int h,int mouseX,int mouseY){
        this.x=x;this.y=y;this.w=w;this.h=h;var mc=Minecraft.getInstance();
        g.fill(x,y,x+w,y+h,0xff171d21);g.renderOutline(x,y,w,h,0xff777f80);
        if(!example&&(snapshot.getBoolean("NoHome")||snapshot.getBoolean("Unloaded")))g.drawWordWrap(mc.font,tr(snapshot.getBoolean("NoHome")?"no_home":"unloaded"),x+6,y+6,w-12,0xffe1b573);
        var cells=example?sample:actual;var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();g.flush();g.enableScissor(x,y,x+w,y+h);
        pose.pushPose();pose.translate(x+w*.5,y+h*.6,300);float scale=Math.min(w/26F,h/23F)*(zoom/7F);pose.scale(scale,-scale,scale);pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.mulPose(Axis.YP.rotationDegrees(yaw));
        var matrix=new org.joml.Matrix4f(pose.last().pose());Cell hovered=null;float nearestZ=-Float.MAX_VALUE;double best=Double.MAX_VALUE;
        RenderSystem.enableDepthTest();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        for(Cell cell:cells){
            var pos=cell.pos();float depth=(float)(pos.getX()*Math.sin(Math.toRadians(yaw))+pos.getZ()*Math.cos(Math.toRadians(yaw)));
            if(cutaway && pos.getY()>1 && depth>0 && !cell.facts().contains("Category"))continue;
            float py=pos.getY()+(exploded?Math.max(0,pos.getY())*.65F:0);
            pose.pushPose();pose.translate(pos.getX(),py,pos.getZ());
            mc.getBlockRenderer().renderSingleBlock(cell.state(),pose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            boolean decor=cell.facts().contains("Category");float red=decor?.25F:.2F,green=decor?.65F:.85F,blue=decor?1F:.35F;
            LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),0,0,0,1,1,1,red,green,blue,.65F);pose.popPose();
            var projected=matrix.transformPosition(new Vector3f(pos.getX()+.5F,py+.5F,pos.getZ()+.5F));double distance=Math.pow(projected.x-mouseX,2)+Math.pow(projected.y-mouseY,2);
            if(distance<scale*scale*.8 && (distance<best*.65||projected.z>nearestZ)){hovered=cell;nearestZ=projected.z;best=distance;}
        }
        if(!example && !snapshot.getBoolean("BedPresent")&&!snapshot.getBoolean("Unloaded"))LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),0,0,0,1,1,2,1,.2F,.2F,1);
        buffers.endBatch();pose.popPose();g.disableScissor();RenderSystem.disableDepthTest();com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        g.drawString(mc.font,tr(example?"example_note":"area"),x+5,y+h-12,0xffd1d4ce,false);
        if(inside(mouseX,mouseY)&&hovered!=null){
            java.util.List<Component> lines=new java.util.ArrayList<>();lines.add(hovered.state().getBlock().getName());
            CompoundTag facts=hovered.facts();lines.add(tr(facts.getBoolean("Bed")?"bed_rule":facts.contains("Category")?facts.getBoolean("Counted")?"counted":!facts.getBoolean("Active")?"unlit":!facts.getBoolean("Unique")?"duplicate":"capped":"structure"));
            if(facts.contains("Category")){lines.add(tr("category",facts.getString("Category"),facts.getInt("Cap")));lines.add(tr("points_rule"));}
            if(!example){var absolute=hovered.pos().offset(BlockPos.of(snapshot.getLong("Anchor")));lines.add(Component.literal(absolute.getX()+", "+absolute.getY()+", "+absolute.getZ()));}
            g.renderComponentTooltip(mc.font,lines,mouseX,mouseY);
        }
    }
    public Component score(){return tr("score",snapshot.getInt("CurrentPoints"),snapshot.getInt("VerifiedPoints"));}
    public static Component tr(String key,Object...args){return Component.translatable("jem.settlements.housing."+key,args);}
}
