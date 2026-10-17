package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** One continuous UV strip follows server-resolved visible terrain, plus painted fire flanks. */
final class SurfaceVfx {
    private record Trail(ClientLevel level,long start,int kind,List<Vec3> points) {}
    private static final List<Trail> ACTIVE=new ArrayList<>();
    private SurfaceVfx() {}
    static void clear(){ACTIVE.clear();}
    static void receive(SandboxNetwork.Path packet) {
        var c=Minecraft.getInstance();if(c.level==null || !c.hasSingleplayerServer() || packet.points().size()<2)return;
        if(ACTIVE.size()==16)ACTIVE.remove(0);
        ACTIVE.add(new Trail(c.level,c.level.getGameTime(),packet.kind(),packet.points()));
    }
    static void render(RenderLevelStageEvent event) {
        var c=Minecraft.getInstance();if(c.level==null || !c.hasSingleplayerServer()){clear();return;}
        ACTIVE.removeIf(t->t.level!=c.level || c.level.getGameTime()-t.start>=48);
        for(Trail t:ACTIVE) {
            double age=c.level.getGameTime()-t.start+event.getPartialTick();int frame=Math.min(23,(int)(age/2));
            int count=t.points.size()-1,visible=Math.min(count,Math.max(1,(int)Math.ceil(count*(age+1)/17)));
            var poses=event.getPoseStack();poses.pushPose();Vec3 camera=event.getCamera().getPosition();poses.translate(-camera.x,-camera.y,-camera.z);
            var buffers=c.renderBuffers().bufferSource();
            var type=RenderType.entityCutoutNoCull(new ResourceLocation("jem_visual_sandbox","elemental/"+(t.kind==0?"ground_fissure":"sculk")+".png"));
            var out=buffers.getBuffer(type);
            for(int i=0;i<visible;i++) {
                Vec3 a=t.points.get(i),b=t.points.get(i+1),side=side(a,b).scale(t.kind==0?.85:.6);
                quad(out,poses.last(),a.add(side),b.add(side),b.subtract(side),a.subtract(side),i/(float)count,(i+1)/(float)count,frame,false);
            }
            buffers.endBatch(type);
            if(t.kind==0) {
                type=RenderType.entityCutoutNoCull(new ResourceLocation("jem_visual_sandbox","elemental/fire_wall.png"));out=buffers.getBuffer(type);
                for(int i=0;i<visible;i++) {
                    Vec3 a=t.points.get(i),b=t.points.get(i+1),side=side(a,b).scale(.9);
                    for(int sign:new int[]{-1,1}) {
                        Vec3 left=a.add(side.scale(sign)),right=b.add(side.scale(sign));
                        quad(out,poses.last(),left,right,right.add(0,1.8,0),left.add(0,1.8,0),i/(float)count,(i+1)/(float)count,frame,true);
                    }
                }
                buffers.endBatch(type);
            }
            poses.popPose();
        }
    }
    private static Vec3 side(Vec3 a,Vec3 b) { Vec3 d=b.subtract(a).multiply(1,0,1).normalize();return new Vec3(-d.z,0,d.x); }
    private static void quad(VertexConsumer out,com.mojang.blaze3d.vertex.PoseStack.Pose pose,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float from,float to,int frame,boolean wall) {
        Vec3[] points={a,b,c,d};float[][] uv=wall?new float[][]{{from,1},{to,1},{to,0},{from,0}}:new float[][]{{0,from},{0,to},{1,to},{1,from}};
        for(int i=0;i<4;i++){Vec3 p=points[i];out.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).color(255,255,255,255).uv(uv[i][0],(frame+uv[i][1])/24F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.normal(),0,1,0).endVertex();}
    }
}
