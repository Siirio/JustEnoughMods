package com.siirio.jemserver.client.smp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.siirio.jemserver.smp.events.AttackGeometry;
import com.siirio.jemserver.smp.events.EventNetwork;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.CLIENT)
public final class EventAttackRenderer {
    private static final Map<UUID, Telegraph> TELEGRAPHS = new HashMap<>();
    private static final Map<UUID, AttackVfx> ATTACKS = new HashMap<>();
    private static final float GROUND_OFFSET = .025F;
    private static final float OUTLINE_OFFSET = .004F;
    private static final float TILE_INSET = .065F;
    private static final float WARNING_SWEEP_WIDTH = .075F;
    private static final float OUTER_OUTLINE_WIDTH = .11F;
    private static final float INNER_OUTLINE_WIDTH = .035F;
    private static final float ARROW_WIDTH = .055F;
    private static final float ATTACK_CORE_WIDTH = .045F;
    private static final float ATTACK_TRAIL_WIDTH = .22F;
    private static final int OUTER_OUTLINE_ALPHA = 72;
    private static final int ARROW_SPACING = 19;
    private static final int FINAL_WARNING_START_PERCENT = 72;
    private static final int MAX_WARNING_FILL_ALPHA = 76;

    private record Cell(int x, int z, float y) {
    }

    private record Telegraph(EventNetwork.Telegraph packet, long expires, List<Cell> cells) {
    }

    private record AttackVfx(EventNetwork.AttackVfx packet, long started, long expires, List<Cell> cells) {
    }

    public static void accept(EventNetwork.Telegraph packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        TELEGRAPHS.put(packet.eventId(), new Telegraph(packet,
                client.level.getGameTime() + packet.duration(), sample(packet.geometry())));
    }

    public static void accept(EventNetwork.AttackVfx packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        long started = client.level.getGameTime();
        TELEGRAPHS.remove(packet.eventId());
        ATTACKS.put(packet.eventId(), new AttackVfx(packet, started, started + packet.duration(), sample(packet.geometry())));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ShaderState.shadowPass()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        long now = client.level.getGameTime();
        TELEGRAPHS.entrySet().removeIf(entry -> entry.getValue().expires() <= now);
        ATTACKS.entrySet().removeIf(entry -> entry.getValue().expires() <= now);
        if (TELEGRAPHS.isEmpty() && ATTACKS.isEmpty()) {
            return;
        }
        var camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poses.last();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TelegraphRenderType.ground());
        for (Telegraph telegraph : TELEGRAPHS.values()) {
            EventNetwork.Telegraph packet = telegraph.packet();
            if (!packet.dimension().equals(client.level.dimension().location())) {
                continue;
            }
            double progress = progress(client.level.getGameTime() + event.getPartialTick(),
                    packet.geometry().startTick(), packet.geometry().windupDuration());
            AttackGeometry geometry = packet.geometry().atProgress(progress);
            int fillAlpha = warningAlpha(progress);
            int outlineAlpha = Math.min(255, 145 + (int) (progress * 95));
            renderTelegraph(consumer, pose, telegraph.cells(), geometry, packet.color(), fillAlpha, outlineAlpha);
        }
        for (AttackVfx attack : ATTACKS.values()) {
            EventNetwork.AttackVfx packet = attack.packet();
            if (!packet.dimension().equals(client.level.dimension().location())) {
                continue;
            }
            double progress = progress(client.level.getGameTime() + event.getPartialTick(), attack.started(), packet.duration());
            renderAttack(consumer, pose, attack.cells(), packet.geometry(), packet.color(), progress);
        }
        buffers.endBatch(TelegraphRenderType.ground());
        poses.popPose();
    }

    private static void renderTelegraph(VertexConsumer consumer, PoseStack.Pose pose, List<Cell> cells, AttackGeometry geometry,
                                        int color, int fillAlpha, int outlineAlpha) {
        int shadowColor = mix(color, 0x000000, .32);
        int accentColor = mix(color, 0xFFFFFF, .62);
        double progress = geometry.progress();
        for (Cell cell : cells) {
            double phase = geometry.activationProgress(cell.x() + .5, cell.z() + .5);
            double sweepDistance = Math.abs(phase - progress);
            int alpha = Math.min(MAX_WARNING_FILL_ALPHA, fillAlpha + (geometry.front(cell.x() + .5, cell.z() + .5) ? 12 : 0));
            tile(consumer, pose, cell.x(), cell.y(), cell.z(), TILE_INSET, color, alpha);
            if (sweepDistance <= WARNING_SWEEP_WIDTH) {
                double strength = 1 - sweepDistance / WARNING_SWEEP_WIDTH;
                int sweepAlpha = 74 + (int) (strength * 92);
                tile(consumer, pose, cell.x(), cell.y() + OUTLINE_OFFSET, cell.z(), TILE_INSET * .55F, accentColor, sweepAlpha);
            }
            if (outsideEdge(cell, geometry)) {
                contour(consumer, pose, cell, geometry, shadowColor, OUTER_OUTLINE_ALPHA, OUTER_OUTLINE_WIDTH, OUTLINE_OFFSET);
                contour(consumer, pose, cell, geometry, accentColor, outlineAlpha, INNER_OUTLINE_WIDTH, OUTLINE_OFFSET * 2);
            }
            if (Math.floorMod(cell.x() * 31 + cell.z() * 17, ARROW_SPACING) == 0) {
                int arrowAlpha = progress * 100 >= FINAL_WARNING_START_PERCENT ? outlineAlpha : Math.max(104, outlineAlpha - 56);
                arrow(consumer, pose, cell, geometry.directionX(), geometry.directionZ(), accentColor, arrowAlpha);
            }
        }
    }

    private static void renderAttack(VertexConsumer consumer, PoseStack.Pose pose, List<Cell> cells, AttackGeometry geometry,
                                     int color, double progress) {
        int coreColor = mix(color, 0xFFFFFF, .78);
        for (Cell cell : cells) {
            double phase = geometry.activationProgress(cell.x() + .5, cell.z() + .5);
            double trail = progress - phase;
            if (trail < -ATTACK_CORE_WIDTH || trail > ATTACK_TRAIL_WIDTH) {
                continue;
            }
            boolean core = Math.abs(trail) <= ATTACK_CORE_WIDTH;
            double fade = core ? 1 : 1 - Math.max(0, trail) / ATTACK_TRAIL_WIDTH;
            int alpha = core ? 246 : 58 + (int) (fade * 132);
            tile(consumer, pose, cell.x(), cell.y() + OUTLINE_OFFSET, cell.z(), core ? TILE_INSET * .2F : TILE_INSET, core ? coreColor : color, alpha);
            if (core && outsideEdge(cell, geometry)) {
                contour(consumer, pose, cell, geometry, coreColor, 255, INNER_OUTLINE_WIDTH, OUTLINE_OFFSET * 3);
            }
        }
    }

    private static void contour(VertexConsumer consumer, PoseStack.Pose pose, Cell cell, AttackGeometry geometry, int color, int alpha,
                                float width, float offset) {
        float y = cell.y() + offset;
        if (!geometry.contains(cell.x() - .5, cell.z() + .5)) {
            strip(consumer, pose, cell.x(), y, cell.z(), 0, 1, 1, width, color, alpha);
        }
        if (!geometry.contains(cell.x() + 1.5, cell.z() + .5)) {
            strip(consumer, pose, cell.x() + 1, y, cell.z(), 0, 1, 1, width, color, alpha);
        }
        if (!geometry.contains(cell.x() + .5, cell.z() - .5)) {
            strip(consumer, pose, cell.x(), y, cell.z(), 1, 0, 1, width, color, alpha);
        }
        if (!geometry.contains(cell.x() + .5, cell.z() + 1.5)) {
            strip(consumer, pose, cell.x(), y, cell.z() + 1, 1, 0, 1, width, color, alpha);
        }
    }

    private static void arrow(VertexConsumer consumer, PoseStack.Pose pose, Cell cell, double directionX, double directionZ,
                              int color, int alpha) {
        if (Math.hypot(directionX, directionZ) < .01) {
            return;
        }
        float centerX = cell.x() + .5F;
        float centerZ = cell.z() + .5F;
        float y = cell.y() + OUTLINE_OFFSET * 2;
        float dx = (float) directionX;
        float dz = (float) directionZ;
        strip(consumer, pose, centerX - dx * .3F, y, centerZ - dz * .3F, dx, dz, .6F, ARROW_WIDTH, color, alpha);
        strip(consumer, pose, centerX + dx * .3F, y, centerZ + dz * .3F,
                -dx + dz, -dz - dx, .25F, ARROW_WIDTH, color, alpha);
        strip(consumer, pose, centerX + dx * .3F, y, centerZ + dz * .3F,
                -dx - dz, -dz + dx, .25F, ARROW_WIDTH, color, alpha);
    }

    private static void strip(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float dx, float dz,
                              float length, float width, int color, int alpha) {
        float magnitude = (float) Math.max(.001, Math.hypot(dx, dz));
        dx /= magnitude;
        dz /= magnitude;
        float perpendicularX = -dz * width;
        float perpendicularZ = dx * width;
        float endX = x + dx * length;
        float endZ = z + dz * length;
        vertex(consumer, pose, x + perpendicularX, y, z + perpendicularZ, 0, 0, color, alpha);
        vertex(consumer, pose, endX + perpendicularX, y, endZ + perpendicularZ, 1, 0, color, alpha);
        vertex(consumer, pose, endX - perpendicularX, y, endZ - perpendicularZ, 1, 1, color, alpha);
        vertex(consumer, pose, x - perpendicularX, y, z - perpendicularZ, 0, 1, color, alpha);
    }

    private static void tile(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float inset, int color, int alpha) {
        float minX = x + inset;
        float minZ = z + inset;
        float maxX = x + 1 - inset;
        float maxZ = z + 1 - inset;
        vertex(consumer, pose, minX, y, minZ, 0, 0, color, alpha);
        vertex(consumer, pose, minX, y, maxZ, 0, 1, color, alpha);
        vertex(consumer, pose, maxX, y, maxZ, 1, 1, color, alpha);
        vertex(consumer, pose, maxX, y, minZ, 1, 0, color, alpha);
    }

    private static int mix(int first, int second, double weight) {
        double clamped = Math.max(0, Math.min(1, weight));
        int red = (int) (((first >> 16 & 255) * (1 - clamped)) + ((second >> 16 & 255) * clamped));
        int green = (int) (((first >> 8 & 255) * (1 - clamped)) + ((second >> 8 & 255) * clamped));
        int blue = (int) (((first & 255) * (1 - clamped)) + ((second & 255) * clamped));
        return red << 16 | green << 8 | blue;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float textureX, float textureY, int color, int alpha) {
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();
        consumer.vertex(position, x, y, z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha)
                .uv(textureX, textureY)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

    private static List<Cell> sample(AttackGeometry geometry) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return List.of();
        }
        List<Cell> cells = new ArrayList<>();
        for (int x = geometry.minX(); x <= geometry.maxX(); x++) {
            for (int z = geometry.minZ(); z <= geometry.maxZ(); z++) {
                if (!geometry.contains(x + .5, z + .5)) {
                    continue;
                }
                cells.add(new Cell(x, z, surfaceY(client.level, x, z)));
            }
        }
        return List.copyOf(cells);
    }

    private static float surfaceY(ClientLevel level, int x, int z) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos overlayPosition = new BlockPos(x, height, z);
        VoxelShape overlay = level.getBlockState(overlayPosition).getBlockSupportShape(level, overlayPosition);
        return height + (overlay.isEmpty() ? 0 : (float) overlay.max(Direction.Axis.Y)) + GROUND_OFFSET;
    }

    private static boolean outsideEdge(Cell cell, AttackGeometry geometry) {
        return !geometry.contains(cell.x() - .5, cell.z() + .5)
                || !geometry.contains(cell.x() + 1.5, cell.z() + .5)
                || !geometry.contains(cell.x() + .5, cell.z() - .5)
                || !geometry.contains(cell.x() + .5, cell.z() + 1.5);
    }

    private static int warningAlpha(double progress) {
        double pulse = (Math.sin(progress * Math.PI * (4 + progress * 8)) + 1) * .5;
        return Math.min(76, 28 + (int) (progress * 30) + (int) (pulse * 18));
    }

    private static double progress(double now, long started, long duration) {
        return Math.max(0, Math.min(1, (now - started) / Math.max(1, duration)));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clear();
        }
    }

    private static void clear() {
        TELEGRAPHS.clear();
        ATTACKS.clear();
    }

    static final class ShaderState {
        private static final Object API = api();
        private static final Method SHADOW = method(API, "isRenderingShadowPass");

        static boolean shadowPass() {
            return invokeBoolean(API, SHADOW);
        }

        private static Object api() {
            if (!ModList.get().isLoaded("oculus")) {
                return null;
            }
            try {
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                return type.getMethod("getInstance").invoke(null);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static Method method(Object owner, String name) {
            if (owner == null) {
                return null;
            }
            try {
                return owner.getClass().getMethod(name);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static boolean invokeBoolean(Object owner, Method method) {
            if (owner == null || method == null) {
                return false;
            }
            try {
                return Boolean.TRUE.equals(method.invoke(owner));
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }
    }

    private EventAttackRenderer() {
    }
}
