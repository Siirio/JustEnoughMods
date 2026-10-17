package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cosmetic history of actual accepted pearl entities; never predicts gameplay. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox", value=Dist.CLIENT)
public final class PearlAfterfire {
    private static final ResourceLocation TEXTURE=new ResourceLocation("jem_visual_sandbox","ancients_pearl/pearl_afterfire.png");
    private static final Map<Integer,Trail> TRAILS=new LinkedHashMap<>();
    private static ClientLevel level;
    private static final class Trail {
        final long born;
        long lastSeen;
        boolean seen;
        final List<Vec3> points=new ArrayList<>();
        Trail(long now,Vec3 origin){born=lastSeen=now;points.add(origin);}
    }
    private PearlAfterfire() {}
    static void clear(){TRAILS.clear();level=null;}
    static void receive(SandboxNetwork.Visual packet){
        var client=Minecraft.getInstance();
        if(client.level==null || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported()) || packet.subject()<0)return;
        if(level!=client.level){clear();level=client.level;}
        if(TRAILS.size()>=16)TRAILS.remove(TRAILS.keySet().iterator().next());
        TRAILS.put(packet.subject(),new Trail(level.getGameTime(),packet.origin()));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var client=Minecraft.getInstance();
        if(client.level==null || level!=client.level || !(com.siirio.jemvisualsandbox.client.races.RaceExperienceClient.supported())){clear();return;}
        long now=level.getGameTime();
        TRAILS.entrySet().removeIf(entry->{
            Trail trail=entry.getValue();var pearl=level.getEntity(entry.getKey());
            if(pearl!=null && pearl.isAlive()){
                trail.seen=true;trail.lastSeen=now;
                Vec3 pos=pearl.position();
                if(trail.points.get(0).distanceToSqr(pos)>.00001){
                    // Never bridge a discontinuity into a world-spanning ribbon.
                    if(trail.points.get(0).distanceToSqr(pos)>64)trail.points.clear();
                    trail.points.add(0,pos);if(trail.points.size()>12)trail.points.remove(12);
                }
            }
            return now-trail.born>200 || now-trail.lastSeen>(trail.seen?6:10);
        });
    }
    static void render(RenderLevelStageEvent event){
        var client=Minecraft.getInstance();
        if(client.level==null || level!=client.level)return;
        double now=level.getGameTime()+event.getPartialTick();
        for(var entry:TRAILS.entrySet()){
            Trail trail=entry.getValue();if(trail.points.size()<2)continue;
            var pearl=level.getEntity(entry.getKey());boolean alive=pearl!=null && pearl.isAlive();
            Vec3 head=alive?pearl.getPosition(event.getPartialTick()):trail.points.get(0);
            float alpha=alive?1:(float)Math.max(0,1-(now-trail.lastSeen)/6);
            int frame=(int)((now-trail.born)/2)%8;
            draw(VisualRenderContext.world(event),trail.points,head,frame,alpha);
        }
    }
    static void draw(VisualRenderContext context,List<Vec3> points,Vec3 head,int frame,float alpha) {
        if(points.size()<2 || alpha<=0)return;
        var client=Minecraft.getInstance();
        var type=RenderType.entityTranslucent(TEXTURE);
        var buffers=client.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        var pose=context.getPoseStack().last();Vec3 camera=context.getCamera().getPosition();
            Vec3 previous=head,previousSide=null;double distance=0;
            for(int i=1;i<points.size() && distance<2.6;i++){
                Vec3 next=points.get(i),delta=next.subtract(previous);
                double length=delta.length();if(length<.0001)continue;
                if(distance+length>2.6){next=previous.add(delta.scale((2.6-distance)/length));length=2.6-distance;delta=next.subtract(previous);}
                Vec3 side=delta.normalize().cross(camera.subtract(previous.add(next).scale(.5))).normalize();
                if(side.lengthSqr()<.01)side=Math.abs(delta.normalize().y)>.9?new Vec3(1,0,0):new Vec3(0,1,0);
                if(previousSide!=null && side.dot(previousSide)<0)side=side.scale(-1);
                Vec3 a=previousSide==null?side:previousSide;
                Vec3[] vertices={previous.add(a.scale(.21)),next.add(side.scale(.21)),next.subtract(side.scale(.21)),previous.subtract(a.scale(.21))};
                float u0=(float)(1-distance/2.6),u1=(float)(1-(distance+length)/2.6);
                float[][] uv={{u0,frame/8F},{u1,frame/8F},{u1,(frame+1)/8F},{u0,(frame+1)/8F}};
                for(int v=0;v<4;v++){
                    Vec3 p=vertices[v].subtract(camera);
                    out.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).color(255,255,255,(int)(alpha*255)).uv(uv[v][0],uv[v][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.normal(),0,1,0).endVertex();
                }
                distance+=length;previous=next;previousSide=side;
            }
        buffers.endBatch(type);
    }

}
