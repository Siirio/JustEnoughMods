package dev.taste.engine.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.render.GlStateScope;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Accumulates a render-only coating on loaded, exposed full top faces. */
public final class SnowSurfaceOverlay {
    private static final int RADIUS = 11;
    private static final int SIDE = RADIUS * 2 + 1;
    private static final int COLUMNS_PER_UPDATE = 40;
    private static final int CAPACITY = 512;
    private final Map<BlockPos, Patch> patches = new LinkedHashMap<>();
    private ClientLevel level;
    private long lastTick = Long.MIN_VALUE;

    private static final class Patch {
        private final BlockState state;
        private float coverage;

        Patch(BlockState state, float coverage) {
            this.state = state;
            this.coverage = coverage;
        }
    }

    public void tick(Minecraft client, ConfigSnapshot settings, boolean graphicsActive) {
        if (level != client.level) {
            patches.clear();
            level = client.level;
            lastTick = Long.MIN_VALUE;
        }
        if (!graphicsActive || level == null || !active(settings) || !level.dimensionType().hasSkyLight()) {
            patches.clear();
            return;
        }
        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || client.isPaused()) return;
        long tick = level.getGameTime();
        if ((tick & 3) != 0 || tick == lastTick) return;
        lastTick = tick;
        Vec3 eye = camera.getPosition();
        BlockPos center = BlockPos.containing(eye);
        float precipitation = level.getRainLevel(1.0F);

        patches.entrySet().removeIf(entry -> {
            BlockPos position = entry.getKey();
            Patch patch = entry.getValue();
            if (Math.abs(position.getX() - center.getX()) > RADIUS + 5
                    || Math.abs(position.getZ() - center.getZ()) > RADIUS + 5
                    || !level.hasChunkAt(position) || level.getBlockState(position) != patch.state
                    || !exposed(position)) return true;
            if (precipitation <= 0.06F || !snowingAt(position)) {
                patch.coverage = Math.max(0, patch.coverage - 0.012F);
            }
            return patch.coverage <= 0;
        });
        if (precipitation <= 0.06F) return;

        int start = Math.floorMod(tick / 4 * COLUMNS_PER_UPDATE, SIDE * SIDE);
        for (int i = 0; i < COLUMNS_PER_UPDATE; i++) {
            int column = (start + i) % (SIDE * SIDE);
            int x = center.getX() + column % SIDE - RADIUS;
            int z = center.getZ() + column / SIDE - RADIUS;
            if (!level.hasChunkAt(x, z)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            if (y < level.getMinBuildHeight() || Math.abs(y + 1 - eye.y) > 10) continue;
            BlockPos position = new BlockPos(x, y, z);
            if (!snowingAt(position)) continue;
            Patch patch = patches.get(position);
            if (patch != null) {
                patch.coverage = Math.min(1.0F, patch.coverage + precipitation * 0.16F);
                continue;
            }
            BlockState state = level.getBlockState(position);
            if (state.isAir() || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW)
                    || state.hasBlockEntity() || !state.getFluidState().isEmpty() || !state.isSolidRender(level, position)
                    || !Block.isShapeFullBlock(state.getShape(level, position))) continue;
            if (patches.size() >= CAPACITY) patches.remove(patches.keySet().iterator().next());
            patches.put(position, new Patch(state, precipitation * 0.16F));
        }
    }

    public void render(Minecraft client, Camera camera, Matrix4f pose, ConfigSnapshot settings) {
        if (patches.isEmpty() || !active(settings) || level != client.level) return;
        Vec3 eye = camera.getPosition();
        try (GlStateScope ignored = GlStateScope.capture()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (var entry : patches.entrySet()) {
                BlockPos position = entry.getKey();
                Patch patch = entry.getValue();
                if (Vec3.atCenterOf(position).distanceToSqr(eye) > 23.0 * 23.0
                        || !level.hasChunkAt(position) || level.getBlockState(position) != patch.state
                        || !exposed(position)) continue;
                emit(buffer, pose, position, eye, patch.coverage);
            }
            BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
            if (rendered != null) BufferUploader.drawWithShader(rendered);
        }
    }

    private void emit(BufferBuilder buffer, Matrix4f pose, BlockPos position, Vec3 eye, float coverage) {
        float opacity = Math.min(0.67F, coverage * coverage * 0.70F + coverage * 0.08F);
        if (opacity < 0.006F) return;
        BlockPos surface = position.above();
        int block = level.getBrightness(LightLayer.BLOCK, surface);
        int sky = Math.max(0, level.getBrightness(LightLayer.SKY, surface) - level.getSkyDarken());
        float light = 0.26F + Math.max(block, sky) / 15.0F * 0.67F;
        float warm = Math.max(0, block - sky) / 15.0F * 0.08F;
        float red = light * 0.92F + warm;
        float green = light * 0.96F + warm * 0.42F;
        float blue = light;
        double x = position.getX() - eye.x;
        double y = position.getY() + 1.0024 - eye.y;
        double z = position.getZ() - eye.z;
        long seed = position.asLong() * 0x9E3779B97F4A7C15L;
        double inset = 0.31 * (1 - coverage);
        double offsetX = (((seed >>> 16) & 15) / 15.0 - 0.5) * inset;
        double offsetZ = (((seed >>> 30) & 15) / 15.0 - 0.5) * inset;
        double left = x + inset + offsetX;
        double right = x + 1 - inset + offsetX;
        double near = z + inset + offsetZ;
        double far = z + 1 - inset + offsetZ;
        double fringe = 0.045;
        quad(buffer, pose, y, left + fringe, near + fringe, right - fringe, far - fringe,
                red, green, blue, opacity, opacity, opacity, opacity);
        quad(buffer, pose, y, left, near, right, near + fringe,
                red, green, blue, 0, opacity, opacity, 0);
        quad(buffer, pose, y, left, far - fringe, right, far,
                red, green, blue, opacity, 0, 0, opacity);
        quad(buffer, pose, y, left, near + fringe, left + fringe, far - fringe,
                red, green, blue, 0, 0, opacity, opacity);
        quad(buffer, pose, y, right - fringe, near + fringe, right, far - fringe,
                red, green, blue, opacity, opacity, 0, 0);
    }

    private static void quad(BufferBuilder buffer, Matrix4f pose, double y, double x0, double z0, double x1, double z1,
            float r, float g, float b, float a0, float a1, float a2, float a3) {
        buffer.vertex(pose, (float) x0, (float) y, (float) z0).color(r, g, b, a0).endVertex();
        buffer.vertex(pose, (float) x0, (float) y, (float) z1).color(r, g, b, a1).endVertex();
        buffer.vertex(pose, (float) x1, (float) y, (float) z1).color(r, g, b, a2).endVertex();
        buffer.vertex(pose, (float) x1, (float) y, (float) z0).color(r, g, b, a3).endVertex();
    }

    private boolean exposed(BlockPos position) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ()) == position.getY() + 1;
    }

    private boolean snowingAt(BlockPos position) {
        BlockPos surface = position.above();
        return level.getBiome(surface).value().getPrecipitationAt(surface) == Biome.Precipitation.SNOW;
    }

    private static boolean active(ConfigSnapshot settings) {
        return settings.requestedEnabled() && settings.value("weather.snowSurface").getAsBoolean();
    }
}
