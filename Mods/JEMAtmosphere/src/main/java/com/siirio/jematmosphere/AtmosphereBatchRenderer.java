package com.siirio.jematmosphere;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

final class AtmosphereBatchRenderer {
    private AtmosphereBatchRenderer() {
    }

    static void draw(BufferBuilder.RenderedBuffer vertices, ShaderInstance shader, ResourceLocation texture) {
        var previousShader = RenderSystem.getShader();
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(activeTexture);

        try {
            shader.setSampler("Sampler0", Minecraft.getInstance().getTextureManager().getTexture(texture).getId());
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
            shader.COLOR_MODULATOR.set(1F, 1F, 1F, 1F);
            shader.apply();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableCull();
            BufferUploader.draw(vertices);
        } finally {
            shader.clear();
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(previousTexture);
            GlStateManager._activeTexture(activeTexture);
            RenderSystem.setShader(() -> previousShader);
            GlStateManager._glUseProgram(previousProgram);
            RenderSystem.depthMask(depthMask);
            GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (!depth) {
                RenderSystem.disableDepthTest();
            }
            if (!blend) {
                RenderSystem.disableBlend();
            }
            if (cull) {
                RenderSystem.enableCull();
            }
        }
    }
}
