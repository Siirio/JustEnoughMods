package dev.taste.engine.light;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.taste.engine.material.MaterialDescription;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

/** Conservatively voxelizes emitted model triangles into toroidal occupancy and material textures. */
public final class VoxelOccupancyPass implements AutoCloseable {
    private static final int BANKS = 13;
    private static final int GROUP_SIZE = 64;
    private static final int FACE_FLOATS = 4 * VoxelSectionGeometry.VERTEX_FLOATS + 4;
    private static final int OCCUPANCY_FORMAT = GL11.GL_RGBA8;
    private static final int ORIENTATION_FORMAT = GL11.GL_RGBA8;
    private static final int EMISSION_FORMAT = GL30.GL_RGBA16F;

    /** Half-open bounds in absolute voxel-cell coordinates. */
    public record Cells(int x0, int y0, int z0, int x1, int y1, int z1) {
        public Cells {
            if (x1 <= x0 || y1 <= y0 || z1 <= z0) throw new IllegalArgumentException("Invalid voxel region");
        }
        public int width() { return x1 - x0; }
        public int height() { return y1 - y0; }
        public int depth() { return z1 - z0; }
        public int count() { return Math.multiplyExact(Math.multiplyExact(width(), height()), depth()); }
    }

    public static final class Volume implements AutoCloseable {
        private final int resolution;
        private int occupancy;
        private int orientation;
        private int emission;

        private Volume(int resolution) {
            this.resolution = resolution;
            try {
                occupancy = texture(resolution, resolution, resolution, OCCUPANCY_FORMAT);
                orientation = texture(resolution, resolution, resolution, ORIENTATION_FORMAT);
                emission = texture(resolution, resolution, resolution, EMISSION_FORMAT);
            } catch (RuntimeException failure) {
                close();
                throw failure;
            }
        }

        public int resolution() { return resolution; }
        public int occupancy() { return occupancy; }
        public int orientation() { return orientation; }
        public int emission() { return emission; }
        public long bytes() { return (long) resolution * resolution * resolution * 16L; }

        @Override public void close() {
            if (occupancy != 0) GL11.glDeleteTextures(occupancy);
            if (orientation != 0) GL11.glDeleteTextures(orientation);
            if (emission != 0) GL11.glDeleteTextures(emission);
            occupancy = orientation = emission = 0;
        }
    }

    private record Uniforms(int atlas, int operation, int resolution, int faces, int minimum,
            int extent, int offset, int cellSize) {
        static Uniforms read(int program) {
            return new Uniforms(uniform(program, "BlockAtlas"), uniform(program, "Operation"),
                    uniform(program, "Resolution"), uniform(program, "FaceCount"), uniform(program, "Minimum"),
                    uniform(program, "Extent"), uniform(program, "SectionCellOffset"), uniform(program, "CellSize"));
        }
    }

    record ImageBinding(int name, int level, boolean layered, int layer, int access, int format) {
        static ImageBinding read(int unit) {
            return new ImageBinding(GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_NAME, unit),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_LEVEL, unit),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_LAYERED, unit) != 0,
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_LAYER, unit),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_ACCESS, unit),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_FORMAT, unit));
        }

        void restore(int unit) {
            GL42.glBindImageTexture(unit, name, level, layered, layer,
                    name == 0 ? GL15.GL_READ_ONLY : access, name == 0 ? GL30.GL_R32UI : format);
        }
    }

    private int program;
    private Uniforms uniforms;
    private int accumulator;
    private int accumulatorResolution;
    private int faces;
    private boolean batchActive;

    public void prepare(ResourceManager resources) throws IOException {
        RenderSystem.assertOnRenderThread();
        if (program != 0) return;
        if (!GL.getCapabilities().OpenGL43) throw new IllegalStateException("Voxel occupancy requires OpenGL 4.3");
        int next = computeProgram(resources, "taste_voxel_occupancy");
        try {
            uniforms = Uniforms.read(next);
            faces = GL15.glGenBuffers();
            int prior = GL11.glGetInteger(GL43.GL_SHADER_STORAGE_BUFFER_BINDING);
            try {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, faces);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 16L, GL15.GL_STREAM_DRAW);
            } finally {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, prior);
            }
            program = next;
        } catch (RuntimeException failure) {
            GL20.glDeleteProgram(next);
            if (faces != 0) GL15.glDeleteBuffers(faces);
            faces = 0;
            throw failure;
        }
    }

    public Volume createVolume(int resolution) {
        RenderSystem.assertOnRenderThread();
        if (resolution < 8 || resolution > GL11.glGetInteger(GL12.GL_MAX_3D_TEXTURE_SIZE) / BANKS)
            throw new IllegalArgumentException("Voxel resolution exceeds image limits");
        return new Volume(resolution);
    }

    public static long scratchBytes(int resolution) {
        long accumulatorBytes = (long) BANKS * resolution * resolution * resolution * Integer.BYTES;
        long faceBytes = (long) VoxelSectionGeometry.MAX_QUADS * FACE_FLOATS * Float.BYTES;
        return accumulatorBytes + faceBytes;
    }

    public Batch begin(Volume volume, Cells dirty, float voxelSize, int atlas) {
        Batch batch = bind(volume, dirty, voxelSize, atlas);
        try {
            batch.dispatch(0, dirty);
            return batch;
        } catch (RuntimeException failure) {
            batch.close();
            throw failure;
        }
    }

    public void invalidate(Volume volume, Cells dirty) {
        try (Batch batch = bind(volume, dirty, 1.0F, 0)) {
            batch.dispatch(4, dirty);
        }
    }

    private Batch bind(Volume volume, Cells dirty, float voxelSize, int atlas) {
        RenderSystem.assertOnRenderThread();
        if (batchActive || program == 0 || volume.occupancy == 0)
            throw new IllegalStateException("Voxel occupancy program or batch state is invalid");
        if (Math.max(dirty.width(), Math.max(dirty.height(), dirty.depth())) > volume.resolution)
            throw new IllegalArgumentException("Voxel region spans multiple clipmap wraps");
        if (voxelSize <= 0.0F || !Float.isFinite(voxelSize)) throw new IllegalArgumentException("Invalid voxel size");
        if (accumulatorResolution != volume.resolution) {
            int size = volume.resolution;
            int next = texture(size, size, size * BANKS, GL30.GL_R32UI);
            if (accumulator != 0) GL11.glDeleteTextures(accumulator);
            accumulator = next;
            accumulatorResolution = size;
        }
        return new Batch(volume, dirty, voxelSize, atlas);
    }

    public final class Batch implements AutoCloseable {
        private final Cells dirty;
        private final float size;
        private final ImageBinding[] previousImages = new ImageBinding[4];
        private final int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int previousBuffer = GL11.glGetInteger(GL43.GL_SHADER_STORAGE_BUFFER_BINDING);
        private final int previousIndexedBuffer = GL30.glGetIntegeri(GL43.GL_SHADER_STORAGE_BUFFER_BINDING, 0);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int previousAtlas;
        private boolean committed;
        private boolean closed;

        private Batch(Volume volume, Cells dirty, float size, int atlas) {
            this.dirty = dirty;
            this.size = size;
            for (int index = 0; index < previousImages.length; index++) previousImages[index] = ImageBinding.read(index);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            previousAtlas = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas);
            GL20.glUseProgram(program);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, faces);
            image(0, accumulator, GL15.GL_READ_WRITE, GL30.GL_R32UI);
            image(1, volume.occupancy, GL15.GL_WRITE_ONLY, OCCUPANCY_FORMAT);
            image(2, volume.orientation, GL15.GL_WRITE_ONLY, ORIENTATION_FORMAT);
            image(3, volume.emission, GL15.GL_WRITE_ONLY, EMISSION_FORMAT);
            GL20.glUniform1i(uniforms.atlas(), 0);
            GL20.glUniform1i(uniforms.resolution(), volume.resolution);
            GL20.glUniform1f(uniforms.cellSize(), size);
            batchActive = true;
        }

        /** Marks cells from a known loaded section as valid, including its empty cells. */
        public void loaded(Cells region) {
            requireOpen();
            if (region.x0() < dirty.x0() || region.y0() < dirty.y0() || region.z0() < dirty.z0()
                    || region.x1() > dirty.x1() || region.y1() > dirty.y1() || region.z1() > dirty.z1())
                throw new IllegalArgumentException("Loaded cells must lie inside the dirty region");
            dispatch(1, region);
        }

        public void draw(VoxelSectionGeometry.Coordinate coordinate, VoxelSectionGeometry.Mesh mesh) {
            requireOpen();
            if (mesh.quads() == 0) return;
            FloatBuffer packed = MemoryUtil.memAllocFloat(Math.multiplyExact(mesh.quads(), FACE_FLOATS));
            float minX = (float) (dirty.x0() * (double) size - coordinate.blockX()) - 0.001F;
            float minY = (float) (dirty.y0() * (double) size - coordinate.blockY()) - 0.001F;
            float minZ = (float) (dirty.z0() * (double) size - coordinate.blockZ()) - 0.001F;
            float maxX = (float) (dirty.x1() * (double) size - coordinate.blockX()) + 0.001F;
            float maxY = (float) (dirty.y1() * (double) size - coordinate.blockY()) + 0.001F;
            float maxZ = (float) (dirty.z1() * (double) size - coordinate.blockZ()) + 0.001F;
            int faceCount = 0;
            try {
                FloatBuffer vertices = mesh.vertices();
                for (int quad = 0; quad < mesh.quads(); quad++) {
                    int start = quad * 4 * VoxelSectionGeometry.VERTEX_FLOATS;
                    if (!overlaps(vertices, start, 0, minX, maxX)
                            || !overlaps(vertices, start, 1, minY, maxY)
                            || !overlaps(vertices, start, 2, minZ, maxZ)) continue;
                    faceCount++;
                    for (int corner = 0; corner < 4; corner++) {
                        int vertex = quad * 4 + corner;
                        int raw = mesh.rawColor(vertex);
                        int vertexStart = vertex * VoxelSectionGeometry.VERTEX_FLOATS;
                        for (int channel = 0; channel < VoxelSectionGeometry.VERTEX_FLOATS; channel++) {
                            float value = switch (channel) {
                                case 6 -> (raw >> 16 & 255) / 255.0F;
                                case 7 -> (raw >> 8 & 255) / 255.0F;
                                case 8 -> (raw & 255) / 255.0F;
                                default -> vertices.get(vertexStart + channel);
                            };
                            packed.put(value);
                        }
                    }
                    MaterialDescription material = mesh.surface(quad).material();
                    var metadata = material.metadata();
                    Integer emissionTint = metadata.emissionColorRgb();
                    packed.put(metadata.opacity());
                    packed.put(material.flags().cutout() ? metadata.cutoutThreshold() : 0.0F);
                    packed.put(material.emission());
                    packed.put(emissionTint == null ? -1.0F : emissionTint.floatValue());
                }
                if (faceCount == 0) return;
                packed.flip();
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, packed, GL15.GL_STREAM_DRAW);
            } finally {
                MemoryUtil.memFree(packed);
            }
            GL20.glUniform1i(uniforms.operation(), 2);
            GL20.glUniform1i(uniforms.faces(), faceCount);
            GL20.glUniform3f(uniforms.offset(), local(coordinate.blockX(), dirty.x0()),
                    local(coordinate.blockY(), dirty.y0()), local(coordinate.blockZ(), dirty.z0()));
            bounds(dirty);
            compute(faceCount);
        }

        private boolean overlaps(FloatBuffer vertices, int start, int axis, float min, float max) {
            float low = Float.POSITIVE_INFINITY;
            float high = Float.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                float value = vertices.get(start + vertex * VoxelSectionGeometry.VERTEX_FLOATS + axis);
                low = Math.min(low, value);
                high = Math.max(high, value);
            }
            return low <= max && high >= min;
        }

        public void commit() {
            requireOpen();
            dispatch(3, dirty);
            committed = true;
        }

        private float local(int block, int cell) { return (float) ((double) block / size - cell); }

        private void dispatch(int operation, Cells region) {
            GL20.glUniform1i(uniforms.operation(), operation);
            bounds(region);
            compute(region.count());
        }

        private void bounds(Cells region) {
            GL20.glUniform3i(uniforms.minimum(), region.x0(), region.y0(), region.z0());
            GL20.glUniform3i(uniforms.extent(), region.width(), region.height(), region.depth());
        }

        private void requireOpen() {
            if (closed || committed) throw new IllegalStateException("Voxel batch no longer accepts changes");
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            batchActive = false;
            for (int index = 0; index < previousImages.length; index++) previousImages[index].restore(index);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, previousIndexedBuffer);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, previousBuffer);
            GL20.glUseProgram(previousProgram);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousAtlas);
            GL13.glActiveTexture(activeTexture);
        }
    }

    private static void image(int unit, int texture, int access, int format) {
        GL42.glBindImageTexture(unit, texture, 0, true, 0, access, format);
    }

    private static void compute(int count) {
        GL43.glDispatchCompute((count + GROUP_SIZE - 1) / GROUP_SIZE, 1, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL42.GL_TEXTURE_FETCH_BARRIER_BIT);
    }

    static int texture(int width, int height, int depth, int format) {
        return texture(width, height, depth, format, 1, false);
    }

    static int texture(int width, int height, int depth, int format, int levels, boolean filtered) {
        int prior = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
        int texture = GL11.glGenTextures();
        try {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, texture);
            GL42.glTexStorage3D(GL12.GL_TEXTURE_3D, levels, format, width, height, depth);
            int sample = filtered ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            int minification = levels > 1 ? GL11.GL_LINEAR_MIPMAP_LINEAR : sample;
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, minification);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, sample);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_MAX_LEVEL, levels - 1);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR)
                throw new IllegalStateException("Voxel texture allocation failed with OpenGL error 0x" + Integer.toHexString(error));
            return texture;
        } catch (RuntimeException failure) {
            GL11.glDeleteTextures(texture);
            throw failure;
        } finally {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, prior);
        }
    }

    static int computeProgram(ResourceManager resources, String name) throws IOException {
        String source;
        var location = new ResourceLocation("taste", "shaders/gi/" + name + ".comp");
        try (var input = resources.getResourceOrThrow(location).open()) {
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        int program = 0;
        try {
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new IOException(name + " compute compile failed: " + GL20.glGetShaderInfoLog(shader));
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, shader);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
                throw new IOException(name + " compute link failed: " + GL20.glGetProgramInfoLog(program));
            GL20.glDetachShader(program, shader);
            return program;
        } catch (IOException | RuntimeException failure) {
            if (program != 0) GL20.glDeleteProgram(program);
            throw failure;
        } finally {
            GL20.glDeleteShader(shader);
        }
    }

    static int uniform(int program, String name) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) throw new IllegalStateException("Missing voxel uniform " + name);
        return location;
    }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if (batchActive) throw new IllegalStateException("Cannot close active voxel batch");
        if (program != 0) GL20.glDeleteProgram(program);
        if (faces != 0) GL15.glDeleteBuffers(faces);
        if (accumulator != 0) GL11.glDeleteTextures(accumulator);
        program = faces = accumulator = accumulatorResolution = 0;
        uniforms = null;
    }
}
