package dev.taste.engine.material;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import dev.taste.engine.material.MaterialDescription.TextureFormat;
import dev.taste.engine.material.SpriteAnimationClock.Step;
import dev.taste.engine.material.SpriteAnimationClock.Timeline;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Owns linear companion textures aligned to the active block atlas. */
public final class GpuCompanionAtlas implements AutoCloseable {
    public record Request(TextureAtlasSprite sprite, MaterialDescription material, TextureFormat configuredFormat,
            TextureFormat declaredFormat) {
        public Request {
            Objects.requireNonNull(sprite);
            Objects.requireNonNull(material);
        }
    }

    public record Availability(boolean normal, boolean specular, boolean height) {
        public Availability(boolean normal, boolean specular) { this(normal, specular, false); }
    }

    private record Layout(int baseTexture, int width, int height, int mipLevel, long textureBytes) { }
    private record Signature(TextureFormat format, ResourceLocation normal, ResourceLocation specular, ResourceLocation height) { }
    private record Entry(TextureAtlasSprite sprite, Signature signature, Availability availability,
            Map<Channel, AnimatedImage> animated, boolean conflict) { }
    private record ImageSize(int width, int height) { }
    private record Uploaded(boolean available, AnimatedImage animated) {
        private static final Uploaded NONE = new Uploaded(false, null);
    }

    private enum Channel {
        NORMAL, SPECULAR, HEIGHT;

        long bytes(Layout layout) { return this == HEIGHT ? layout.textureBytes() / 4 : layout.textureBytes(); }
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Availability NONE = new Availability(false, false, false);
    private static final long DEFAULT_BUDGET = 192L * 1024 * 1024;
    private static final long MAX_SPRITE_PIXELS = 4L * 1024 * 1024;
    private static final long MAX_ANIMATED_BYTES = 48L * 1024 * 1024;
    private static final long PNG_SIGNATURE = 0x89504E470D0A1A0AL;
    private static final int IHDR = 0x49484452;
    private final CompanionAtlas companions = new CompanionAtlas();
    private final Map<ResourceLocation, Entry> entries = new HashMap<>();
    private ResourceManager resources;
    private TextureAtlas baseAtlas;
    private Layout layout;
    private final int[] textures = new int[Channel.values().length];
    private final boolean[] denied = new boolean[Channel.values().length];
    private long animatedBytes;
    private long memoryBudget = -1;
    private long generation;

    public int normalTexture() {
        return textures[Channel.NORMAL.ordinal()];
    }

    public int specularTexture() {
        return textures[Channel.SPECULAR.ordinal()];
    }

    public int heightTexture() {
        return textures[Channel.HEIGHT.ordinal()];
    }

    public long allocatedBytes() {
        if (layout == null) return 0;
        long bytes = 0;
        for (Channel channel : Channel.values()) {
            if (textures[channel.ordinal()] != 0) bytes += channel.bytes(layout);
        }
        return bytes;
    }

    public long generation() {
        return generation;
    }

    public Availability availability(TextureAtlasSprite sprite) {
        Entry entry = entries.get(sprite.contents().name());
        return entry != null && entry.sprite() == sprite ? entry.availability() : NONE;
    }

    public void prepare(ResourceManager activeResources, TextureAtlas activeAtlas, Collection<Request> requests) {
        prepare(activeResources, activeAtlas, requests, DEFAULT_BUDGET);
    }

    public void prepare(ResourceManager activeResources, TextureAtlas activeAtlas, Collection<Request> requests,
            long budgetBytes) {
        RenderSystem.assertOnRenderThread();
        int baseTexture = activeAtlas.getId();
        if (resources != activeResources || baseAtlas != activeAtlas || (layout != null && layout.baseTexture() != baseTexture)
                || allocatedBytes() > budgetBytes || (memoryBudget != budgetBytes && budgetWasDenied())) {
            close();
            resources = activeResources;
            baseAtlas = activeAtlas;
        }
        memoryBudget = budgetBytes;
        if (requests.isEmpty() || budgetBytes <= 0) return;
        try (UploadBatch batch = new UploadBatch()) {
            if (layout == null) {
                if (requests.stream().noneMatch(request -> format(request) != TextureFormat.PROCEDURAL
                        || request.material().metadata().heightMap() != null)) return;
                batch.begin();
                layout = readLayout(baseTexture);
            }
            if (layout == null) return;
            for (Request request : requests) prepareSprite(activeResources, activeAtlas, request, budgetBytes, batch);
        }
    }

    private static TextureFormat format(Request request) {
        return request.material().metadata().effectiveTextureFormat(request.configuredFormat(), request.declaredFormat());
    }

    private void prepareSprite(ResourceManager resources, TextureAtlas activeAtlas, Request request, long budgetBytes,
            UploadBatch batch) {
        TextureAtlasSprite sprite = request.sprite();
        ResourceLocation name = sprite.contents().name();
        if (activeAtlas.getSprite(name) != sprite) return;
        TextureFormat format = format(request);
        if (format == TextureFormat.PROCEDURAL && request.material().metadata().heightMap() == null) return;

        CompanionAtlas.Companions resolved = companions.resolve(
                resources, name, request.material(), request.configuredFormat(), request.declaredFormat());
        Signature signature = new Signature(format,
                format == TextureFormat.PROCEDURAL ? null : resolved.normal().orElse(null),
                format == TextureFormat.PROCEDURAL ? null : resolved.specular().orElse(null),
                resolved.height().orElse(null));
        Entry existing = entries.get(name);
        if (existing != null) {
            if (!existing.conflict() && !existing.signature().equals(signature)) {
                LOGGER.warn("Taste disabled companions for {} because material rules request different maps or formats", name);
                releaseAnimations(existing);
                entries.put(name, new Entry(sprite, signature, NONE, Map.of(), true));
                if (existing.availability().normal() || existing.availability().specular() || existing.availability().height()) {
                    generation++;
                }
            } else if (!existing.animated().isEmpty()) {
                refreshAnimations(name, existing, batch);
            }
            return;
        }
        if (signature.normal() == null && signature.specular() == null && signature.height() == null) {
            entries.put(name, new Entry(sprite, signature, NONE, Map.of(), false));
            return;
        }
        if (!validPlacement(sprite)) {
            LOGGER.warn("Taste skipped companions for {} because its base sprite has an unsafe mip rectangle", name);
            entries.put(name, new Entry(sprite, signature, NONE, Map.of(), false));
            return;
        }

        batch.begin();
        Timeline timeline = SpriteAnimationClock.timeline(sprite);
        Uploaded normal = upload(resources, sprite, signature.normal(), format, Channel.NORMAL, budgetBytes, timeline);
        Uploaded specular = upload(resources, sprite, signature.specular(), format, Channel.SPECULAR, budgetBytes, timeline);
        Uploaded height = upload(resources, sprite, signature.height(), format, Channel.HEIGHT, budgetBytes, timeline);
        EnumMap<Channel, AnimatedImage> animated = new EnumMap<>(Channel.class);
        if (normal.animated() != null) animated.put(Channel.NORMAL, normal.animated());
        if (specular.animated() != null) animated.put(Channel.SPECULAR, specular.animated());
        if (height.animated() != null) animated.put(Channel.HEIGHT, height.animated());
        entries.put(name, new Entry(sprite, signature, new Availability(normal.available(), specular.available(),
                height.available()), Map.copyOf(animated), false));
    }

    private void refreshAnimations(ResourceLocation name, Entry entry, UploadBatch batch) {
        EnumMap<Channel, AnimatedImage> retained = null;
        for (var channel : entry.animated().entrySet()) {
            AnimatedImage animation = channel.getValue();
            Step step = SpriteAnimationClock.step(entry.sprite(), animation.timeline);
            if (step != null && step.equals(animation.previous)) continue;
            try {
                if (step == null) throw new IOException("native animation ticker is absent");
                batch.begin();
                animation.upload(textures[channel.getKey().ordinal()], entry.sprite(), step);
            } catch (IOException | RuntimeException failure) {
                LOGGER.warn("Taste disabled animated {} companion for {}: {}", channel.getKey(), name, failure.getMessage());
                if (retained == null) {
                    retained = new EnumMap<>(Channel.class);
                    retained.putAll(entry.animated());
                }
                retained.remove(channel.getKey());
                animatedBytes -= animation.bytes;
                animation.close();
            }
        }
        if (retained != null) {
            Availability old = entry.availability();
            entries.put(name, new Entry(entry.sprite(), entry.signature(), new Availability(
                    old.normal() && (!entry.animated().containsKey(Channel.NORMAL) || retained.containsKey(Channel.NORMAL)),
                    old.specular() && (!entry.animated().containsKey(Channel.SPECULAR) || retained.containsKey(Channel.SPECULAR)),
                    old.height() && (!entry.animated().containsKey(Channel.HEIGHT) || retained.containsKey(Channel.HEIGHT))),
                    Map.copyOf(retained), false));
            generation++;
        }
    }

    private boolean validPlacement(TextureAtlasSprite sprite) {
        SpriteContents contents = sprite.contents();
        int width = contents.width();
        int height = contents.height();
        int step = 1 << layout.mipLevel();
        return (long) width * height <= MAX_SPRITE_PIXELS
                && sprite.getX() >= 0 && sprite.getY() >= 0
                && (long) sprite.getX() + width <= layout.width()
                && (long) sprite.getY() + height <= layout.height()
                && sprite.getX() % step == 0 && sprite.getY() % step == 0
                && width % step == 0 && height % step == 0;
    }

    private Uploaded upload(ResourceManager resources, TextureAtlasSprite sprite, ResourceLocation location,
            TextureFormat format, Channel channel, long budgetBytes, Timeline timeline) {
        if (location == null) return Uploaded.NONE;
        try {
            Resource resource = resources.getResource(location).orElseThrow(() -> new IOException("resource is absent"));
            var metadata = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER);
            int width = sprite.contents().width();
            int height = sprite.contents().height();
            try (BufferedInputStream stream = new BufferedInputStream(resource.open())) {
                ImageSize image = pngDimensions(stream);
                boolean animated = image.width() != width || image.height() != height;
                if (animated && (timeline == null || image.width() != timeline.sheetWidth()
                        || image.height() != timeline.sheetHeight())) {
                    throw new IOException("image does not match the native sprite frame or animation sheet");
                }
                if (metadata.isPresent()) {
                    AnimationMetadataSection declared = metadata.get();
                    var frame = declared.calculateFrameSize(image.width(), image.height());
                    Timeline supplied = SpriteAnimationClock.describe(declared, image.width(), image.height(),
                            frame.width(), frame.height());
                    if (supplied == null || frame.width() != width || frame.height() != height
                            || (animated && !supplied.equals(timeline))
                            || (!animated && supplied.frames().stream().anyMatch(item -> item.index() != 0))) {
                        throw new IOException("declared animation does not match the native sprite timeline");
                    }
                }
                try (NativeImage base = NativeImage.read(NativeImage.Format.RGBA, stream)) {
                    if (base.getWidth() != image.width() || base.getHeight() != image.height()) {
                        throw new IOException("decoded dimensions differ from the PNG header");
                    }
                    if (!animated) {
                        int texture = texture(channel, budgetBytes);
                        if (texture == 0) return Uploaded.NONE;
                        uploadMipChain(texture, sprite, base, format, channel);
                        return new Uploaded(true, null);
                    }
                    Step step = SpriteAnimationClock.step(sprite, timeline);
                    if (step == null) throw new IOException("native animation ticker is absent");
                    long bytes = animationBytes(timeline);
                    if (animatedBytes + bytes > MAX_ANIMATED_BYTES) {
                        throw new IOException("animated companion CPU image budget exceeded");
                    }
                    int texture = texture(channel, budgetBytes);
                    if (texture == 0) return Uploaded.NONE;
                    AnimatedImage animation = new AnimatedImage(base, timeline, format, channel, bytes);
                    try {
                        animation.upload(texture, sprite, step);
                    } catch (IOException | RuntimeException failure) {
                        animation.close();
                        throw failure;
                    }
                    animatedBytes += bytes;
                    return new Uploaded(true, animation);
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Taste skipped companion {} for {}: {}", location, sprite.contents().name(), exception.getMessage());
            return Uploaded.NONE;
        }
    }

    private long animationBytes(Timeline timeline) {
        long mipBytes = 0;
        for (int mip = 0; mip <= layout.mipLevel(); mip++) {
            mipBytes += (long) (timeline.frameWidth() >> mip) * (timeline.frameHeight() >> mip) * 4;
        }
        long count = timeline.frames().stream().mapToInt(SpriteAnimationClock.Frame::index).distinct().count();
        return mipBytes * (count + (timeline.interpolate() ? 1 : 0));
    }

    private static ImageSize pngDimensions(BufferedInputStream stream) throws IOException {
        stream.mark(24);
        DataInputStream header = new DataInputStream(stream);
        if (header.readLong() != PNG_SIGNATURE || header.readInt() != 13 || header.readInt() != IHDR) {
            throw new IOException("expected a PNG image");
        }
        int width = header.readInt();
        int height = header.readInt();
        if (width < 1 || height < 1 || (long) width * height > MAX_SPRITE_PIXELS) {
            throw new IOException("PNG dimensions exceed the companion image budget");
        }
        stream.reset();
        return new ImageSize(width, height);
    }

    private boolean budgetWasDenied() {
        for (boolean channel : denied) if (channel) return true;
        return false;
    }

    private int texture(Channel channel, long budgetBytes) throws IOException {
        int index = channel.ordinal();
        int current = textures[index];
        if (current != 0) return current;
        if (denied[index]) return 0;
        if (allocatedBytes() + channel.bytes(layout) > budgetBytes) {
            denied[index] = true;
            throw new IOException("companion atlas exceeds its GPU memory budget");
        }

        int texture = TextureUtil.generateTextureId();
        try {
            RenderSystem.bindTexture(texture);
            boolean height = channel == Channel.HEIGHT;
            for (int mip = 0; mip <= layout.mipLevel(); mip++) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, mip, height ? GL30.GL_R8 : GL11.GL_RGBA8,
                        Math.max(1, layout.width() >> mip), Math.max(1, layout.height() >> mip), 0,
                        height ? GL11.GL_RED : GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            }
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, layout.mipLevel());
            filters(layout.mipLevel() > 0);
            requireNoError("companion atlas allocation");
        } catch (IOException | RuntimeException failure) {
            TextureUtil.releaseTextureId(texture);
            denied[index] = true;
            throw failure;
        }
        textures[index] = texture;
        return texture;
    }

    private void uploadMipChain(int texture, TextureAtlasSprite sprite, NativeImage source, TextureFormat format,
            Channel channel) throws IOException {
        RenderSystem.bindTexture(texture);
        NativeImage image = source;
        try {
            for (int mip = 0; mip <= layout.mipLevel(); mip++) {
                uploadMip(image, sprite, mip);
                if (mip < layout.mipLevel()) {
                    NativeImage next = reduce(image, format, channel);
                    if (image != source) image.close();
                    image = next;
                }
            }
            finishUpload();
        } finally {
            if (image != source) image.close();
        }
    }

    private void uploadMip(NativeImage image, TextureAtlasSprite sprite, int mip) {
        image.upload(mip, sprite.getX() >> mip, sprite.getY() >> mip, 0, 0,
                image.getWidth(), image.getHeight(), false, true, layout.mipLevel() > 0, false);
    }

    private void finishUpload() throws IOException {
        filters(layout.mipLevel() > 0);
        requireNoError("companion sprite upload");
    }

    private void releaseAnimations(Entry entry) {
        for (AnimatedImage animation : entry.animated().values()) {
            animatedBytes -= animation.bytes;
            animation.close();
        }
    }
    private final class AnimatedImage implements AutoCloseable {
        private final Timeline timeline;
        private final TextureFormat format;
        private final Channel channel;
        private final long bytes;
        private final Map<Integer, NativeImage[]> frames = new HashMap<>();
        private final NativeImage[] blend;
        private Step previous;

        private AnimatedImage(NativeImage sheet, Timeline timeline, TextureFormat format, Channel channel, long bytes) {
            this.timeline = timeline;
            this.format = format;
            this.channel = channel;
            this.bytes = bytes;
            blend = timeline.interpolate() ? new NativeImage[layout.mipLevel() + 1] : null;
            try {
                for (var frame : timeline.frames()) {
                    frames.computeIfAbsent(frame.index(), index -> bakeFrame(sheet, timeline, index, format, channel));
                }
                if (blend != null) {
                    for (int mip = 0; mip < blend.length; mip++) {
                        blend[mip] = new NativeImage(NativeImage.Format.RGBA,
                                timeline.frameWidth() >> mip, timeline.frameHeight() >> mip, false);
                    }
                }
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }

        private void upload(int texture, TextureAtlasSprite sprite, Step step) throws IOException {
            RenderSystem.bindTexture(texture);
            NativeImage[] active = frames.get(step.index());
            NativeImage[] next = step.subFrame() == 0 ? null : frames.get(step.next());
            for (int mip = 0; mip < active.length; mip++) {
                NativeImage image = active[mip];
                if (next != null) {
                    double amount = (double) step.subFrame() / step.frameTicks();
                    blendFrames(image, next[mip], blend[mip], amount, format, channel);
                    image = blend[mip];
                }
                uploadMip(image, sprite, mip);
            }
            finishUpload();
            previous = step;
        }

        @Override public void close() {
            frames.values().forEach(GpuCompanionAtlas::closeImages);
            frames.clear();
            closeImages(blend);
        }
    }

    private NativeImage[] bakeFrame(NativeImage sheet, Timeline timeline, int index, TextureFormat format,
            Channel channel) {
        NativeImage[] images = new NativeImage[layout.mipLevel() + 1];
        try {
            int width = timeline.frameWidth();
            int height = timeline.frameHeight();
            int columns = timeline.sheetWidth() / width;
            int sourceX = index % columns * width;
            int sourceY = index / columns * height;
            images[0] = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    images[0].setPixelRGBA(x, y, sheet.getPixelRGBA(sourceX + x, sourceY + y));
                }
            }
            for (int mip = 1; mip < images.length; mip++) images[mip] = reduce(images[mip - 1], format, channel);
            return images;
        } catch (RuntimeException | Error failure) {
            closeImages(images);
            throw failure;
        }
    }

    private static void closeImages(NativeImage[] images) {
        if (images == null) return;
        for (NativeImage image : images) if (image != null) image.close();
    }

    private static void blendFrames(NativeImage first, NativeImage second, NativeImage out, double amount,
            TextureFormat format, Channel channel) {
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int a = first.getPixelRGBA(x, y);
                int b = second.getPixelRGBA(x, y);
                int pixel = switch (channel) {
                    case NORMAL -> blendNormals(a, b, amount, format == TextureFormat.LABPBR);
                    case SPECULAR -> blendSpecular(a, b, amount, format);
                    case HEIGHT -> rgba(lerp(channel(a, 0), channel(b, 0), amount), 0, 0, 255);
                };
                out.setPixelRGBA(x, y, pixel);
            }
        }
    }

    private static int blendNormals(int a, int b, double amount, boolean lab) {
        double ax = channel(a, 0) / 127.5 - 1, ay = channel(a, 8) / 127.5 - 1;
        double bx = channel(b, 0) / 127.5 - 1, by = channel(b, 8) / 127.5 - 1;
        double az = lab ? Math.sqrt(Math.max(0, 1 - ax * ax - ay * ay)) : channel(a, 16) / 127.5 - 1;
        double bz = lab ? Math.sqrt(Math.max(0, 1 - bx * bx - by * by)) : channel(b, 16) / 127.5 - 1;
        double first = Math.max(1.0E-8, Math.sqrt(ax * ax + ay * ay + az * az));
        double second = Math.max(1.0E-8, Math.sqrt(bx * bx + by * by + bz * bz));
        double x = ax / first * (1 - amount) + bx / second * amount;
        double y = ay / first * (1 - amount) + by / second * amount;
        double z = az / first * (1 - amount) + bz / second * amount;
        double length = Math.sqrt(x * x + y * y + z * z);
        int blue = lab ? lerp(channel(a, 16), channel(b, 16), amount) : length < 1.0E-8 ? 255 : encodedNormal(z / length);
        return rgba(length < 1.0E-8 ? 128 : encodedNormal(x / length),
                length < 1.0E-8 ? 128 : encodedNormal(y / length), blue, lerp(channel(a, 24), channel(b, 24), amount));
    }

    private static int blendSpecular(int a, int b, double amount, TextureFormat format) {
        int red = lerp(channel(a, 0), channel(b, 0), amount);
        int ag = channel(a, 8), bg = channel(b, 8);
        int ab = channel(a, 16), bb = channel(b, 16);
        int aa = channel(a, 24), ba = channel(b, 24);
        if (format == TextureFormat.OLDPBR) return rgba(red, lerp(ag, bg, amount), lerp(ab, bb, amount), lerp(aa, ba, amount));
        int green = ag < 230 && bg < 230 ? lerp(ag, bg, amount) : amount < 0.5 ? ag : bg;
        int blue = (ab < 65) == (bb < 65) ? lerp(ab, bb, amount) : amount < 0.5 ? ab : bb;
        int alpha = aa == 255 && ba == 255 ? 255 : lerp(aa == 255 ? 0 : aa, ba == 255 ? 0 : ba, amount);
        return rgba(red, green, blue, alpha);
    }

    private static int lerp(int a, int b, double amount) {
        return (int) Math.round(a + (b - a) * amount);
    }

    private static NativeImage reduce(NativeImage image, TextureFormat format, Channel channel) {
        NativeImage next = new NativeImage(NativeImage.Format.RGBA, image.getWidth() / 2, image.getHeight() / 2, false);
        try {
            int[] pixels = new int[4];
            for (int y = 0; y < next.getHeight(); y++) {
                for (int x = 0; x < next.getWidth(); x++) {
                    pixels[0] = image.getPixelRGBA(x * 2, y * 2);
                    pixels[1] = image.getPixelRGBA(x * 2 + 1, y * 2);
                    pixels[2] = image.getPixelRGBA(x * 2, y * 2 + 1);
                    pixels[3] = image.getPixelRGBA(x * 2 + 1, y * 2 + 1);
                    int pixel = switch (channel) {
                        case NORMAL -> normalMip(pixels, format);
                        case SPECULAR -> specularMip(pixels, format);
                        case HEIGHT -> rgba(average(pixels, 0), 0, 0, 255);
                    };
                    next.setPixelRGBA(x, y, pixel);
                }
            }
            return next;
        } catch (RuntimeException | Error failure) {
            next.close();
            throw failure;
        }
    }

    private static int normalMip(int[] pixels, TextureFormat format) {
        boolean lab = format == TextureFormat.LABPBR;
        double x = 0;
        double y = 0;
        double z = 0;
        for (int pixel : pixels) {
            double nx = channel(pixel, 0) / 127.5 - 1.0;
            double ny = channel(pixel, 8) / 127.5 - 1.0;
            double nz = lab ? Math.sqrt(Math.max(0.0, 1.0 - nx * nx - ny * ny)) : channel(pixel, 16) / 127.5 - 1.0;
            double magnitude = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (magnitude < 1.0E-8) {
                z++;
                continue;
            }
            x += nx / magnitude;
            y += ny / magnitude;
            z += nz / magnitude;
        }
        double length = Math.sqrt(x * x + y * y + z * z);
        int aoOrZ = lab ? average(pixels, 16) : length < 1.0E-8 ? 255 : encodedNormal(z / length);
        return rgba(length < 1.0E-8 ? 128 : encodedNormal(x / length),
                length < 1.0E-8 ? 128 : encodedNormal(y / length), aoOrZ, average(pixels, 24));
    }

    private static int specularMip(int[] pixels, TextureFormat format) {
        int red = average(pixels, 0);
        if (format == TextureFormat.OLDPBR) {
            return rgba(red, average(pixels, 8), average(pixels, 16), average(pixels, 24));
        }
        int green = dominant(pixels, 8, 230, true);
        int blue = dominant(pixels, 16, 65, false);
        int emission = 0;
        int emittedSamples = 0;
        for (int pixel : pixels) {
            int alpha = channel(pixel, 24);
            if (alpha != 255) {
                emission += alpha;
                emittedSamples++;
            }
        }
        return rgba(red, green, blue, emittedSamples == 0 ? 255 : (emission + 2) / 4);
    }

    private static int dominant(int[] pixels, int shift, int threshold, boolean distinctUpperValues) {
        int winner = -1;
        int bestCount = 0;
        int average = 0;
        for (int candidate : pixels) {
            int category = category(channel(candidate, shift), threshold, distinctUpperValues);
            int count = 0;
            int sum = 0;
            for (int pixel : pixels) {
                int value = channel(pixel, shift);
                if (category(value, threshold, distinctUpperValues) == category) {
                    count++;
                    sum += value;
                }
            }
            if (count > bestCount) {
                winner = category;
                bestCount = count;
                average = (sum + count / 2) / count;
            }
        }
        return distinctUpperValues && winner >= threshold ? winner : average;
    }

    private static int category(int value, int threshold, boolean distinctUpperValues) {
        if (value < threshold) return 0;
        return distinctUpperValues ? value : threshold;
    }

    private static int average(int[] pixels, int shift) {
        int sum = 0;
        for (int pixel : pixels) sum += channel(pixel, shift);
        return (sum + 2) / 4;
    }

    private static int encodedNormal(double value) {
        return (int) Math.round((Math.max(-1, Math.min(1, value)) + 1) * 127.5);
    }

    private static int channel(int pixel, int shift) {
        return (pixel >>> shift) & 255;
    }

    private static int rgba(int red, int green, int blue, int alpha) {
        return red | green << 8 | blue << 16 | alpha << 24;
    }

    private static Layout readLayout(int baseTexture) {
        RenderSystem.bindTexture(baseTexture);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (width <= 0 || height <= 0) return null;
        int maximum = Math.min(12, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL));
        int level = 0;
        long bytes = (long) width * height * 4;
        while (level < maximum) {
            int mipWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level + 1, GL11.GL_TEXTURE_WIDTH);
            int mipHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level + 1, GL11.GL_TEXTURE_HEIGHT);
            if (mipWidth != Math.max(1, width >> (level + 1)) || mipHeight != Math.max(1, height >> (level + 1))) break;
            bytes += (long) mipWidth * mipHeight * 4;
            level++;
        }
        return new Layout(baseTexture, width, height, level, bytes);
    }

    private static void filters(boolean mips) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                mips ? GL11.GL_NEAREST_MIPMAP_NEAREST : GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private static void requireNoError(String operation) throws IOException {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) throw new IOException(operation + " raised GL error 0x" + Integer.toHexString(error));
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (allocatedBytes() != 0 || !entries.isEmpty()) generation++;
        for (int texture : textures) if (texture != 0) TextureUtil.releaseTextureId(texture);
        java.util.Arrays.fill(textures, 0);
        java.util.Arrays.fill(denied, false);
        memoryBudget = -1;
        entries.values().forEach(this::releaseAnimations);
        animatedBytes = 0;
        entries.clear();
        companions.clear();
        resources = null;
        baseAtlas = null;
        layout = null;
    }

    private static final class UnpackState implements AutoCloseable {
        private final int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        private final int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        private final int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        private final int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        private final int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        private final int buffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);

        UnpackState() {
            if (buffer != 0) GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        }

        @Override
        public void close() {
            RenderSystem.bindTexture(texture);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            if (buffer != 0) GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        }
    }

    private static final class UploadBatch implements AutoCloseable {
        private UnpackState state;

        void begin() {
            if (state == null) state = new UnpackState();
        }

        @Override
        public void close() {
            if (state != null) state.close();
        }
    }
}
