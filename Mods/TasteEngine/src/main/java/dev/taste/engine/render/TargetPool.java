package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Render-thread target owner for the portable RGBA8/depth path. */
public final class TargetPool implements AutoCloseable {
    private record Entry(TargetDescriptor descriptor, TextureTarget target) { }
    private final MemoryBudget budget;
    private final Map<String, Entry> targets = new LinkedHashMap<>();

    public TargetPool(MemoryBudget budget) { this.budget = budget; }

    public TextureTarget acquire(TargetDescriptor descriptor) {
        RenderSystem.assertOnRenderThread();
        if (descriptor.format() != TargetDescriptor.Format.RGBA8
                && descriptor.format() != TargetDescriptor.Format.RGBA16F
                && descriptor.format() != TargetDescriptor.Format.RGBA32F)
            throw new IllegalArgumentException("Unsupported portable target format " + descriptor.format());
        Entry current = targets.get(descriptor.name());
        if (current != null && current.descriptor().equals(descriptor)) return current.target();
        budget.stage(descriptor.name(), descriptor.byteSize());
        TextureTarget replacement = null;
        try {
            replacement = new TextureTarget(descriptor.width(), descriptor.height(), descriptor.depth(), Minecraft.ON_OSX);
            if (descriptor.format() != TargetDescriptor.Format.RGBA8)
                allocateFloatColor(replacement, descriptor.format());
            replacement.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            if (current != null) current.target().destroyBuffers();
            targets.put(descriptor.name(), new Entry(descriptor, replacement));
            budget.commit();
            return replacement;
        } catch (RuntimeException failure) {
            budget.rollback();
            if (replacement != null) replacement.destroyBuffers();
            throw failure;
        }
    }

    public TextureTarget get(String name) {
        Entry entry = targets.get(name);
        if (entry == null) throw new IllegalArgumentException("Unknown target " + name);
        return entry.target();
    }

    public void release(String name) {
        RenderSystem.assertOnRenderThread();
        Entry entry = targets.remove(name);
        if (entry != null) entry.target().destroyBuffers();
        budget.release(name);
    }

    public long allocatedBytes() { return budget.activeBytes(); }
    public long limitBytes() { return budget.limitBytes(); }
    public long externalBytes() { return budget.externalBytes(); }
    public void accountExternal(long bytes) { budget.accountExternal(bytes); }
    public Map<String, Long> allocations() { return budget.activeAllocations(); }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        targets.values().forEach(entry -> entry.target().destroyBuffers());
        targets.clear();
        budget.clear();
    }

    private static void allocateFloatColor(TextureTarget target, TargetDescriptor.Format format) {
        int priorTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int priorFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, target.getColorTextureId());
            int storage = format == TargetDescriptor.Format.RGBA32F ? GL30.GL_RGBA32F : GL30.GL_RGBA16F;
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, storage, target.width, target.height, 0,
                    GL11.GL_RGBA, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException(format + " target is incomplete");
        } finally {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, priorFramebuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, priorTexture);
        }
    }
}
