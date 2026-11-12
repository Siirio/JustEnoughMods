package dev.taste.engine.effects;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.api.LightProvider.Light;
import dev.taste.engine.client.ClientEvents;
import dev.taste.engine.effects.ClientInteractionEffects.PortalCell;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.GlStateScope;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.io.IOException;
import java.util.List;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Adds local portal optics to native planes before bloom and tone mapping. */
public final class PortalScenePass implements AutoCloseable {
    private static final String TARGET = "portal-optics";
    private ShaderInstance shader;

    public List<Light> edgeLights(ClientLevel level, FrameSnapshot.Vec3d eye, PipelineParameters settings) {
        if (level == null || !settings.value("portals.enabled").getAsBoolean()
                || !settings.value("portals.edgeRays").getAsBoolean()) return List.of();
        float intensity = settings.value("portals.emission").getAsFloat();
        if (intensity <= 0) return List.of();
        return ClientEvents.portalCells().stream().filter(cell -> valid(level, cell, eye) && edge(level, cell))
                .sorted(Comparator.comparingDouble(cell -> cell.position().distToCenterSqr(eye.x(), eye.y(), eye.z())))
                .limit(2).map(cell -> edgeLight(cell, eye, intensity)).toList();
    }

    private static boolean edge(ClientLevel level, PortalCell cell) {
        var position = cell.position();
        if (cell.end()) return !level.getBlockState(position.north()).is(Blocks.END_PORTAL)
                || !level.getBlockState(position.south()).is(Blocks.END_PORTAL)
                || !level.getBlockState(position.west()).is(Blocks.END_PORTAL)
                || !level.getBlockState(position.east()).is(Blocks.END_PORTAL);
        Direction side = cell.axis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        return !level.getBlockState(position.relative(side)).is(Blocks.NETHER_PORTAL)
                || !level.getBlockState(position.relative(side.getOpposite())).is(Blocks.NETHER_PORTAL)
                || !level.getBlockState(position.above()).is(Blocks.NETHER_PORTAL)
                || !level.getBlockState(position.below()).is(Blocks.NETHER_PORTAL);
    }

    private static Light edgeLight(PortalCell cell, FrameSnapshot.Vec3d eye, float intensity) {
        double x = cell.position().getX() + 0.5, y = cell.position().getY() + 0.5, z = cell.position().getZ() + 0.5;
        if (cell.end()) {
            y = cell.position().getY() + cell.bounds().maxY + 0.11;
            return new Light(x, y, z, 0.10F, 0.32F, 0.38F, Math.min(3, intensity) * 0.8F, 8F, true);
        }
        if (cell.axis() == Direction.Axis.X) z += Math.copySign(0.28, eye.z() - z);
        else x += Math.copySign(0.28, eye.x() - x);
        return new Light(x, y, z, 0.44F, 0.09F, 0.86F, Math.min(4, intensity) * 1.5F, 10F, true);
    }

    public TextureTarget draw(TargetPool targets, FrameSnapshot frame, PipelineParameters settings,
            TextureTarget scene, TextureTarget opaque) throws IOException {
        ClientLevel level = Minecraft.getInstance().level;
        float emission = settings.value("portals.emission").getAsFloat();
        boolean reducedMotion = settings.value("accessibility.reducedMotion").getAsBoolean();
        float distortion = reducedMotion ? 0.0F : settings.value("portals.distortion").getAsFloat();
        if (level == null || scene == null || opaque == null || !settings.value("portals.enabled").getAsBoolean()
                || (emission <= 0 && distortion <= 0) || opaque.getDepthTextureId() <= 0
                || scene.width != opaque.width || scene.height != opaque.height) {
            targets.release(TARGET);
            return scene;
        }
        List<PortalCell> cells = ClientEvents.portalCells().stream()
                .filter(cell -> valid(level, cell, frame.cameraWorld())).toList();
        if (cells.isEmpty()) {
            targets.release(TARGET);
            return scene;
        }

        try (GlStateScope ignored = GlStateScope.capture()) {
            if (shader == null) {
                shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                        new ResourceLocation("taste", "taste_portal_optics"), DefaultVertexFormat.POSITION_TEX_COLOR);
            }
            TextureTarget output = targets.acquire(new TargetDescriptor(TARGET, scene.width, scene.height,
                    TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
            if (output == scene) throw new IllegalArgumentException("Portal pass requires a distinct input target");
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scene.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, output.frameBufferId);
            RenderSystem.disableScissor();
            GL30.glBlitFramebuffer(0, 0, scene.width, scene.height, 0, 0, output.width, output.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, opaque.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, opaque.width, opaque.height, 0, 0, output.width, output.height,
                    GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            output.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            shader.setSampler("PortalScene", scene.getColorTextureId());
            shader.setSampler("PortalDepth", opaque.getDepthTextureId());
            shader.getUniform("ViewProjection").set(new Matrix4f().set(frame.projection().columnMajor())
                    .mul(new Matrix4f().set(frame.view().columnMajor())));
            double phase = reducedMotion ? 0.0
                    : frame.timeSeconds() * settings.value("portals.swirlSpeed").getAsFloat();
            shader.getUniform("VisualTime").set((float) (phase % (200.0 * Math.PI)));
            shader.getUniform("Emission").set(emission);
            shader.getUniform("Distortion").set(distortion);
            BufferBuilder vertices = Tesselator.getInstance().getBuilder();
            vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (PortalCell cell : cells) emit(vertices, cell, frame.origin());
            BufferUploader.drawWithShader(vertices.end());
            return output;
        }
    }

    private static boolean valid(ClientLevel level, PortalCell cell, FrameSnapshot.Vec3d eye) {
        double x = cell.position().getX() + 0.5 - eye.x();
        double y = cell.position().getY() + 0.5 - eye.y();
        double z = cell.position().getZ() + 0.5 - eye.z();
        if (x * x + y * y + z * z > 44.0 * 44.0 || !level.hasChunkAt(cell.position())) return false;
        BlockState state = level.getBlockState(cell.position());
        return cell.end() ? state.is(Blocks.END_PORTAL) : state.is(Blocks.NETHER_PORTAL)
                && state.getValue(NetherPortalBlock.AXIS) == cell.axis();
    }

    private static void emit(BufferBuilder buffer, PortalCell cell, FrameSnapshot.Vec3d origin) {
        AABB shape = cell.bounds();
        double ox = cell.position().getX() - origin.x();
        double oy = cell.position().getY() - origin.y();
        double oz = cell.position().getZ() - origin.z();
        int u = Math.floorMod(cell.axis() == net.minecraft.core.Direction.Axis.Z
                ? cell.position().getZ() : cell.position().getX(), 256);
        int v = Math.floorMod(cell.end() ? cell.position().getZ() : cell.position().getY(), 256);
        if (cell.end()) {
            double y = oy + shape.maxY;
            vertex(buffer, ox + shape.minX, y, oz + shape.minZ, u, v, true);
            vertex(buffer, ox + shape.minX, y, oz + shape.maxZ, u, v + 1, true);
            vertex(buffer, ox + shape.maxX, y, oz + shape.maxZ, u + 1, v + 1, true);
            vertex(buffer, ox + shape.maxX, y, oz + shape.minZ, u + 1, v, true);
        } else if (cell.axis() == net.minecraft.core.Direction.Axis.X) {
            double z = oz + (shape.minZ + shape.maxZ) * 0.5;
            vertex(buffer, ox + shape.minX, oy + shape.minY, z, u, v, false);
            vertex(buffer, ox + shape.minX, oy + shape.maxY, z, u, v + 1, false);
            vertex(buffer, ox + shape.maxX, oy + shape.maxY, z, u + 1, v + 1, false);
            vertex(buffer, ox + shape.maxX, oy + shape.minY, z, u + 1, v, false);
        } else {
            double x = ox + (shape.minX + shape.maxX) * 0.5;
            vertex(buffer, x, oy + shape.minY, oz + shape.minZ, u, v, false);
            vertex(buffer, x, oy + shape.maxY, oz + shape.minZ, u, v + 1, false);
            vertex(buffer, x, oy + shape.maxY, oz + shape.maxZ, u + 1, v + 1, false);
            vertex(buffer, x, oy + shape.minY, oz + shape.maxZ, u + 1, v, false);
        }
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, float u, float v, boolean end) {
        buffer.vertex(x, y, z).uv(u, v).color(end ? 255 : 0, 0, 0, 255).endVertex();
    }

    @Override
    public void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
