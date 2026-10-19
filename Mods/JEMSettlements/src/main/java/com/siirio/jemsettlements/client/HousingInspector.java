package com.siirio.jemsettlements.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Detached snapshot rendering. All clipping is local geometry selection, never a world edit. */
public final class HousingInspector {
    private record Cell(BlockPos pos,BlockState state,CompoundTag facts) {
        boolean relevant(){return facts.getBoolean("Bed")||facts.contains("Category");}
        Vector3f center(){return new Vector3f(pos.getX()+.5F,pos.getY()+.5F,pos.getZ()+.5F);}
    }
    private CompoundTag snapshot=new CompoundTag();
    private final List<Cell> actual=new ArrayList<>(),sample=new ArrayList<>(),visible=new ArrayList<>();
    private float yaw=135,pitch=28,targetYaw=135,targetPitch=28,zoom=1,panX,panY;
    private float cachedYaw=Float.NaN,cachedPitch=Float.NaN;
    private int depth,cachedDepth=-1,x,y,w,h;
    private boolean cachedExample;
    public boolean example,onlyAmenities,surfaceOnly,showBounds=true;
    public void refreshGeometry(){invalidate();}
    private long lastFrame;
    private final java.util.Map<Long,Integer> surface=new java.util.HashMap<>();
    private final Vector3f focus=new Vector3f(),interest=new Vector3f(),minimum=new Vector3f(),maximum=new Vector3f();

    public void accept(CompoundTag data){
        snapshot=data.copy();actual.clear();sample.clear();surface.clear();invalidate();
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var palette=data.getList("Palette",Tag.TAG_COMPOUND);var rows=data.getList("Blocks",Tag.TAG_COMPOUND);BlockPos anchor=BlockPos.of(data.getLong("Anchor"));
        for(int i=0;i<Math.min(data.getBoolean("City")?16384:3179,rows.size());i++){
            var row=rows.getCompound(i);int id=row.getInt("State");
            if(id>=0&&id<palette.size())actual.add(new Cell(BlockPos.of(row.getLong("Position")).subtract(anchor),NbtUtils.readBlockState(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK),palette.getCompound(id)),row));
        }
        for(Cell cell:actual)surface.merge(net.minecraft.world.level.ChunkPos.asLong(cell.pos().getX(),cell.pos().getZ()),cell.pos().getY(),Math::max);
        for(int sx=-3;sx<=3;sx++)for(int sz=-3;sz<=3;sz++){
            sample.add(new Cell(new BlockPos(sx,-1,sz),Blocks.OAK_PLANKS.defaultBlockState(),new CompoundTag()));
            if(Math.abs(sx)==3||Math.abs(sz)==3)for(int sy=0;sy<=3;sy++)sample.add(new Cell(new BlockPos(sx,sy,sz),Blocks.OAK_PLANKS.defaultBlockState(),new CompoundTag()));
            sample.add(new Cell(new BlockPos(sx,4,sz),Blocks.OAK_SLAB.defaultBlockState(),new CompoundTag()));
        }
        CompoundTag bed=new CompoundTag();bed.putBoolean("Bed",true);sample.add(new Cell(BlockPos.ZERO,Blocks.RED_BED.defaultBlockState(),bed));
        ListTag examples=data.getList("ExampleAmenities",Tag.TAG_COMPOUND);
        for(int i=0;i<examples.size();i++){
            CompoundTag facts=examples.getCompound(i);var id=net.minecraft.resources.ResourceLocation.tryParse(facts.getString("Block"));
            if(id!=null&&net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id))sample.add(new Cell(new BlockPos(i-1,0,2),net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState(),facts));
        }
    }
    private void invalidate(){cachedDepth=-1;}
    public int depth(){return depth;}
    public void depth(int value){depth=Math.max(0,Math.min(10,value));}
    public void resetView(){yaw=targetYaw=135;pitch=targetPitch=28;zoom=1;panX=panY=0;lastFrame=0;invalidate();}
    public boolean inside(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    public void rotate(double dx,double dy){targetYaw+=(float)dx*.7F;targetPitch=Math.max(-10,Math.min(89,targetPitch+(float)dy*.5F));}
    public void pan(double dx,double dy){panX=Math.max(-w*.4F,Math.min(w*.4F,panX+(float)dx));panY=Math.max(-h*.4F,Math.min(h*.4F,panY+(float)dy));}
    public void zoom(double delta){zoom=Math.max(.55F,Math.min(3F,zoom*(float)Math.pow(1.12,delta)));}

    private void camera(){
        long now=System.nanoTime();float elapsed=lastFrame==0?1/60F:Math.min(.1F,(now-lastFrame)/1_000_000_000F);lastFrame=now;
        float blend=1-(float)Math.exp(-18*elapsed);yaw+=(targetYaw-yaw)*blend;pitch+=(targetPitch-pitch)*blend;
        if(Math.abs(targetYaw-yaw)<.02F)yaw=targetYaw;if(Math.abs(targetPitch-pitch)<.02F)pitch=targetPitch;
    }
    private Matrix4f rotation(){return new Matrix4f().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw));}

    private void geometry(List<Cell> cells,Matrix4f rotation){
        if(cachedDepth==depth&&cachedExample==example&&Math.abs(cachedYaw-yaw)<.6F&&Math.abs(cachedPitch-pitch)<.6F)return;
        cachedDepth=depth;cachedExample=example;cachedYaw=yaw;cachedPitch=pitch;visible.clear();
        minimum.set(Float.POSITIVE_INFINITY);maximum.set(Float.NEGATIVE_INFINITY);interest.zero();int relevant=0;
        for(Cell cell:cells){
            Vector3f center=cell.center();
            float span=footprint(cell);
            minimum.min(new Vector3f(center).sub(.5F,.5F,.5F));
            maximum.max(new Vector3f(center).add(span-.5F,.5F,span-.5F));
            if(cell.relevant()){interest.add(center);relevant++;}
        }
        if(cells.isEmpty()){minimum.set(-.5F);maximum.set(.5F);}
        if(!example&&snapshot.contains("Radius")&&snapshot.contains("VerticalRadius")){
            int radius=snapshot.getInt("Radius"),vertical=snapshot.getInt("VerticalRadius");
            // Fit the same authoritative scan box that is outlined below, including sparse
            // homes. User zoom and pan still deliberately move beyond this reset framing.
            BlockPos anchor=BlockPos.of(snapshot.getLong("Anchor"));
            if(snapshot.getBoolean("City")) {
                minimum.min(new Vector3f(snapshot.getInt("MinX")-anchor.getX(),minimum.y,snapshot.getInt("MinZ")-anchor.getZ()));
                maximum.max(new Vector3f(snapshot.getInt("MaxX")-anchor.getX()+1,maximum.y,snapshot.getInt("MaxZ")-anchor.getZ()+1));
            } else {
                minimum.min(new Vector3f(-radius,-vertical,-radius));
                maximum.max(new Vector3f(radius+1,vertical+1,radius+1));
            }
        }
        focus.set(minimum).add(maximum).mul(.5F);if(relevant>0)interest.div(relevant);else interest.set(focus);
        if(depth==0){visible.addAll(cells);return;}
        // GUI depth regards +Z as camera-facing. Invert the very same rotation used to draw,
        // including pitch, so neither world axes nor the player's world camera choose a wall.
        Vector3f camera=rotation.invert(new Matrix4f()).transformDirection(new Vector3f(0,0,1)).normalize();
        float radius=.8F+depth*.8F,front=-Float.MAX_VALUE;
        float halfBlockProjection=(Math.abs(camera.x)+Math.abs(camera.y)+Math.abs(camera.z))*.5F;
        for(Cell cell:cells){
            Vector3f relative=cell.center().sub(interest);float along=relative.dot(camera);
            float lateral=relative.lengthSquared()-along*along;
            if(lateral<=radius*radius)front=Math.max(front,along+halfBlockProjection);
        }
        float sphereCenter=depth*.25F,sphereRadius=depth*.75F;
        for(Cell cell:cells){
            if(cell.relevant()){visible.add(cell);continue;}
            Vector3f relative=cell.center().sub(interest);float along=relative.dot(camera),inward=front-along;
            float lateral=Math.max(0,relative.lengthSquared()-along*along);
            // Flattened sphere cap: voxel selection grows in width and depth, but its farthest
            // removed block center can never exceed the user's block-depth limit.
            float rounded=1-(inward-sphereCenter)*(inward-sphereCenter)/(sphereRadius*sphereRadius);
            if(along<0||inward<0||inward>depth||rounded<=0||lateral>radius*radius*rounded)visible.add(cell);
        }
    }
    private int footprint(Cell cell){
        if(example||!snapshot.getBoolean("City"))return 1;
        return cell.facts().getBoolean("Coarse")?Math.max(1,snapshot.getInt("Stride")):Math.max(1,cell.facts().getInt("Span"));
    }
    private float fittedScale(Matrix4f rotation,int width,int height){
        float extentX=1,extentY=1;
        for(int corner=0;corner<8;corner++){
            Vector3f point=new Vector3f((corner&1)==0?minimum.x:maximum.x,(corner&2)==0?minimum.y:maximum.y,(corner&4)==0?minimum.z:maximum.z).sub(focus);
            rotation.transformPosition(point);extentX=Math.max(extentX,Math.abs(point.x)*2);extentY=Math.max(extentY,Math.abs(point.y)*2);
        }
        return Math.max(.0001F,Math.min((width-16)/extentX,(height-16)/extentY))*zoom;
    }
    public void render(GuiGraphics g,int x,int y,int w,int h,int mouseX,int mouseY){
        this.x=x;this.y=y;this.w=w;this.h=h;var mc=Minecraft.getInstance();camera();Matrix4f rotation=rotation();geometry(example?sample:actual,rotation);
        g.fill(x,y,x+w,y+h,0xff171d21);g.renderOutline(x,y,w,h,0xff777f80);
        var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();g.flush();g.enableScissor(x+1,y+1,x+w-1,y+h-1);
        Cell hovered=null;float nearestZ=-Float.MAX_VALUE;double best=Double.MAX_VALUE;float scale=fittedScale(rotation,w,h);
        pose.pushPose();
        try{
            pose.translate(x+w*.5+panX,y+h*.5+panY,300);pose.scale(scale,-scale,scale);pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.translate(-focus.x,-focus.y,-focus.z);
            Matrix4f matrix=new Matrix4f(pose.last().pose());RenderSystem.enableDepthTest();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            for(Cell cell:visible){
                if(onlyAmenities&&!cell.facts().getBoolean("Counted"))continue;
                if(surfaceOnly&&cell.pos().getY()<surface.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(cell.pos().getX(),cell.pos().getZ()),Integer.MIN_VALUE))continue;
                var pos=cell.pos();pose.pushPose();pose.translate(pos.getX(),pos.getY(),pos.getZ());
                if(snapshot.getBoolean("City")&&footprint(cell)>1) {
                    BlockPos anchor=BlockPos.of(snapshot.getLong("Anchor"));int stride=footprint(cell);
                    int spanX=Math.max(1,Math.min(stride,snapshot.getInt("MaxX")-anchor.getX()-pos.getX()+1));
                    int spanZ=Math.max(1,Math.min(stride,snapshot.getInt("MaxZ")-anchor.getZ()-pos.getZ()+1));
                    pose.scale(spanX,1,spanZ);
                }
                mc.getBlockRenderer().renderSingleBlock(cell.state(),pose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                if(cell.relevant())LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),0,0,0,1,1,1,cell.facts().getBoolean("Bed")?.3F:.25F,cell.facts().getBoolean("Bed")?1F:.65F,cell.facts().getBoolean("Bed")?.4F:1F,.85F);
                pose.popPose();
                Vector3f projected=matrix.transformPosition(cell.center());double distance=Math.pow(projected.x-mouseX,2)+Math.pow(projected.y-mouseY,2);
                if(distance<scale*scale*.65&&(distance<best*.65||projected.z>nearestZ)){hovered=cell;nearestZ=projected.z;best=distance;}
            }
            if(!example&&showBounds){
                int radius=snapshot.getInt("Radius"),vertical=snapshot.getInt("VerticalRadius");
                if(snapshot.getBoolean("City")) {
                    BlockPos anchor=BlockPos.of(snapshot.getLong("Anchor"));
                    LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),snapshot.getInt("MinX")-anchor.getX(),minimum.y,snapshot.getInt("MinZ")-anchor.getZ(),snapshot.getInt("MaxX")-anchor.getX()+1,maximum.y,snapshot.getInt("MaxZ")-anchor.getZ()+1,.8F,.72F,.4F,.5F);
                } else LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),-radius,-vertical,-radius,radius+1,vertical+1,radius+1,.8F,.72F,.4F,.22F);
                if(!snapshot.getBoolean("City")&&!snapshot.getBoolean("BedPresent")&&!snapshot.getBoolean("Unloaded")&&!snapshot.getBoolean("NoHome"))LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),0,0,0,1,1,2,1,.2F,.2F,1);
            }
            buffers.endBatch();
        }finally{
            pose.popPose();g.disableScissor();RenderSystem.disableDepthTest();com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        }
        if(!example&&(snapshot.getBoolean("NoHome")||snapshot.getBoolean("Unloaded")))g.drawWordWrap(mc.font,tr(snapshot.getBoolean("NoHome")?"no_home":"unloaded"),x+6,y+6,w-12,0xffe1b573);
        if(inside(mouseX,mouseY)&&hovered!=null){
            List<Component> lines=new ArrayList<>();lines.add(hovered.state().getBlock().getName());CompoundTag facts=hovered.facts();
            if(snapshot.getBoolean("City")&&facts.contains("Category")) {
                lines.add(Component.translatable("jem.settlements.city.category",Component.translatable("jem.settlements.amenity."+facts.getString("Category"))));
                if(!facts.getBoolean("Active"))lines.add(tr("unlit"));
            } else {
                lines.add(tr(facts.getBoolean("Bed")?"bed_rule":facts.contains("Category")?facts.getBoolean("Counted")?"counted":!facts.getBoolean("Active")?"unlit":!facts.getBoolean("Unique")?"duplicate":"capped":"structure"));
                if(facts.contains("Category")){lines.add(tr("category",Component.translatable("jem.settlements.amenity."+facts.getString("Category")),facts.getInt("Cap")));lines.add(tr("points_rule"));}
            }
            if(!example){var absolute=hovered.pos().offset(BlockPos.of(snapshot.getLong("Anchor")));lines.add(Component.literal(absolute.getX()+", "+absolute.getY()+", "+absolute.getZ()));}
            g.renderComponentTooltip(mc.font,lines,mouseX,mouseY);
        }
    }
    public Component score(){return tr("score",snapshot.getInt("CurrentPoints"),snapshot.getInt("VerifiedPoints"));}
    public static Component tr(String key,Object...args){return Component.translatable("jem.settlements.housing."+key,args);}
}
