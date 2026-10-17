package com.siirio.jemvisualsandbox.workbench;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Elevated, solid pressure geometry driven by the existing sonic-wave clock. */
final class SonicWaveRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("jem_visual_sandbox", "life_chorus/sonic_band.png");
    private static final int SEGMENTS = 40, FRAMES = 24, MAX_IMPACTS = 24;
    private static final int TEAL = 0x63CFCB, IVORY = 0xE9FFF1;
    // Clockwise closed radial/height profile: trailing heel, inner shoulder,
    // compressed crown, sloped leading lip, front foot, undercut return.
    private static final float[][] PROFILE = {{-.68F, -.34F}, {-.51F, .16F}, {-.19F, .47F},
            {0, .13F}, {-.06F, -.22F}, {-.35F, -.40F}};
    private record Hit(ClientLevel level, long start, Vec3 center, int seed) {}
    private static final List<Hit> HITS = new ArrayList<>();

    private SonicWaveRenderer() {}

    static void clear() { HITS.clear(); }

    /** Called only for the server's successful-damage visual packet. */
    static void impact(SandboxNetwork.Visual packet) {
        var client = Minecraft.getInstance();
        if (packet.action() != 36 || client.level == null || !client.hasSingleplayerServer()) return;
        var target = client.level.getEntity(packet.subject());
        double height = target == null ? .9 : Math.min(1.8, target.getBbHeight() * .55);
        if (HITS.size() >= MAX_IMPACTS) HITS.remove(0);
        HITS.add(new Hit(client.level, client.level.getGameTime(), packet.origin().add(0, height, 0), packet.subject()));
    }

    static void render(RenderLevelStageEvent event, Vec3 origin, float age) {
        var client=Minecraft.getInstance();
        if(client.level==null || !client.hasSingleplayerServer())return;
        render(VisualRenderContext.world(event),origin,age);
    }
    static void render(VisualRenderContext event, Vec3 origin, float age) {
        var client = Minecraft.getInstance();
        if (age < 0 || age >= 27) return;
        var poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poses.pushPose();
        try {
            poses.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
            var pose = poses.last();
            var buffers = client.renderBuffers().bufferSource();
            var out = buffers.getBuffer(Material.TYPE);
            if (age < 7) {
                float charge = age / 7;
                // An inward squeeze at torso height reads as held pressure,
                // rather than an expanding floor marker before damage starts.
                front(out, pose, .92F - .38F * charge, .88F, .36F + .25F * charge,
                        .15F + .5F * charge, Math.min(5, (int) age), age * .035F);
            } else {
                float radius = Math.min(8, 8 * (age - 6) / 12);
                float dissolve = Mth.clamp((27 - age) / 8, 0, 1);
                int frame = Math.min(FRAMES - 1, (int) (age - 7));
                // The whole cross-section advances as one connected pressure
                // body. Its leading shoulder never exceeds the eight-block edge.
                front(out, pose, radius, .84F, 1, .87F * dissolve, frame, 0);
                if (age >= 9) front(out, pose, Math.max(.22F, radius - .62F), 1.08F, .54F,
                        .40F * dissolve, Math.min(23, frame + 3), .06F);
                if (age >= 11) front(out, pose, Math.max(.18F, radius - 1.18F), .58F, .35F,
                        .23F * dissolve, Math.min(23, frame + 6), -.04F);
                fragments(out, pose, radius, age - 7, dissolve, frame, 12);
            }
            buffers.endBatch(Material.TYPE);
        } finally {
            poses.popPose();
        }
    }

    static void renderImpacts(RenderLevelStageEvent event) {
        var client = Minecraft.getInstance();
        if (client.level == null || !client.hasSingleplayerServer()) { clear(); return; }
        HITS.removeIf(hit -> hit.level != client.level || client.level.getGameTime() - hit.start >= 6);
        if (HITS.isEmpty()) return;
        for (Hit hit : HITS) {
            float age = client.level.getGameTime() - hit.start + event.getPartialTick();
            if (age < 0 || age >= 6) continue;
            drawImpact(VisualRenderContext.world(event),hit.center,age,hit.seed);
        }
    }

    static void drawImpact(VisualRenderContext context,Vec3 center,float age,int seed) {
        if(age<0 || age>=6)return;
        var poses=context.getPoseStack();Vec3 camera=context.getCamera().getPosition();
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var out=buffers.getBuffer(Material.TYPE);
            poses.pushPose();
            try {
                poses.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
                float fade = 1 - age / 6;
                front(out, poses.last(), .24F + age * .07F, 0, .32F, .9F * fade,
                        Math.min(23, 7 + (int) age), seed * .31F);
                fragments(out, poses.last(), .25F + age * .045F, age, fade, 9 + (int) age, 3);
            } finally {
                poses.popPose();
            }
        buffers.endBatch(Material.TYPE);
    }

    private static void front(VertexConsumer out, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                              float radius, float centerY, float thickness, float alpha, int frame, float phase) {
        if (radius <= 0 || alpha <= 0) return;
        float[][][] ring = new float[SEGMENTS][PROFILE.length][];
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float angle = segment * Mth.TWO_PI / SEGMENTS;
            for (int side = 0; side < PROFILE.length; side++) {
                ring[segment][side] = point(radius, centerY, thickness, angle, side, phase);
            }
        }
        for (int segment = 0; segment < SEGMENTS; segment++) {
            int nextSegment = (segment + 1) % SEGMENTS;
            for (int side = 0; side < PROFILE.length; side++) {
                int next = (side + 1) % PROFILE.length;
                float[] v0 = ring[segment][side];
                float[] v1 = ring[nextSegment][side];
                float[] v2 = ring[nextSegment][next];
                float[] v3 = ring[segment][next];
                int color = side == 1 || side == 2 ? IVORY : TEAL;
                // One continuous UV wrap carries the authored native painting
                // across connected geometry; these are never billboard tiles.
                quad(out, pose, v0, v1, v2, v3, segment / (float) SEGMENTS,
                        (segment + 1) / (float) SEGMENTS, side / (float) PROFILE.length,
                        (side + 1) / (float) PROFILE.length, frame, color, alpha);
            }
        }
    }

    private static float[] point(float radius, float y, float thickness, float angle, int profile, float phase) {
        float r = Math.max(.035F, radius + PROFILE[profile][0] * thickness);
        // Restrained height changes make a forceful low-poly crown above ground.
        float lift = Mth.sin(angle * 3 + phase) * .055F * thickness;
        return new float[]{Mth.cos(angle) * r, y + PROFILE[profile][1] * thickness + lift, Mth.sin(angle) * r};
    }

    private static void fragments(VertexConsumer out, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                  float radius, float age, float alpha, int frame, int count) {
        for (int i = 0; i < count; i++) {
            float angle = i * 2.399963F;
            float offset = .18F + (i % 3) * .15F;
            float r = Math.max(.1F, radius - offset - age * .012F);
            float y = .57F + (i % 4) * .13F + Math.min(age, 8) * .035F - age * age * .0018F;
            float length = .10F + (i % 3) * .045F;
            cuboid(out, pose, Mth.cos(angle) * r, y, Mth.sin(angle) * r,
                    length, .055F + (i % 2) * .035F, .07F, angle + age * .06F,
                    Math.min(23, frame), i % 3 == 0 ? IVORY : TEAL, alpha * .68F);
        }
    }

    private static void cuboid(VertexConsumer out, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                               float x, float y, float z, float length, float height, float depth, float yaw,
                               int frame, int color, float alpha) {
        float[][] points = new float[8][];
        for (int i = 0; i < 8; i++) {
            float dx = (i & 1) == 0 ? -length : length;
            float dy = (i & 2) == 0 ? -height : height;
            float dz = (i & 4) == 0 ? -depth : depth;
            points[i] = new float[]{x + dx * Mth.cos(yaw) - dz * Mth.sin(yaw), y + dy,
                    z + dx * Mth.sin(yaw) + dz * Mth.cos(yaw)};
        }
        for (int[] face : CUBE_FACES) quad(out, pose, points[face[0]], points[face[1]], points[face[2]], points[face[3]],
                .25F, .75F, .22F, .72F, frame, color, alpha);
    }

    private static final int[][] CUBE_FACES = {{0, 2, 3, 1}, {4, 5, 7, 6}, {0, 4, 6, 2},
            {1, 3, 7, 5}, {2, 6, 7, 3}, {0, 1, 5, 4}};

    private static void quad(VertexConsumer out, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             float[] a, float[] b, float[] c, float[] d,
                             float u0, float u1, float v0, float v1, int frame, int color, float alpha) {
        Vec3 normal = new Vec3(b[0] - a[0], b[1] - a[1], b[2] - a[2])
                .cross(new Vec3(c[0] - a[0], c[1] - a[1], c[2] - a[2])).normalize();
        vertex(out, pose, a, u0, v0, frame, color, alpha, normal);
        vertex(out, pose, b, u1, v0, frame, color, alpha, normal);
        vertex(out, pose, c, u1, v1, frame, color, alpha, normal);
        vertex(out, pose, d, u0, v1, frame, color, alpha, normal);
    }

    private static void vertex(VertexConsumer out, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                               float[] p, float u, float v, int frame, int color, float alpha, Vec3 normal) {
        out.vertex(pose.pose(), p[0], p[1], p[2])
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, (int) (255 * Mth.clamp(alpha, 0, 1)))
                .uv(u, (Math.min(23, frame) + Mth.clamp(v, .002F, .998F)) / FRAMES)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    private static final class Material extends RenderType {
        private static final RenderType TYPE = create("jem_sonic_pressure_body", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, MEDIUM_BUFFER_SIZE, false, true, CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(TEXTURE, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private Material(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                         boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }
}
