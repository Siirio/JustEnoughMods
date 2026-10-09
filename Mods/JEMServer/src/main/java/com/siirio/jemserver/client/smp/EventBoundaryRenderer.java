package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.EventNetwork;
import com.siirio.jemserver.smp.events.BloodMoonSolidBoundary;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid="jem_server",value=Dist.CLIENT)
public final class EventBoundaryRenderer {
    private static final float FILLED_WALL_ALPHA_SCALE=.35F;
    private static final int FILLED_WALL_MIN_ALPHA=51;
    private static final int FILLED_WALL_MAX_ALPHA=89;
    private static final int BASE_ALPHA=188;
    private static final int PULSE_ALPHA=34;
    private static final double PULSE_SPEED=12;
    private static net.minecraft.client.resources.sounds.SoundInstance ambience;
    private static boolean insideBloodMoon;
    private static List<EventNetwork.Boundary> zones=List.of();
    public static void accept(EventNetwork.Zones packet) {
        zones=List.copyOf(packet.bounds());
        BloodMoonSolidBoundary.clientZones(zones);
    }
    public static boolean bloodMoon() {
        var mc=Minecraft.getInstance();
        return mc.player!=null && mc.level!=null && zones.stream().anyMatch(z->z.active() && z.type().equals("BLOOD_MOON") && z.dimension().equals(mc.level.dimension().location().toString()) && mc.player.getX()>=z.minX() && mc.player.getX()<z.maxX()+1 && mc.player.getY()>=z.minY() && mc.player.getY()<z.maxY()+1 && mc.player.getZ()>=z.minZ() && mc.player.getZ()<z.maxZ()+1);
    }
    public static boolean canPass(BlockPos position) {
        var mc=Minecraft.getInstance();
        var level=mc.level;
        if(level==null||mc.player==null) return false;
        return zones.stream().anyMatch(zone->zone.passable()&&zone.type().equals("BOSS_FIGHT")
                &&zone.dimension().equals(level.dimension().location().toString())&&onShell(zone,position)
                &&!insideStructure(zone,mc.player.position()));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        zones=List.of();
        BloodMoonSolidBoundary.clientZones(zones);
        stopAmbience();
        insideBloodMoon=false;
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END) return;
        boolean inside=bloodMoon();
        if(inside && !insideBloodMoon) {
            ambience=new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                    net.minecraft.sounds.SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value().getLocation(),
                    net.minecraft.sounds.SoundSource.AMBIENT,.3f,.75f,net.minecraft.util.RandomSource.create(),true,0,
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,0,0,0,true);
            Minecraft.getInstance().getSoundManager().play(ambience);
        } else if(!inside && insideBloodMoon) stopAmbience();
        insideBloodMoon=inside;
    }
    private static boolean onShell(EventNetwork.Boundary zone,BlockPos position) {
        int minX=zone.minX(),minY=zone.minY(),minZ=zone.minZ();
        int maxX=zone.maxX(),maxY=zone.maxY(),maxZ=zone.maxZ();
        return position.getX()>=minX&&position.getX()<=maxX&&position.getY()>=minY&&position.getY()<=maxY
                &&position.getZ()>=minZ&&position.getZ()<=maxZ&&(position.getX()==minX||position.getX()==maxX
                ||position.getY()==minY||position.getY()==maxY||position.getZ()==minZ||position.getZ()==maxZ);
    }
    private static boolean insideStructure(EventNetwork.Boundary zone,net.minecraft.world.phys.Vec3 position) {
        return position.x>=zone.minX()+1&&position.x<zone.maxX()&&position.y>=zone.minY()+1&&position.y<zone.maxY()
                &&position.z>=zone.minZ()+1&&position.z<zone.maxZ();
    }
    private static void stopAmbience() {
        if(ambience!=null) Minecraft.getInstance().getSoundManager().stop(ambience);
        ambience=null;
    }
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor event) {
        if(bloodMoon()) { event.setRed(event.getRed()*.4f+.14f);event.setGreen(event.getGreen()*.18f);event.setBlue(event.getBlue()*.22f); }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || EventAttackRenderer.ShaderState.shadowPass()
                || mc.level==null || mc.player==null || zones.isEmpty()) return;
        var camera=event.getCamera().getPosition();
        PoseStack poses=event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x,-camera.y,-camera.z);
        PoseStack.Pose pose=poses.last();
        MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
        VertexConsumer consumer=buffers.getBuffer(TelegraphRenderType.boundary());
        for(var zone:zones) {
            if(!zone.dimension().equals(mc.level.dimension().location().toString())) continue;
            int alpha=BASE_ALPHA+(int)(PULSE_ALPHA*Math.sin((mc.level.getGameTime()+event.getPartialTick())/PULSE_SPEED));
            renderFilledBoundary(consumer,pose,zone,alpha);
        }
        buffers.endBatch(TelegraphRenderType.boundary());
        poses.popPose();
    }
    private static void renderFilledBoundary(VertexConsumer consumer,PoseStack.Pose pose,EventNetwork.Boundary zone,int alpha) {
        float x1=zone.minX(),x2=zone.maxX()+1;
        float z1=zone.minZ(),z2=zone.maxZ()+1;
        float y1=zone.minY(),y2=zone.maxY()+1;
        int fillAlpha=Math.min(FILLED_WALL_MAX_ALPHA,Math.max(FILLED_WALL_MIN_ALPHA,Math.round(alpha*FILLED_WALL_ALPHA_SCALE)));
        wall(consumer,pose,x1,z1,x2,z1,y1,y1,y2-y1,zone.color(),fillAlpha);
        wall(consumer,pose,x1,z2,x2,z2,y1,y1,y2-y1,zone.color(),fillAlpha);
        wall(consumer,pose,x1,z1,x1,z2,y1,y1,y2-y1,zone.color(),fillAlpha);
        wall(consumer,pose,x2,z1,x2,z2,y1,y1,y2-y1,zone.color(),fillAlpha);
    }

    private static void wall(VertexConsumer consumer,PoseStack.Pose pose,float x1,float z1,float x2,float z2,float y1,float y2,float height,int color,int alpha) {
        vertex(consumer,pose,x1,y1,z1,color,alpha);
        vertex(consumer,pose,x2,y2,z2,color,alpha);
        vertex(consumer,pose,x2,y2+height,z2,color,alpha);
        vertex(consumer,pose,x1,y1+height,z1,color,alpha);
    }

    private static void vertex(VertexConsumer consumer,PoseStack.Pose pose,float x,float y,float z,int color,int alpha) {
        Matrix4f position=pose.pose();
        consumer.vertex(position,x,y,z)
                .color(color>>16&255,color>>8&255,color&255,alpha)
                .endVertex();
    }
    private EventBoundaryRenderer() {}
}
