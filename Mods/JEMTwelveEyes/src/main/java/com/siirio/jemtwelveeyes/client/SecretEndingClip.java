package com.siirio.jemtwelveeyes.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Plays the user's authored frames with one reusable GPU texture and bounded decoding. */
final class SecretEndingClip implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretEndingClip.class);
    private static final int WIDTH = 2048;
    private static final int HEIGHT = 1080;
    private static final int FRAMES = 75;
    private static final int FPS = 24;

    private final ArrayBlockingQueue<Frame> ready = new ArrayBlockingQueue<>(4);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread decoder;
    private DynamicTexture texture;
    private ResourceLocation textureId;
    private volatile boolean failed;

    SecretEndingClip(ResourceManager resources) {
        decoder = new Thread(() -> decode(resources), "jem-secret-ending-frames");
        decoder.setDaemon(true);
        decoder.start();
    }

    void render(GuiGraphics graphics, int screenWidth, int screenHeight, long elapsedNanos) {
        graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
        int requested = Math.min(FRAMES - 1, (int) (elapsedNanos * FPS / 1_000_000_000L));
        Frame newest = null;
        Frame next;
        while ((next = ready.peek()) != null && next.index() <= requested) {
            next = ready.poll();
            if (next == null) break;
            if (newest != null) newest.image().close();
            newest = next;
        }
        if (newest != null) {
            if (texture == null) {
                texture = new DynamicTexture(newest.image());
                textureId = Minecraft.getInstance().getTextureManager().register("jem_secret_ending", texture);
            } else {
                texture.setPixels(newest.image());
                texture.upload();
            }
        }
        if (textureId != null) {
            float scale = Math.min(screenWidth / (float) WIDTH, screenHeight / (float) HEIGHT);
            graphics.pose().pushPose();
            graphics.pose().translate((screenWidth - WIDTH * scale) / 2.0F,
                    (screenHeight - HEIGHT * scale) / 2.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.blit(textureId, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
            graphics.pose().popPose();
        } else if (failed) {
            graphics.drawCenteredString(Minecraft.getInstance().font,
                    "Cutscene frames could not load. See latest.log", screenWidth / 2, screenHeight / 2, 0xFFFF5555);
        }
    }

    private void decode(ResourceManager resources) {
        try {
            for (int index = 0; index < FRAMES && !closed.get(); index++) {
                ResourceLocation path = new ResourceLocation("jem_twelve_eyes",
                        "textures/secret_ending/frame_%03d.png".formatted(index));
                NativeImage image;
                try (InputStream stream = resources.getResource(path)
                        .orElseThrow(() -> new IOException("Missing frame " + path)).open()) {
                    image = NativeImage.read(stream);
                }
                if (image.getWidth() != WIDTH || image.getHeight() != HEIGHT) {
                    image.close();
                    throw new IOException("Invalid frame dimensions: " + path);
                }
                Frame frame = new Frame(index, image);
                boolean enqueued = false;
                try {
                    while (!closed.get() && !(enqueued = ready.offer(frame, 100, TimeUnit.MILLISECONDS))) { }
                } finally {
                    if (!enqueued || (closed.get() && ready.remove(frame))) image.close();
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException failure) {
            failed = true;
            LOGGER.error("Secret ending clip could not load", failure);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        decoder.interrupt();
        Frame frame;
        while ((frame = ready.poll()) != null) frame.image().close();
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
        texture = null;
        textureId = null;
    }

    private record Frame(int index, NativeImage image) { }
}
