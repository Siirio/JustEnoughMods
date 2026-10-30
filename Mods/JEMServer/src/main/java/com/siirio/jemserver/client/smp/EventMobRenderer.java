package com.siirio.jemserver.client.smp;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid="jem_server",value=Dist.CLIENT)
public final class EventMobRenderer {
    private static final Set<Integer> BLOOD_MOON_MOBS=new HashSet<>();
    private static final ArrayList<Beam> BEAMS=new ArrayList<>();
    private static final float[] BEAM_COLOR={1,1,1};
    private static final Component MARKER=Component.literal("!");
    private static final float MARKER_HEIGHT=2.5f;
    private static final float BASE_SCALE=.06f;
    private static final double SCALE_DISTANCE=24;
    private static final float MAX_DISTANCE_SCALE=3;
    private static final int MARKER_COLOR=0xFFFF2020;
    private static final int MARKER_BACKGROUND=0x99000000;
    private static final Set<RenderType> SUBMITTED_TYPES=new LinkedHashSet<>();
    private static final MultiBufferSource OWN_BUFFERS=type->{
        SUBMITTED_TYPES.add(type);
        return Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(type);
    };
    private record Beam(ResourceLocation dimension,BlockPos position,long expires,float radius) {}

    public static void accept(int entityId,boolean bloodMoon) {
        var level=Minecraft.getInstance().level;if(level==null) return;
        var entity=level.getEntity(entityId);if(entity==null) return;
        if(bloodMoon) BLOOD_MOON_MOBS.add(entityId);
    }

    public static void beam(ResourceLocation dimension,BlockPos position,int duration,float radius) {
        var level=Minecraft.getInstance().level;
        if(level!=null) BEAMS.add(new Beam(dimension,position,level.getGameTime()+duration,radius));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var client=Minecraft.getInstance();
        if(client.level==null || client.player==null || BEAMS.isEmpty() && BLOOD_MOON_MOBS.isEmpty()) return;
        if(EventShaderState.shadowPass()) return;
        var camera=event.getCamera().getPosition();
        PoseStack poses=event.getPoseStack();
        SUBMITTED_TYPES.clear();
        try {
            var beams=BEAMS.iterator();
            while(beams.hasNext()) {
                var beam=beams.next();
                if(beam.expires()<=client.level.getGameTime()) { beams.remove();continue; }
                if(!beam.dimension().equals(client.level.dimension().location())) continue;
                poses.pushPose();
                try {
                    poses.translate(beam.position().getX()-camera.x,beam.position().getY()-camera.y,beam.position().getZ()-camera.z);
                    BeaconRenderer.renderBeaconBeam(poses,OWN_BUFFERS,BeaconRenderer.BEAM_LOCATION,event.getPartialTick(),1,client.level.getGameTime(),0,
                            Math.max(1,client.level.getMaxBuildHeight()-beam.position().getY()),BEAM_COLOR,beam.radius()*.8F,beam.radius());
                } finally {
                    poses.popPose();
                }
            }
            var tracked=BLOOD_MOON_MOBS.iterator();
            while(tracked.hasNext()) {
                int id=tracked.next();
                var entity=client.level.getEntity(id);
                if(entity==null || !entity.isAlive()) { tracked.remove();continue; }
                double x=entity.getX()-camera.x,y=entity.getY()+entity.getBbHeight()+MARKER_HEIGHT-camera.y,z=entity.getZ()-camera.z;
                float distanceScale=(float)Math.min(MAX_DISTANCE_SCALE,Math.max(1,entity.distanceTo(client.player)/SCALE_DISTANCE));
                poses.pushPose();
                try {
                    poses.translate(x,y,z);
                    poses.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
                    poses.scale(-BASE_SCALE*distanceScale,-BASE_SCALE*distanceScale,BASE_SCALE*distanceScale);
                    float textX=-client.font.width(MARKER)/2f;
                    client.font.drawInBatch(MARKER,textX,0,MARKER_COLOR,false,poses.last().pose(),OWN_BUFFERS,Font.DisplayMode.SEE_THROUGH,MARKER_BACKGROUND,LightTexture.FULL_BRIGHT);
                } finally {
                    poses.popPose();
                }
            }
        } finally {
            try {
                if(!SUBMITTED_TYPES.isEmpty()) {
                    var buffers=client.renderBuffers().bufferSource();
                    for(RenderType type:SUBMITTED_TYPES) buffers.endBatch(type);
                }
            } finally {
                SUBMITTED_TYPES.clear();
            }
        }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { BLOOD_MOON_MOBS.clear();BEAMS.clear();SUBMITTED_TYPES.clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if(event.getLevel().isClientSide()) { BLOOD_MOON_MOBS.clear();BEAMS.clear();SUBMITTED_TYPES.clear(); } }
    private EventMobRenderer() {}
}
