package dev.taste.engine.light;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import dev.taste.engine.render.backend.TerrainBackend;
import dev.taste.engine.render.embeddium.Embeddium031Backend;
import dev.taste.engine.render.vanilla.VanillaTerrainBackend;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Builds directional depth maps from terrain meshes in independent light views. */
public final class ShadowRenderer implements AutoCloseable {
    public record MapLayer(CascadeController.LightView view, int texture, int resolution, int submissions) { }
    private static final int MAX_CASCADES = 4;
    private final CascadeController cascades = new CascadeController();
    private ShaderInstance shader;

    public List<MapLayer> draw(TerrainBackend terrain, TargetPool targets, FrameSnapshot frame,
            Vector3f towardsSun, int resolution, int count, float distance, float splitLambda) throws IOException {
        CascadeController.Cascade[] splits = cascades.splits(0.05F, distance, count, splitLambda, resolution);
        List<MapLayer> result = new ArrayList<>(count);
        releaseFrom(targets, count);
        RenderSystem.disableScissor();
        BufferUploader.reset();
        for (int index = 0; index < count; index++) {
            CascadeController.LightView view = cascades.fit(splits[index], frame, towardsSun, resolution);
            TextureTarget map = targets.acquire(new TargetDescriptor(targetName(index), resolution,
                    resolution, TargetDescriptor.Format.RGBA8, true, 1, 1, 1));
            RenderSystem.depthMask(true);
            map.clear(Minecraft.ON_OSX);
            map.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.colorMask(false, false, false, false);
            try {
                int submissions = drawCasters(terrain, view.projection(), frame.origin());
                result.add(new MapLayer(view, map.getDepthTextureId(), resolution, submissions));
            } finally {
                RenderSystem.colorMask(true, true, true, true);
            }
        }
        return List.copyOf(result);
    }

    private int drawCasters(TerrainBackend terrain, Matrix4f projection, FrameSnapshot.Vec3d origin) throws IOException {
        if (terrain instanceof Embeddium031Backend embeddium) return embeddium.drawShadowCasters(projection, origin);
        if (!(terrain instanceof VanillaTerrainBackend vanilla))
            throw new IllegalStateException("Unsupported shadow terrain backend " + terrain.id());
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null) {
            shader = new ShaderInstance(minecraft.getResourceManager(),
                    new ResourceLocation("taste", "taste_shadow"), DefaultVertexFormat.BLOCK);
        }
        shader.setSampler("BlockAtlas", minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
        requireUniform("LightProjection").set(projection);
        Uniform sectionOrigin = requireUniform("SectionOrigin");
        Uniform cutoff = requireUniform("AlphaCutoff");
        shader.apply();
        try {
            return vanilla.drawShadowCasters(projection, origin, (mesh, x, y, z, alpha) -> {
                sectionOrigin.set(x, y, z);
                sectionOrigin.upload();
                cutoff.set(alpha);
                cutoff.upload();
                mesh.bind();
                mesh.draw();
            });
        } finally {
            VertexBuffer.unbind();
            shader.clear();
        }
    }

    public void releaseFrom(TargetPool targets, int count) {
        for (int index = count; index < MAX_CASCADES; index++) targets.release(targetName(index));
    }

    private static String targetName(int index) { return "sun-shadow-" + index; }

    private Uniform requireUniform(String name) {
        Uniform value = shader.getUniform(name);
        if (value == null) throw new IllegalStateException("Missing shadow uniform " + name);
        return value;
    }

    @Override
    public void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
