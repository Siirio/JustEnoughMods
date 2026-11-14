package com.siirio.jemlightingsandbox.terrain;

import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class EmbeddiumTerrainShaders {
    private static final String VERTEX = "blocks/block_layer_opaque.vsh";
    private static final String FRAGMENT = "blocks/block_layer_opaque.fsh";
    private static final String STOCK_ROOT = "assets/sodium/shaders/";
    private static final String LIGHTING = "/assets/jem_lighting_sandbox/shaders/terrain/lighting.glsl";
    private static final String CUTOUT_SHADOW = "/assets/jem_lighting_sandbox/shaders/include/cutout_shadow.glsl";
    private static final String VERTEX_COLOR = "out vec4 v_Color;";
    private static final String VERTEX_LIGHTMAP =
            "    v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);";
    private static final String FRAGMENT_MAIN = "void main() {";
    private static final String FRAGMENT_COLOR = "#ifdef USE_VANILLA_COLOR_FORMAT";
    private static final String FRAGMENT_FOG =
            "    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);";

    private static final ThreadLocal<Boolean> PATCHED_VERTEX = new ThreadLocal<>();
    private static Sources sources;
    private static boolean initialized;

    private EmbeddiumTerrainShaders() {}

    public static boolean isTerrainStage(String path) {
        return VERTEX.equals(path) || FRAGMENT.equals(path);
    }

    public static synchronized String patch(String path, String source, ClassLoader stockLoader) {
        if (!initialized) {
            initialized = true;
            try {
                sources = loadPair(stockLoader);
            } catch (IOException | IllegalArgumentException exception) {
                LogUtils.getLogger().warn("JEM terrain lighting could not load the Embeddium shader pair", exception);
            }
        }
        if (sources == null) {
            return source;
        }
        if (VERTEX.equals(path)) {
            boolean stockVertex = sources.vertex().stock().equals(source);
            PATCHED_VERTEX.set(stockVertex);
            return stockVertex ? sources.vertex().patched() : source;
        }
        // Embeddium loads the vertex stage first. A stock fragment has no JEM inputs.
        boolean patchedVertex = Boolean.TRUE.equals(PATCHED_VERTEX.get());
        PATCHED_VERTEX.remove();
        return patchedVertex && sources.fragment().stock().equals(source) ? sources.fragment().patched() : source;
    }

    private static Sources loadPair(ClassLoader stockLoader) throws IOException {
        String vertex = read(stockLoader.getResourceAsStream(STOCK_ROOT + VERTEX), VERTEX);
        String fragment = read(stockLoader.getResourceAsStream(STOCK_ROOT + FRAGMENT), FRAGMENT);
        String lighting = read(EmbeddiumTerrainShaders.class.getResourceAsStream(LIGHTING), LIGHTING);
        String cutout = read(EmbeddiumTerrainShaders.class.getResourceAsStream(CUTOUT_SHADOW), CUTOUT_SHADOW);
        return new Sources(new Stage(vertex, patchVertex(vertex)),
                new Stage(fragment, patchFragment(fragment, cutout + "\n" + lighting)));
    }

    private static String patchVertex(String source) {
        source = replace(source, VERTEX_COLOR, VERTEX_COLOR + "\n" + """
                out vec3 v_JemPosition;
                out vec4 v_JemUnlitColor;
                out vec4 v_JemSkyColor;
                out vec2 v_JemLightLevels;
                flat out uint v_JemWater;
                """);
        return replace(source, VERTEX_LIGHTMAP, VERTEX_LIGHTMAP + "\n" + """
                    v_JemPosition = position;
                    v_JemUnlitColor = _vert_color;
                    v_JemSkyColor = _vert_color * _sample_lightmap(u_LightTex, ivec2(0, _vert_tex_light_coord.y));
                    v_JemLightLevels = clamp(vec2(_vert_tex_light_coord) / 240.0, 0.0, 1.0);
                    v_JemWater = _material_params & 128u;
                """);
    }

    private static String patchFragment(String source, String lighting) {
        source = replace(source, FRAGMENT_MAIN, """
                in vec3 v_JemPosition;
                in vec4 v_JemUnlitColor;
                in vec4 v_JemSkyColor;
                in vec2 v_JemLightLevels;
                flat in uint v_JemWater;

                """ + lighting + "\n" + FRAGMENT_MAIN + "\n" + """
                    vec3 jemTangentX = dFdx(v_JemPosition);
                    vec3 jemTangentY = dFdy(v_JemPosition);
                """);
        source = replace(source, FRAGMENT_COLOR, "    vec3 jemTextureColor = diffuseColor.rgb;\n\n" + FRAGMENT_COLOR);
        return replace(source, FRAGMENT_FOG, """
                    vec3 jemAlbedo = jemTextureColor * v_JemUnlitColor.rgb;
                    vec3 jemSkyBaseColor = jemTextureColor * v_JemSkyColor.rgb;
                #ifndef USE_VANILLA_COLOR_FORMAT
                    jemAlbedo *= v_JemUnlitColor.a;
                    jemSkyBaseColor *= v_JemSkyColor.a;
                #endif
                    diffuseColor.rgb = jemApplyLighting(diffuseColor.rgb, jemSkyBaseColor, jemAlbedo,
                        v_JemPosition, v_JemLightLevels, jemTangentX, jemTangentY);
                    if (v_JemWater != 0u) {
                        diffuseColor = jemWaterSurface(diffuseColor, v_JemPosition, v_JemLightLevels);
                    }

                """ + FRAGMENT_FOG);
    }

    private static String replace(String source, String marker, String replacement) {
        int index = source.indexOf(marker);
        if (index < 0 || source.indexOf(marker, index + marker.length()) >= 0) {
            throw new IllegalArgumentException("Expected one Embeddium shader marker: " + marker);
        }
        return source.substring(0, index) + replacement + source.substring(index + marker.length());
    }

    private static String read(InputStream stream, String path) throws IOException {
        if (stream == null) {
            throw new IOException("Missing shader resource " + path);
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Sources(Stage vertex, Stage fragment) {}

    private record Stage(String stock, String patched) {}
}
