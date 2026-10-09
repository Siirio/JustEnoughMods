package com.siirio.jemserver.client.smp;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;

public final class TelegraphRenderType extends RenderType {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final RenderType GROUND = create(
            "jem_telegraph_ground",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            MEDIUM_BUFFER_SIZE,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));
    private static final RenderType BOUNDARY = create(
            "jem_event_boundary",
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

    public static RenderType ground() {
        return GROUND;
    }

    public static RenderType boundary() {
        return BOUNDARY;
    }

    private TelegraphRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
