package com.siirio.jemvisualsandbox.workbench;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Pixel-authored rift contours clip the actual vanilla End portal shader. */
final class ElementalRenderer {
    private record Effect(ClientLevel level,Vec3 origin,float yaw,long start,int action,int delay) {}
    private record Run(int row,int first,int last) {}
    private static final List<Effect> ACTIVE=new ArrayList<>();
    private static final List<List<Run>> APERTURES=new ArrayList<>();
    private static long sculkTick=Long.MIN_VALUE;
    private static int sculkOrdinal;
    private ElementalRenderer() {}
    static void clear() { ACTIVE.clear();APERTURES.clear();sculkTick=Long.MIN_VALUE;sculkOrdinal=0; }
    static void receive(SandboxNetwork.Visual packet) {
        Minecraft c=Minecraft.getInstance();if(c.level==null)return;
        if(packet.action()!=5 && packet.action()!=7 && packet.action()!=9)return;
        if(ACTIVE.size()>=32)ACTIVE.remove(0);
        int delay=0;
        if(packet.action()==7) { long now=c.level.getGameTime();if(sculkTick!=now){sculkTick=now;sculkOrdinal=0;}delay=Math.min(7,sculkOrdinal++)*2; }
        ACTIVE.add(new Effect(c.level,packet.origin(),packet.yaw(),c.level.getGameTime(),packet.action(),delay));
    }
    private static ResourceLocation file(String name) { return new ResourceLocation("jem_visual_sandbox","elemental/"+name+".png"); }
    private static void aperture(Minecraft c) throws IOException {
        if(!APERTURES.isEmpty())return;
        try(var stream=c.getResourceManager().getResourceOrThrow(file("rift-mask")).open();var im=NativeImage.read(stream)) {
            if(im.getWidth()!=96 || im.getHeight()!=2304)throw new IOException("Invalid authored rift aperture dimensions");
            for(int f=0;f<24;f++) {
                List<Run> runs=new ArrayList<>();
                for(int y=0;y<96;y++) {
                    int first=-1;
                    for(int x=0;x<=96;x++) {
                        boolean filled=x<96 && (im.getPixelRGBA(x,f*96+y)>>>24)>0;
                        if(filled && first<0)first=x;
                        if(!filled && first>=0){runs.add(new Run(y,first,x));first=-1;}
                    }
                }
                APERTURES.add(runs);
            }
        }
    }
    static void render(RenderLevelStageEvent event) {
        Minecraft c=Minecraft.getInstance();
        ACTIVE.removeIf(e->c.level!=e.level || c.level==null || c.level.getGameTime()-e.start-e.delay >= (e.action==9?10:24));
        if(c.level==null || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported())){clear();return;}
        for(Effect e:ACTIVE) {
            int age=(int)(c.level.getGameTime()-e.start)-e.delay;if(age<0)continue;
            draw(VisualRenderContext.world(event),e.origin,e.yaw,e.action,age);
        }
    }
    static void draw(VisualRenderContext context,Vec3 origin,float yaw,int action,float age) {
        if(age<0 || age>=(action==9?10:24))return;
        Minecraft c=Minecraft.getInstance();
            int frame=(int)age+(action==9?14:0);if(frame>=24)return;
            boolean rift=action!=7;
            var poses=context.getPoseStack();poses.pushPose();Vec3 camera=context.getCamera().getPosition();
            poses.translate(origin.x-camera.x,origin.y-camera.y+(rift?2.5:.045),origin.z-camera.z);
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
            if(!rift)poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            var buffers=c.renderBuffers().bufferSource();var pose=poses.last();
            if(rift) {
                try { aperture(c); }
                catch(IOException error){poses.popPose();throw new IllegalStateException("Authored rift aperture missing/invalid",error);}
                var type=RenderType.endPortal();var out=buffers.getBuffer(type);
                for(Run run:APERTURES.get(frame)) {
                    float left=(run.first/96F-.5F)*5,right=(run.last/96F-.5F)*5,top=(.5F-run.row/96F)*5,bottom=(.5F-(run.row+1)/96F)*5;
                    for(float[] v:new float[][]{{left,bottom},{right,bottom},{right,top},{left,top},{left,top},{right,top},{right,bottom},{left,bottom}})out.vertex(pose.pose(),v[0],v[1],0).endVertex();
                }
                buffers.endBatch(type);
            }
            var type=RenderType.entityCutoutNoCull(file(rift?"rift":"sculk"));var out=buffers.getBuffer(type);float half=rift?2.5F:1.1F;
            for(float[] v:new float[][]{{-half,-half,0,1},{half,-half,1,1},{half,half,1,0},{-half,half,0,0}})out.vertex(pose.pose(),v[0],v[1],.004F).color(255,255,255,255).uv(v[2],(frame+v[3])/24F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.normal(),0,0,1).endVertex();
            buffers.endBatch(type);poses.popPose();
    }
}
