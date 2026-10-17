package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Distinct externally authored cels; attached to the actual server-accepted subject. */
final class UtilityVfx {
    private record Effect(ClientLevel level,long start,int action,int subject,Vec3 origin) {}
    private static final List<Effect> ACTIVE=new ArrayList<>();
    private UtilityVfx() {}
    static void clear(){ACTIVE.clear();}
    static void receive(SandboxNetwork.Visual packet) {
        var c=Minecraft.getInstance();if(c.level==null || !c.hasSingleplayerServer())return;
        if(ACTIVE.size()==48)ACTIVE.remove(0);ACTIVE.add(new Effect(c.level,c.level.getGameTime(),packet.action(),packet.subject(),packet.origin()));
    }
    static void render(RenderLevelStageEvent event) {
        var c=Minecraft.getInstance();if(c.level==null || !c.hasSingleplayerServer()){clear();return;}
        ACTIVE.removeIf(e->e.level!=c.level || c.level.getGameTime()-e.start>= (e.action==28?100:24));
        for(Effect e:ACTIVE) {
            var subject=e.subject<0?null:c.level.getEntity(e.subject);if(e.subject>=0 && subject==null)continue;
            Vec3 origin=subject==null?e.origin:subject.getPosition(event.getPartialTick());
            if(e.action==21 && subject!=null)origin=origin.add(subject.getLookAngle().scale(.8));
            int age=(int)(c.level.getGameTime()-e.start),frame=e.action==28?age%16:age;
            String file=switch(e.action){case 20,27->"chorus";case 21,28,29->"void_orb";case 22->"gravity";case 23->"path_leaf";case 24->"echo_scan";case 25->"echo_absorb";case 26->"lava_drink";default->throw new IllegalArgumentException("Unknown utility VFX");};
            boolean ground=e.action==23;float size=e.action==24?5:e.action==28?.65F:ground?1.1F:2.6F;
            var poses=event.getPoseStack();poses.pushPose();Vec3 camera=event.getCamera().getPosition();
            poses.translate(origin.x-camera.x,origin.y-camera.y+(ground?.02:e.action==28?0:1.0),origin.z-camera.z);
            if(ground)poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));else poses.mulPose(c.getEntityRenderDispatcher().cameraOrientation());
            var type=RenderType.entityCutoutNoCull(new ResourceLocation("jem_visual_sandbox","elemental/"+file+".png"));var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);var pose=poses.last();
            for(float[] v:new float[][]{{-.5F,-.5F,0,1},{.5F,-.5F,1,1},{.5F,.5F,1,0},{-.5F,.5F,0,0}})out.vertex(pose.pose(),v[0]*size,v[1]*size,0).color(255,255,255,255).uv(v[2],(frame+v[3])/24F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.normal(),0,0,1).endVertex();
            buffers.endBatch(type);poses.popPose();
        }
    }
}
