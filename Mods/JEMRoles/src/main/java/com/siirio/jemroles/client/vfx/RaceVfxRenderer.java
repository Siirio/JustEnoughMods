package com.siirio.jemroles.client.vfx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.network.RaceVfxPacket;
import com.siirio.jemroles.vfx.RaceVfxClientConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = JemRoles.MOD_ID, value = Dist.CLIENT)
public final class RaceVfxRenderer {
    private static final double GROUND_OFFSET = 0.025D;

    private RaceVfxRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || RaceVfxShaderState.shadowPass()) return;
        Minecraft client = Minecraft.getInstance();
        List<RaceVfxInstance> instances = RaceVfxController.instances();
        if (client.level == null || instances.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poses.last();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(VfxRenderType.geometry());
        double now = client.level.getGameTime() + event.getPartialTick();
        for (RaceVfxInstance instance : instances) renderInstance(out, pose, instance, now);
        buffers.endBatch(VfxRenderType.geometry());
        poses.popPose();
    }

    private static void renderInstance(VertexConsumer out, PoseStack.Pose pose, RaceVfxInstance instance, double now) {
        RaceVfxPacket packet = instance.packet();
        double progress = instance.progress(now);
        int fade = (int)(255.0D * Math.min(1.0D, (1.0D - progress) * 3.5D));
        Vec3 origin = packet.origin();
        Vec3 target = packet.target();
        switch (packet.id()) {
            case LIFE_CHORUS -> {
                double radius = progress < 0.3D ? lerp(3.6D, 0.75D, progress / 0.3D) : 0.75D + Math.sin(progress * Math.PI) * 0.35D;
                ring(out, pose, origin, radius + 0.10D, 0.20D, RaceVfxPalettes.ANCESTOR_DARK, fade * 2 / 3, 40);
                ring(out, pose, origin, radius, 0.08D, RaceVfxPalettes.ANCESTOR_LIGHT, fade, 40);
                if (progress >= 0.18D) for (int entityId : packet.relatedEntityIds()) {
                    Vec3 recipient = entityPosition(entityId, origin).add(0.0D, 0.8D, 0.0D);
                    arc(out, pose, origin.add(0.0D, 1.0D, 0.0D), recipient, 0.055D,
                            RaceVfxPalettes.ANCESTOR, fade * 3 / 4);
                }
                if (packet.tier() >= 3 && progress > 0.55D)
                    ring(out, pose, origin.add(0.0D, 2.05D, 0.0D), 0.75D, 0.06D,
                            RaceVfxPalettes.ANCESTOR_LIGHT, fade, 32);
            }
            case GUARDIAN_FORM, GUARDIAN_DISSOLVE -> {
                boolean dissolve = packet.id() == com.siirio.jemroles.vfx.RaceVfxId.GUARDIAN_DISSOLVE;
                double visible = dissolve ? 1.0D - progress : progress;
                int layers = Math.max(1, (int)Math.ceil(visible * 7.0D));
                for (int i = 0; i < layers; i++) {
                    double layer = dissolve ? 6 - i : i;
                    pillar(out, pose, origin.add(0.0D, layer * 0.38D, 0.0D), 0.32D,
                            0.52D - layer * 0.025D, RaceVfxPalettes.ANCESTOR, fade * (i + 2) / (layers + 2));
                }
                if (!dissolve && progress > 0.72D)
                    ring(out, pose, origin.add(0.0D, 2.15D, 0.0D), 0.42D, 0.07D,
                            RaceVfxPalettes.ANCESTOR_LIGHT, fade, 24);
            }
            case MERCHANT_BLESSING -> ring(out, pose, origin, 0.55D + progress * 0.55D, 0.05D,
                    RaceVfxPalettes.ANCESTOR_LIGHT, fade, 24);
            case RIFT_DEPART -> {
                double radius = lerp(2.2D, 0.15D, progress);
                ring(out, pose, origin, radius, 0.12D, RaceVfxPalettes.VOID_DARK, fade, 32);
                ring(out, pose, origin, radius * 0.78D, 0.045D, RaceVfxPalettes.VOID_LIGHT, fade, 32);
                seam(out, pose, origin, target, 0.035D, RaceVfxPalettes.VOID, fade / 2);
            }
            case RIFT_ARRIVE -> {
                double radius = lerp(0.15D, 2.35D, Math.min(1.0D, progress * 1.8D));
                ring(out, pose, origin, radius, 0.11D, RaceVfxPalettes.VOID, fade, 36);
                arc(out, pose, origin.add(0.0D, 0.15D, 0.0D), origin.add(origin.subtract(target).normalize().scale(2.2D)).add(0.0D, 0.15D, 0.0D),
                        0.08D, RaceVfxPalettes.VOID_LIGHT, fade);
            }
            case VOID_PROJECTILE_CHARGE -> {
                double pulse = 0.28D + (1.0D - progress) * 0.65D + Math.sin(progress * Math.PI * 8.0D) * 0.08D;
                ring(out, pose, origin, pulse, 0.055D, RaceVfxPalettes.VOID_LIGHT, fade, 28);
                seam(out, pose, target, origin, 0.025D, RaceVfxPalettes.VOID, fade / 2);
            }
            case VOID_PROJECTILE_RELEASE -> {
                Vec3 head = origin.add(target.subtract(origin).scale(progress));
                seam(out, pose, origin, head, 0.05D, RaceVfxPalettes.VOID_DARK, fade * 2 / 3);
                seam(out, pose, origin, head, 0.018D, RaceVfxPalettes.VOID_LIGHT, fade);
                ring(out, pose, head, 0.24D, 0.045D, RaceVfxPalettes.VOID_LIGHT, fade, 24);
            }
            case VOID_PROJECTILE_IMPACT -> {
                double radius = lerp(2.1D, 0.18D, progress);
                ring(out, pose, origin, radius, 0.10D, RaceVfxPalettes.VOID, fade, 36);
                ring(out, pose, origin, radius * 0.55D, 0.04D, RaceVfxPalettes.VOID_LIGHT, fade, 28);
            }
            case VOID_RESCUE -> {
                crack(out, pose, origin, target.subtract(origin), 2.2D, 7,
                        RaceVfxPalettes.VOID_LIGHT, fade, packet.seed());
                ring(out, pose, target, lerp(1.4D, 0.35D, progress), 0.07D,
                        RaceVfxPalettes.VOID, fade, 32);
            }
            case GRAVITY_DENIAL -> {
                double y = progress * 2.8D;
                ring(out, pose, origin.add(0.0D, y, 0.0D), 0.75D + Math.sin(progress * Math.PI * 4.0D) * 0.12D,
                        0.05D, RaceVfxPalettes.VOID_LIGHT, fade * 2 / 3, 28);
                pillar(out, pose, origin, 2.8D, 0.32D, RaceVfxPalettes.VOID, fade / 3);
            }
            case VERDANT_DOMINION -> {
                ring(out, pose, origin, packet.tier() >= 3 ? 7.5D : 2.2D + packet.tier(), 0.13D,
                        RaceVfxPalettes.BLOOM_DARK, fade * 2 / 3, 64);
                for (int entityId : packet.relatedEntityIds()) {
                    Vec3 recipient = entityPosition(entityId, origin);
                    seam(out, pose, origin, recipient, 0.095D, RaceVfxPalettes.BLOOM_DARK, fade);
                    seam(out, pose, origin, recipient, 0.035D, RaceVfxPalettes.BLOOM_LIGHT, fade);
                    ring(out, pose, recipient, 0.65D + progress * 0.2D, 0.065D, RaceVfxPalettes.BLOOM, fade, 24);
                }
            }
            case LIVING_PATH_STEP -> {
                Vec3 previous = origin;
                for (Vec3 point : sampledPath(packet.path())) {
                    seam(out, pose, previous, point, 0.045D, RaceVfxPalettes.BLOOM_LIGHT, fade);
                    ring(out, pose, point, 0.46D, 0.035D, RaceVfxPalettes.BLOOM, fade * 2 / 3, 24);
                    previous = point;
                }
            }
            case SECOND_BLOSSOM -> {
                double opening = Math.sin(progress * Math.PI);
                ring(out, pose, origin, 0.35D + opening * 1.65D, 0.13D,
                        RaceVfxPalettes.BLOOM_LIGHT, fade, 40);
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI * 2.0D * i / 6.0D;
                    Vec3 petal = origin.add(Math.cos(angle) * opening * 1.4D, 0.1D + opening * 0.8D,
                            Math.sin(angle) * opening * 1.4D);
                    arc(out, pose, origin.add(0.0D, 0.35D, 0.0D), petal, 0.065D,
                            RaceVfxPalettes.BLOOM, fade);
                }
            }
            case PHOTOSYNTHESIS -> ring(out, pose, origin.add(0.0D, progress * 1.4D, 0.0D),
                    0.32D + progress * 0.35D, 0.035D, RaceVfxPalettes.BLOOM_LIGHT, fade / 2, 24);
            case SCULK_VEINS -> {
                Vec3 previous = origin;
                for (Vec3 point : sampledPath(packet.path())) {
                    seam(out, pose, previous, point, 0.08D, RaceVfxPalettes.SCULK_DARK, fade);
                    seam(out, pose, previous, point, 0.026D, RaceVfxPalettes.SCULK_LIGHT, fade);
                    previous = point;
                }
                ring(out, pose, target, 0.55D + Math.sin(progress * Math.PI * 4.0D) * 0.18D,
                        0.07D, RaceVfxPalettes.SCULK_LIGHT, fade, 28);
                pillar(out, pose, target, 2.2D, 0.34D, RaceVfxPalettes.SCULK, fade / 2);
            }
            case ECHO_VISION -> {
                ring(out, pose, origin, lerp(0.4D, 12.0D, progress), 0.08D,
                        RaceVfxPalettes.SCULK_LIGHT, fade, 64);
                for (int entityId : packet.relatedEntityIds())
                    ring(out, pose, entityPosition(entityId, origin), 0.55D + progress * 0.2D, 0.04D,
                            RaceVfxPalettes.SCULK, fade / 2, 24);
            }
            case ECHO_ABSORB -> {
                double radius = progress < 0.65D ? lerp(2.8D, 0.35D, progress / 0.65D)
                        : lerp(0.35D, 1.65D, (progress - 0.65D) / 0.35D);
                ring(out, pose, origin.add(0.0D, 1.0D, 0.0D), radius, 0.08D,
                        progress < 0.65D ? RaceVfxPalettes.SCULK : RaceVfxPalettes.SCULK_LIGHT, fade, 40);
            }
            case RESONANCE_THRESHOLD -> {
                ring(out, pose, origin.add(0.0D, 1.05D, 0.0D), lerp(0.2D, 1.1D, progress),
                        0.06D, RaceVfxPalettes.SCULK_LIGHT, fade, 28);
                pillar(out, pose, origin, 1.8D, 0.22D, RaceVfxPalettes.SCULK, fade / 3);
            }
            case INFERNAL_TELEGRAPH -> {
                Vec3 direction = target.subtract(origin);
                crack(out, pose, origin, direction, 2.8D, 8, RaceVfxPalettes.NETHER_DARK,
                        Math.min(220, fade), packet.seed());
                crack(out, pose, origin, direction, 2.55D, 7,
                        RaceVfxPalettes.mix(RaceVfxPalettes.NETHER, RaceVfxPalettes.NETHER_LIGHT, progress),
                        Math.min(255, 90 + (int)(progress * 165.0D)), packet.seed());
                ring(out, pose, origin, lerp(1.2D, 0.32D, progress), 0.07D,
                        RaceVfxPalettes.NETHER_LIGHT, fade, 28);
            }
            case INFERNAL_IMPACT -> {
                double rise = Math.sin(Math.min(1.0D, progress * 1.7D) * Math.PI * 0.5D);
                pillar(out, pose, origin, 0.8D + rise * 4.2D, 0.72D,
                        RaceVfxPalettes.NETHER_DARK, fade * 2 / 3);
                pillar(out, pose, origin, 0.6D + rise * 3.6D, 0.34D,
                        RaceVfxPalettes.NETHER_LIGHT, fade);
                ring(out, pose, origin, lerp(0.4D, 3.2D, Math.min(1.0D, progress * 2.2D)),
                        0.16D, RaceVfxPalettes.NETHER, fade, 48);
            }
            case VOLCANIC_ZONE -> {
                double pulse = 5.8D + Math.sin(progress * Math.PI * 10.0D) * 0.12D;
                ring(out, pose, origin, pulse, 0.11D, RaceVfxPalettes.NETHER_DARK, fade / 2, 64);
                ring(out, pose, origin, pulse - 0.16D, 0.035D, RaceVfxPalettes.NETHER, fade / 2, 64);
            }
            case LAVA_DRINK_START -> {
                ring(out, pose, origin.add(0.0D, Math.sin(progress * Math.PI * 2.0D) * 0.18D, 0.0D),
                        lerp(1.0D, 0.2D, progress), 0.045D, RaceVfxPalettes.NETHER_LIGHT, fade, 28);
                seam(out, pose, origin, target, 0.025D, RaceVfxPalettes.NETHER, fade / 2);
            }
            case LAVA_DRINK_COMPLETE -> {
                ring(out, pose, origin, lerp(0.15D, 1.25D, progress), 0.08D,
                        RaceVfxPalettes.NETHER_LIGHT, fade, 32);
                pillar(out, pose, origin.add(0.0D, -1.0D, 0.0D), 2.0D, 0.22D,
                        RaceVfxPalettes.NETHER, fade / 2);
            }
            case LAVA_SWIM -> {
                seam(out, pose, origin, target, 0.08D, RaceVfxPalettes.NETHER_DARK, fade);
                seam(out, pose, origin, target, 0.025D, RaceVfxPalettes.NETHER_LIGHT, fade);
            }
            case WATER_STEAM -> ring(out, pose, origin.add(0.0D, progress * 1.4D, 0.0D),
                    lerp(0.35D, 1.25D, progress), 0.09D, 0xD9E4E8, fade / 2, 32);
        }
    }

    private static List<Vec3> sampledPath(List<Vec3> path) {
        int limit = Math.max(2, RaceVfxClientConfig.trailSamples());
        if (path.size() <= limit) return path;
        java.util.ArrayList<Vec3> sampled = new java.util.ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            int index = (int)Math.round(i * (path.size() - 1.0D) / (limit - 1.0D));
            sampled.add(path.get(index));
        }
        return sampled;
    }

    private static Vec3 entityPosition(int entityId, Vec3 fallback) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || entityId < 0) return fallback;
        Entity entity = client.level.getEntity(entityId);
        return entity == null ? fallback : entity.position();
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * Math.max(0.0D, Math.min(1.0D, progress));
    }

    public static void ring(VertexConsumer out, PoseStack.Pose pose, Vec3 center, double radius, double width,
                            int color, int alpha, int segments) {
        int count = Math.max(24, Math.min(96, segments));
        double inner = Math.max(0.0D, radius - width * 0.5D);
        double outer = radius + width * 0.5D;
        double y = center.y + GROUND_OFFSET;
        for (int i = 0; i < count; i++) {
            double first = Math.PI * 2.0D * i / count;
            double second = Math.PI * 2.0D * (i + 1) / count;
            vertex(out, pose, center.x + Math.cos(first) * inner, y, center.z + Math.sin(first) * inner, color, alpha);
            vertex(out, pose, center.x + Math.cos(second) * inner, y, center.z + Math.sin(second) * inner, color, alpha);
            vertex(out, pose, center.x + Math.cos(second) * outer, y, center.z + Math.sin(second) * outer, color, alpha);
            vertex(out, pose, center.x + Math.cos(first) * outer, y, center.z + Math.sin(first) * outer, color, alpha);
        }
    }

    public static void arc(VertexConsumer out, PoseStack.Pose pose, Vec3 from, Vec3 to, double width,
                           int color, int alpha) {
        Vec3 delta = to.subtract(from);
        Vec3 previous = from;
        for (int i = 1; i <= 16; i++) {
            double progress = i / 16.0D;
            Vec3 next = from.add(delta.scale(progress)).add(0.0D, Math.sin(progress * Math.PI) * 0.65D, 0.0D);
            ribbon(out, pose, previous, next, width, color, alpha);
            previous = next;
        }
    }

    public static void seam(VertexConsumer out, PoseStack.Pose pose, Vec3 from, Vec3 to, double width,
                            int color, int alpha) {
        ribbon(out, pose, from.add(0.0D, GROUND_OFFSET, 0.0D), to.add(0.0D, GROUND_OFFSET, 0.0D), width, color, alpha);
    }

    public static void crack(VertexConsumer out, PoseStack.Pose pose, Vec3 center, Vec3 direction, double length,
                             int branches, int color, int alpha, long seed) {
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 0.0001D) flat = new Vec3(1.0D, 0.0D, 0.0D);
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        RandomSource random = RandomSource.create(seed);
        int count = Math.max(6, Math.min(14, branches));
        Vec3 previous = center;
        for (int i = 1; i <= count; i++) {
            double progress = i / (double) count;
            Vec3 next = center.add(flat.scale(length * progress))
                    .add(side.scale((random.nextDouble() - 0.5D) * 0.35D));
            seam(out, pose, previous, next, Math.max(0.025D, length * 0.018D), color, alpha);
            if (i > 1 && i < count && (i & 1) == 0) {
                double branchSide = random.nextBoolean() ? 1.0D : -1.0D;
                Vec3 branch = next.add(side.scale(branchSide * length * (0.12D + random.nextDouble() * 0.12D)))
                        .add(flat.scale(length * 0.08D));
                seam(out, pose, next, branch, Math.max(0.018D, length * 0.012D), color, alpha * 3 / 4);
            }
            previous = next;
        }
    }

    public static void pillar(VertexConsumer out, PoseStack.Pose pose, Vec3 base, double height, double radius,
                              int color, int alpha) {
        verticalQuad(out, pose, base.add(-radius, 0.0D, 0.0D), base.add(radius, 0.0D, 0.0D), height, color, alpha);
        verticalQuad(out, pose, base.add(0.0D, 0.0D, -radius), base.add(0.0D, 0.0D, radius), height, color, alpha);
    }

    private static void ribbon(VertexConsumer out, PoseStack.Pose pose, Vec3 from, Vec3 to, double width,
                               int color, int alpha) {
        Vec3 delta = to.subtract(from);
        Vec3 perpendicular = new Vec3(-delta.z, 0.0D, delta.x);
        if (perpendicular.lengthSqr() < 0.0001D) perpendicular = new Vec3(width, 0.0D, 0.0D);
        else perpendicular = perpendicular.normalize().scale(width);
        vertex(out, pose, from.x + perpendicular.x, from.y, from.z + perpendicular.z, color, alpha);
        vertex(out, pose, to.x + perpendicular.x, to.y, to.z + perpendicular.z, color, alpha);
        vertex(out, pose, to.x - perpendicular.x, to.y, to.z - perpendicular.z, color, alpha);
        vertex(out, pose, from.x - perpendicular.x, from.y, from.z - perpendicular.z, color, alpha);
    }

    private static void verticalQuad(VertexConsumer out, PoseStack.Pose pose, Vec3 first, Vec3 second, double height,
                                     int color, int alpha) {
        vertex(out, pose, first.x, first.y, first.z, color, alpha);
        vertex(out, pose, second.x, second.y, second.z, color, alpha);
        vertex(out, pose, second.x, second.y + height, second.z, color, 0);
        vertex(out, pose, first.x, first.y + height, first.z, color, 0);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, double x, double y, double z,
                               int color, int alpha) {
        Matrix4f matrix = pose.pose();
        out.vertex(matrix, (float) x, (float) y, (float) z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, Mth.clamp(alpha, 0, 255))
                .endVertex();
    }

    private static final class VfxRenderType extends RenderType {
        private static final RenderType GEOMETRY = create(
                "jem_race_vfx_geometry",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                MEDIUM_BUFFER_SIZE,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        private VfxRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        static RenderType geometry() { return GEOMETRY; }
    }
}
