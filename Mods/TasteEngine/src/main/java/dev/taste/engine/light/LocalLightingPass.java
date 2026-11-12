package dev.taste.engine.light;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.taste.engine.api.LightProvider.Light;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Bounded loaded-section discovery and packed screen tile lists for local light sources. */
public final class LocalLightingPass implements AutoCloseable {
    public record TileData(int texture, int columns, List<Light> lights) { }
    private record Section(int x, int y, int z, double distance) {
        long key() { return SectionPos.asLong(x, y, z); }
    }
    private record Cached(LevelChunkSection section, long refreshedTick, List<Light> lights) { }
    private static final int SCAN_PER_FRAME = 8;
    private static final int CACHE_LIMIT = 2048;
    private static final int TILE = 32;
    private static final int MAX_TILE_LIGHTS = 16;
    private static final int TEXTURE_WIDTH = 256;
    private static final int TILE_TEXELS = 5;
    private final Map<Long, Cached> sections = new LinkedHashMap<>(64, 0.75F, true);
    private final Map<BlockState, MaterialDescription> materials = new HashMap<>();
    private final TiledLightGrid grid = new TiledLightGrid(TILE, MAX_TILE_LIGHTS);
    private long worldEpoch = -1;
    private FloatBuffer buffer;

    public TileData prepare(TargetPool targets, FrameSnapshot frame, PipelineParameters settings, MaterialResolver resolver) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return null;
        if (worldEpoch != frame.worldEpoch()) {
            sections.clear();
            materials.clear();
            worldEpoch = frame.worldEpoch();
        }
        List<Light> candidates = new ArrayList<>();
        boolean blocks = enabled(settings, "lighting.coloredBlocks");
        boolean held = enabled(settings, "lighting.heldLights");
        if (!(blocks || held)) {
            targets.release("local-light-data");
            return null;
        }
        float blockRange = value(settings, "lighting.lightRange");
        if (blocks) {
            collectBlocks(level, frame, resolver, blockRange, candidates);
        }
        if (held && minecraft.player != null) {
            float range = value(settings, "lighting.heldRange");
            addHeld(minecraft.player.getMainHandItem(), frame.cameraWorld(), resolver, range, candidates);
            addHeld(minecraft.player.getOffhandItem(), frame.cameraWorld(), resolver, range, candidates);
        }
        if (candidates.isEmpty()) return null;
        FrameSnapshot.Vec3d eye = frame.cameraWorld();
        int limit = settings.value("lighting.maxLights").getAsInt();
        List<Light> selected = candidates.stream()
                .sorted(Comparator.comparingDouble((Light light) -> influence(light, eye)).reversed())
                .limit(limit).toList();
        List<TiledLightGrid.ProjectedLight> projected = project(frame, selected);
        if (projected.isEmpty()) return null;
        grid.build(frame.width(), frame.height(), projected);
        int texture = upload(targets, frame, settings, selected, limit);
        return new TileData(texture, grid.columns(), selected);
    }

    private void collectBlocks(ClientLevel level, FrameSnapshot frame, MaterialResolver resolver,
            float maximumRange, List<Light> output) {
        FrameSnapshot.Vec3d eye = frame.cameraWorld();
        double searchRange = maximumRange + 32.0;
        int x0 = SectionPos.blockToSectionCoord(eye.x() - searchRange);
        int x1 = SectionPos.blockToSectionCoord(eye.x() + searchRange);
        int y0 = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(eye.y() - searchRange));
        int y1 = Math.min(level.getMaxSection() - 1, SectionPos.blockToSectionCoord(eye.y() + searchRange));
        int z0 = SectionPos.blockToSectionCoord(eye.z() - searchRange);
        int z1 = SectionPos.blockToSectionCoord(eye.z() + searchRange);
        List<Section> candidates = new ArrayList<>();
        for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            double distance = distanceSquared(x * 16.0 + 8, y * 16.0 + 8, z * 16.0 + 8, eye);
            if (distance <= searchRange * searchRange) candidates.add(new Section(x, y, z, distance));
        }
        candidates.sort(Comparator.comparingDouble(Section::distance));
        int scanned = 0;
        long tick = level.getGameTime();
        for (Section position : candidates) {
            LevelChunk chunk = level.getChunkSource().getChunk(position.x(), position.z(), ChunkStatus.FULL, false);
            if (chunk == null) {
                sections.remove(position.key());
                continue;
            }
            LevelChunkSection section = chunk.getSections()[position.y() - level.getMinSection()];
            Cached cached = sections.get(position.key());
            if (cached == null || cached.section() != section || tick - cached.refreshedTick() >= 160) {
                if (scanned < SCAN_PER_FRAME) {
                    cached = new Cached(section, tick, scan(level, section, position, resolver));
                    sections.put(position.key(), cached);
                    scanned++;
                } else if (cached == null || cached.section() != section) continue;
            }
            for (Light light : cached.lights()) {
                if (distanceSquared(light.x(), light.y(), light.z(), eye) > searchRange * searchRange) continue;
                output.add(new Light(light.x(), light.y(), light.z(), light.red(), light.green(), light.blue(),
                        light.intensity(), Math.min(maximumRange, light.range()), light.shadow()));
            }
        }
        while (sections.size() > CACHE_LIMIT) sections.remove(sections.keySet().iterator().next());
    }

    private List<Light> scan(ClientLevel level, LevelChunkSection section, Section location, MaterialResolver resolver) {
        if (section.hasOnlyAir() || !section.maybeHas(state -> state.getLightEmission() > 0 || material(state, resolver).emission() > 0))
            return List.of();
        Light[] cells = new Light[64];
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            BlockState state = section.getBlockState(x, y, z);
            if (state.isAir()) continue;
            position.set(location.x() * 16 + x, location.y() * 16 + y, location.z() * 16 + z);
            MaterialDescription material = material(state, resolver);
            int nativeLight = state.getLightEmission(level, position);
            if (nativeLight <= 0 && material.emission() <= 0) continue;
            int cell = (x / 4) + (z / 4) * 4 + (y / 4) * 16;
            float intensity = Math.max(material.emission(), nativeLight / 15.0F);
            if (cells[cell] != null && cells[cell].intensity() >= intensity) continue;
            cells[cell] = lightFor(state, material, nativeLight, position.getX() + 0.5, position.getY() + 0.65,
                    position.getZ() + 0.5, Math.max(3.0F, nativeLight * 1.5F));
        }
        List<Light> output = new ArrayList<>(cells.length);
        for (Light light : cells) if (light != null) output.add(light);
        return List.copyOf(output);
    }

    private void addHeld(ItemStack item, FrameSnapshot.Vec3d eye, MaterialResolver resolver,
            float range, List<Light> candidates) {
        if (!(item.getItem() instanceof BlockItem block)) return;
        BlockState state = block.getBlock().defaultBlockState();
        MaterialDescription material = material(state, resolver);
        int nativeLight = state.getLightEmission();
        if (nativeLight <= 0 && material.emission() <= 0) return;
        candidates.add(lightFor(state, material, nativeLight, eye.x(), eye.y() - 0.3, eye.z(), range));
    }

    private MaterialDescription material(BlockState state, MaterialResolver resolver) {
        return materials.computeIfAbsent(state, key -> {
            ResourceLocation block = BuiltInRegistries.BLOCK.getKey(key.getBlock());
            ResourceLocation fluid = key.getFluidState().isEmpty() ? null : BuiltInRegistries.FLUID.getKey(key.getFluidState().getType());
            return resolver.resolve(new MaterialQuery(block, fluid, null,
                    key.getTags().map(TagKey::location).collect(Collectors.toSet()), false, false, -1));
        });
    }

    private static Light lightFor(BlockState state, MaterialDescription material, int nativeLight,
            double x, double y, double z, float range) {
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        int rgb = material.emission() > 0 && material.tintRgb() != 0xFFFFFF ? material.tintRgb() : switch (block.toString()) {
            case "minecraft:soul_fire", "minecraft:soul_torch", "minecraft:soul_wall_torch", "minecraft:soul_lantern",
                    "minecraft:soul_campfire" -> 0x46E3EF;
            case "minecraft:sea_lantern", "minecraft:beacon" -> 0xD7FBFF;
            case "minecraft:lava", "minecraft:magma_block" -> 0xFF8031;
            default -> 0xFFE2BA;
        };
        return new Light(x, y, z, linear(rgb >> 16), linear(rgb >> 8), linear(rgb),
                Math.max(material.emission(), nativeLight / 15.0F), range, true);
    }

    private static List<TiledLightGrid.ProjectedLight> project(FrameSnapshot frame, List<Light> lights) {
        Matrix4f view = new Matrix4f().set(frame.view().columnMajor());
        Matrix4f projection = new Matrix4f().set(frame.projection().columnMajor());
        List<TiledLightGrid.ProjectedLight> projected = new ArrayList<>();
        FrameSnapshot.Vec3d origin = frame.origin();
        for (int index = 0; index < lights.size(); index++) {
            Light light = lights.get(index);
            Vector3f point = view.transformPosition(new Vector3f((float) (light.x() - origin.x()),
                    (float) (light.y() - origin.y()), (float) (light.z() - origin.z())));
            float depth = -point.z;
            if (depth + light.range() <= 0.05F) continue;
            if (depth <= light.range() + 0.05F) {
                projected.add(new TiledLightGrid.ProjectedLight(index, frame.width() * 0.5F, frame.height() * 0.5F,
                        Math.max(frame.width(), frame.height()), (float) influence(light, frame.cameraWorld())));
                continue;
            }
            float px = (projection.m00() * point.x / depth - projection.m20()) * 0.5F + 0.5F;
            float py = (projection.m11() * point.y / depth - projection.m21()) * 0.5F + 0.5F;
            float sx = Math.abs(projection.m00()) * (1 + Math.abs(point.x) / depth) * frame.width();
            float sy = Math.abs(projection.m11()) * (1 + Math.abs(point.y) / depth) * frame.height();
            float radius = Math.max(sx, sy) * 0.5F * light.range() / (depth - light.range());
            px *= frame.width();
            py *= frame.height();
            if (px + radius < 0 || py + radius < 0 || px - radius >= frame.width() || py - radius >= frame.height()) continue;
            projected.add(new TiledLightGrid.ProjectedLight(index, px, py, radius, (float) influence(light, frame.cameraWorld())));
        }
        return projected;
    }

    private int upload(TargetPool targets, FrameSnapshot frame, PipelineParameters settings, List<Light> lights, int limit) {
        int used = lights.size() * 2 + grid.columns() * grid.rows() * TILE_TEXELS;
        int height = (used + TEXTURE_WIDTH - 1) / TEXTURE_WIDTH;
        int capacity = (limit * 2 + grid.columns() * grid.rows() * TILE_TEXELS + TEXTURE_WIDTH - 1) / TEXTURE_WIDTH;
        int floats = height * TEXTURE_WIDTH * 4;
        if (buffer == null || buffer.capacity() < floats) buffer = BufferUtils.createFloatBuffer(floats);
        buffer.clear();
        FrameSnapshot.Vec3d origin = frame.origin();
        float strength = value(settings, "lighting.flickerStrength");
        float speed = value(settings, "lighting.flickerSpeed");
        for (Light light : lights) {
            float wave = (float) Math.sin(frame.timeSeconds() * speed + light.x() * 0.71 + light.z() * 0.29);
            buffer.put((float) (light.x() - origin.x())).put((float) (light.y() - origin.y()))
                    .put((float) (light.z() - origin.z())).put(light.range());
            buffer.put(light.red()).put(light.green()).put(light.blue()).put(light.intensity() * (1 + wave * strength));
        }
        for (int y = 0; y < grid.rows(); y++) for (int x = 0; x < grid.columns(); x++) {
            List<Integer> tile = grid.tile(x, y);
            buffer.put(tile.size()).put(0).put(0).put(0);
            for (int index = 0; index < MAX_TILE_LIGHTS; index++) buffer.put(index < tile.size() ? tile.get(index) : -1);
        }
        while (buffer.position() < floats) buffer.put(0);
        buffer.flip();
        int texture = targets.acquire(new TargetDescriptor("local-light-data", TEXTURE_WIDTH, capacity,
                TargetDescriptor.Format.RGBA32F, false, 1, 1, 1)).getColorTextureId();
        RenderSystem.bindTexture(texture);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, TEXTURE_WIDTH, height, GL11.GL_RGBA, GL11.GL_FLOAT, buffer);
        return texture;
    }

    public void dirty(BlockPos position) { sections.remove(SectionPos.asLong(position)); }

    private static float linear(int channel) {
        float s = (channel & 255) / 255.0F;
        return s <= 0.04045F ? s / 12.92F : (float) Math.pow((s + 0.055F) / 1.055F, 2.4);
    }

    private static double influence(Light light, FrameSnapshot.Vec3d eye) {
        return light.intensity() * light.range() * light.range() / Math.max(1.0, distanceSquared(light.x(), light.y(), light.z(), eye));
    }

    private static double distanceSquared(double x, double y, double z, FrameSnapshot.Vec3d origin) {
        double dx = x - origin.x(), dy = y - origin.y(), dz = z - origin.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean enabled(PipelineParameters config, String key) { return config.value(key).getAsBoolean(); }
    private static float value(PipelineParameters config, String key) { return config.value(key).getAsFloat(); }

    @Override public void close() {
        buffer = null;
        sections.clear();
        materials.clear();
    }
}
