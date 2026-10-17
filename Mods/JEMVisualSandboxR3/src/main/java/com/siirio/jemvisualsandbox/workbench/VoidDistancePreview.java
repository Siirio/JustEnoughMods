package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Read-only ballistic estimate. The integrated server still resolves safe arrival. */
final class VoidDistancePreview {
    private static Vec3 landing;
    private static boolean impact;
    static void clear(){landing=null;impact=false;}
    static void render(RenderLevelStageEvent event,boolean enabled){
        var c=Minecraft.getInstance();
        if(!enabled || c.player==null || c.level==null || !c.hasSingleplayerServer()){clear();return;}
        Vec3 point=c.player.getEyePosition().add(0,-.1,0),velocity=c.player.getLookAngle().scale(1.5);
        Vec3 motion=c.player.getDeltaMovement();velocity=velocity.add(motion.x,c.player.onGround()?0:motion.y,motion.z);impact=false;
        for(int tick=0;tick<100;tick++){
            Vec3 next=point.add(velocity);
            var block=c.level.clip(new ClipContext(point,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));
            Vec3 end=block.getType()==HitResult.Type.MISS?next:block.getLocation();
            Vec3 nearest=end;
            for(var entity:c.level.getEntities(c.player,new net.minecraft.world.phys.AABB(point,end).inflate(.3),v->v.isPickable() && !v.isSpectator())){
                var hit=entity.getBoundingBox().inflate(.3).clip(point,end);
                if(hit.isPresent() && point.distanceToSqr(hit.get())<point.distanceToSqr(nearest)){nearest=hit.get();impact=true;}
            }
            if(impact || block.getType()!=HitResult.Type.MISS){point=nearest;impact=true;break;}
            point=next;velocity=velocity.scale(c.level.getFluidState(net.minecraft.core.BlockPos.containing(point)).is(net.minecraft.tags.FluidTags.WATER)?.8:.99).add(0,-.03,0);
        }
        landing=point;if(!impact)return;
        var poses=event.getPoseStack();poses.pushPose();Vec3 cam=event.getCamera().getPosition();poses.translate(point.x-cam.x,point.y-cam.y+.08,point.z-cam.z);poses.mulPose(c.getEntityRenderDispatcher().cameraOrientation());
        var type=RenderType.entityTranslucent(new ResourceLocation("jem_visual_sandbox","life_chorus/landing.png"));var buffers=c.renderBuffers().bufferSource();var out=buffers.getBuffer(type);var p=poses.last();
        for(float[] q:new float[][]{{-.4F,-.4F,0,1},{.4F,-.4F,1,1},{.4F,.4F,1,0},{-.4F,.4F,0,0}})out.vertex(p.pose(),q[0],q[1],0).color(255,255,255,210).uv(q[2],q[3]/24).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,0,1).endVertex();
        buffers.endBatch(type);poses.popPose();
    }
    static void hud(RenderGuiEvent.Post e){
        var c=Minecraft.getInstance();if(landing==null || c.player==null || c.screen!=null)return;
        String text=String.format(java.util.Locale.ROOT,"~ %.1f m%s",c.player.position().distanceTo(landing),impact?"":" +");
        e.getGuiGraphics().drawCenteredString(c.font,text,c.getWindow().getGuiScaledWidth()/2,c.getWindow().getGuiScaledHeight()/2+25,0xc69aef);
    }
}
