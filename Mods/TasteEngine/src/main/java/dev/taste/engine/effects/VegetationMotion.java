package dev.taste.engine.effects;

import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.PipelineParameters;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.lwjgl.opengl.GL20;

/** Shares the wind clock and packed per-vertex anchors between the two terrain layouts. */
public final class VegetationMotion {
    private static final Set<String> VANILLA_PROGRAMS = Set.of("rendertype_solid", "rendertype_cutout",
            "rendertype_cutout_mipped", "rendertype_translucent", "rendertype_tripwire");
    private static final ThreadLocal<Mesh> MESH = new ThreadLocal<>();
    private static final String GLSL = loadShader();
    private static FrameWind active;
    private static FrameWind lastFrame;
    private static FrameSnapshot.Vec3d drawOrigin;

    private record FrameWind(long epoch, long index, FrameSnapshot.Vec3d camera, float phase, float gust, float windX, float windZ,
            float grass, float leaves, float crops, float vines, float water) { }
    private static FrameWind previous;
    private record Mesh(int type, float base, float upper, float height) { }

    private VegetationMotion() { }

    public static void beginFrame(FrameSnapshot frame, PipelineParameters options, EnvironmentSnapshot environment) {
        drawOrigin = null;
        if (!options.value("vegetation.enabled").getAsBoolean()
                || options.value("accessibility.reducedMotion").getAsBoolean()) {
            active = null;
            previous = null;
            lastFrame = null;
            return;
        }
        double phase = frame.timeSeconds() * number(options, "vegetation.speed");
        active = new FrameWind(frame.worldEpoch(), frame.frameIndex(), frame.cameraWorld(), (float) (phase % (Math.PI * 160.0)),
                number(options, "vegetation.gustStrength"), environment.windX(), environment.windZ(),
                number(options, "vegetation.grassStrength"), number(options, "vegetation.leavesStrength"),
                number(options, "vegetation.cropsStrength"), number(options, "vegetation.vinesStrength"),
                options.value("vegetation.waterPlants").getAsBoolean() ? 0.7F : 0F);
        previous = lastFrame != null && lastFrame.epoch() == active.epoch() && lastFrame.index() + 1 == active.index()
                ? lastFrame : active;
    }

    public static void endFrame() {
        if (active != null) lastFrame = active;
        active = null;
        previous = null;
        drawOrigin = null;
    }

    public static FrameSnapshot.Vec3d drawFrom(FrameSnapshot.Vec3d origin) {
        FrameSnapshot.Vec3d previousOrigin = drawOrigin;
        drawOrigin = origin;
        return previousOrigin;
    }
    public static void drawFromCamera() { drawOrigin = null; }
    public static boolean affectsCasters() {
        FrameWind frame = active;
        return frame != null && (frame.grass() > 0 || frame.leaves() > 0 || frame.crops() > 0 || frame.vines() > 0 || frame.water() > 0);
    }

    public static void beginMesh(BlockState state, float baseY) {
        int type = classify(state);
        if (type == 0) {
            MESH.remove();
            return;
        }
        boolean tall = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && (type == 1 || type == 3 || type == 6);
        float upper = tall && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? 1F : 0F;
        MESH.set(new Mesh(type, baseY, upper, tall ? 2F : 1F));
    }

    public static void endMesh() { MESH.remove(); }
    public static boolean hasMesh() { return MESH.get() != null; }

    /** The five-bit record occupies unused bytes or bits in the native meshes. */
    public static int vertex(float y) {
        Mesh mesh = MESH.get();
        if (mesh == null) return 0;
        return encode(mesh.type(), y - mesh.base(), mesh.upper(), mesh.height());
    }

    public static int vertex(BlockState state, float localY) {
        int type = classify(state);
        if (type == 0) return 0;
        boolean tall = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && (type == 1 || type == 3 || type == 6);
        float upper = tall && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? 1F : 0F;
        return encode(type, localY, upper, tall ? 2F : 1F);
    }

    private static int encode(int type, float y, float upper, float height) {
        float weight = Math.max(0F, Math.min(1F, (y + upper) / height));
        if (type == 2) weight = 1F;
        if (type == 4 || type == 5) weight = 1F - weight;
        return type | (Math.round(weight * 3F) << 3);
    }

    private static int classify(BlockState state) {
        if (state.is(BlockTags.LEAVES)) return 2;
        if (state.getBlock() instanceof VineBlock) {
            boolean zFace = state.getValue(VineBlock.NORTH) || state.getValue(VineBlock.SOUTH);
            boolean xFace = state.getValue(VineBlock.EAST) || state.getValue(VineBlock.WEST);
            return zFace == xFace ? 0 : zFace ? 4 : 5;
        }
        if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)) return 6;
        if (state.getBlock() instanceof CropBlock || state.is(BlockTags.FLOWERS)) return 3;
        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)) return 1;
        return 0;
    }

    public static boolean isVanillaProgram(String name) {
        return VANILLA_PROGRAMS.contains(name) || name.startsWith("minecraft:")
                && VANILLA_PROGRAMS.contains(name.substring("minecraft:".length()));
    }

    public static InputStream vanillaSource(String name, InputStream original) throws IOException {
        if (!isVanillaProgram(name)) return original;
        String source = new String(original.readAllBytes(), StandardCharsets.UTF_8);
        if (source.contains("in vec3 Normal;") && source.contains("vec3 pos = Position + ChunkOffset;")) {
            source = source.replace("in vec3 Normal;", "in vec4 Normal;")
                    .replace("vec4(Normal, 0.0)", "vec4(Normal.xyz, 0.0)")
                    .replace("void main() {", GLSL + "\nvoid main() {")
                    .replace("vec3 pos = Position + ChunkOffset;",
                            "vec3 pos = taste_deform(Position + ChunkOffset, int(round(Normal.w * 127.0)));");
        }
        return new java.io.ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
    }

    public static String embeddiumSource(String source) {
        String position = "vec3 position = _vert_position + translation;";
        if (!source.contains(position) || !source.contains("void main() {")) return source;
        return source.replace("void main() {", GLSL + "\nvoid main() {")
                .replace(position, "vec3 position = taste_deform(_vert_position + translation, int(_material_params >> 3u));");
    }

    public static void bind(int program) {
        int clock = GL20.glGetUniformLocation(program, "TasteWindClock");
        if (clock < 0) return;
        FrameWind frame = active;
        if (frame == null) {
            GL20.glUniform4f(clock, 0F, 0F, 0F, 0F);
            GL20.glUniform4f(GL20.glGetUniformLocation(program, "TasteWindPreviousClock"), 0F, 0F, 0F, 0F);
            return;
        }
        FrameSnapshot.Vec3d origin = drawOrigin == null ? frame.camera() : drawOrigin;
        GL20.glUniform4f(clock, frame.phase(), frame.gust(), frame.windX(), frame.windZ());
        FrameWind prior = previous == null ? frame : previous;
        GL20.glUniform4f(GL20.glGetUniformLocation(program, "TasteWindPreviousClock"),
                prior.phase(), prior.gust(), prior.windX(), prior.windZ());
        GL20.glUniform3f(GL20.glGetUniformLocation(program, "TasteWindOrigin"),
                repeat(origin.x()), repeat(origin.y()), repeat(origin.z()));
        GL20.glUniform4f(GL20.glGetUniformLocation(program, "TasteWindPlants"),
                frame.grass(), frame.leaves(), frame.crops(), frame.vines());
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "TasteWindWater"), frame.water());
    }

    private static float repeat(double value) { return (float) (value % 512D); }
    private static float number(PipelineParameters options, String id) { return options.value(id).getAsFloat(); }

    private static String loadShader() {
        try (InputStream stream = VegetationMotion.class.getResourceAsStream("/assets/taste/shaders/include/taste_wind.glsl")) {
            if (stream == null) throw new IllegalStateException("Taste wind shader is missing");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read the Taste wind shader", failure);
        }
    }
}
