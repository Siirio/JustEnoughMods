package dev.taste.engine.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Reads a bounded display-linear .cube lattice supplied by a resource pack. */
final class DisplayLut implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation RESOURCE = new ResourceLocation("taste", "color/grade.cube");
    private static final int MAX_CHARACTERS = 2 * 1024 * 1024;
    private int texture;
    private int size;
    private String status = "Not requested";
    private boolean loaded;

    void load(ResourceManager resources) {
        if (loaded) return;
        loaded = true;
        var source = resources.getResource(RESOURCE);
        if (source.isEmpty()) {
            status = "Resource pack does not provide " + RESOURCE;
            LOGGER.warn("Taste LUT is inactive. {}", status);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.get().open(), StandardCharsets.UTF_8))) {
            Lattice lattice = read(reader);
            upload(lattice);
            status = "Loaded " + RESOURCE;
        } catch (IOException | IllegalArgumentException exception) {
            status = "Cannot load " + RESOURCE + ". " + exception.getMessage();
            LOGGER.warn("Taste LUT is inactive. {}", status);
        }
    }

    private static Lattice read(BufferedReader reader) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] chunk = new char[4096];
        int count;
        while ((count = reader.read(chunk)) != -1) {
            if (text.length() + count > MAX_CHARACTERS) throw new IllegalArgumentException("LUT is larger than 2 MiB");
            text.append(chunk, 0, count);
        }

        int edge = 0;
        int samples = 0;
        FloatBuffer values = null;
        for (String sourceLine : text.toString().split("\\R")) {
            int comment = sourceLine.indexOf('#');
            String line = (comment < 0 ? sourceLine : sourceLine.substring(0, comment)).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "TITLE" -> { }
                case "DOMAIN_MIN" -> requireDomain(parts, 0.0F);
                case "DOMAIN_MAX" -> requireDomain(parts, 1.0F);
                case "LUT_3D_SIZE" -> {
                    if (edge != 0 || parts.length != 2) throw new IllegalArgumentException("Invalid LUT_3D_SIZE");
                    edge = Integer.parseInt(parts[1]);
                    if (edge < 2 || edge > 32) throw new IllegalArgumentException("LUT_3D_SIZE must be between 2 and 32");
                    values = BufferUtils.createFloatBuffer(edge * edge * edge * 3);
                }
                default -> {
                    if (values == null || parts.length != 3 || values.remaining() < 3)
                        throw new IllegalArgumentException("Invalid LUT sample count");
                    for (String part : parts) {
                        float value = Float.parseFloat(part);
                        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F)
                            throw new IllegalArgumentException("LUT samples must be in the range 0 to 1");
                        values.put(value);
                    }
                    samples++;
                }
            }
        }
        if (values == null || samples != edge * edge * edge) throw new IllegalArgumentException("Incomplete LUT data");
        values.flip();
        return new Lattice(edge, values);
    }

    private static void requireDomain(String[] parts, float expected) {
        if (parts.length != 4) throw new IllegalArgumentException("Invalid LUT domain");
        for (int i = 1; i < parts.length; i++) {
            if (Float.parseFloat(parts[i]) != expected) throw new IllegalArgumentException("LUT domain must be 0 to 1");
        }
    }

    private void upload(Lattice lattice) {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int allocated = GL11.glGenTextures();
        try {
            RenderSystem.bindTexture(allocated);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGB16F, lattice.size * lattice.size, lattice.size,
                    0, GL11.GL_RGB, GL11.GL_FLOAT, lattice.values);
            if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new IllegalArgumentException("GPU rejected the LUT texture");
            texture = allocated;
            size = lattice.size;
        } finally {
            RenderSystem.bindTexture(previous);
            if (texture != allocated) GL11.glDeleteTextures(allocated);
        }
    }

    int texture() { return texture; }
    int size() { return size; }
    String status() { return status; }

    @Override public void close() {
        if (texture != 0) GL11.glDeleteTextures(texture);
        texture = 0;
        size = 0;
        loaded = false;
        status = "Not requested";
    }

    private record Lattice(int size, FloatBuffer values) { }
}
