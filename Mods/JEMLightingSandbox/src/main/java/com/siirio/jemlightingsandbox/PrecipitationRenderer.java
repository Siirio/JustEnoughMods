package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

public final class PrecipitationRenderer {
    private static final int BASE_PARTICLE_CAPACITY = 2304;
    private static final int BLIZZARD_DENSITY_MULTIPLIER = 2;
    private static final int MAX_PARTICLES = BASE_PARTICLE_CAPACITY * BLIZZARD_DENSITY_MULTIPLIER;
    private static final int SPAWN_RADIUS = 22;
    private static final int DRAW_RADIUS = 25;
    private static final int SAND_COLOR = 0xD7AA68;
    private static final int RED_SAND_COLOR = 0xBD744E;
    private static final ResourceLocation RED_OASIS = new ResourceLocation("terralith", "red_oasis");
    private static final ResourceLocation STRATIFIED_DESERT = new ResourceLocation("natures_spirit", "stratified_desert");
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<WeatherParticle> particles = new ArrayList<>(MAX_PARTICLES);
    private static ClientLevel currentLevel;
    private static Vec3 lastCamera;
    private static boolean currentStorm;

    private PrecipitationRenderer() {
    }

    static void tick(Minecraft client) {
        var camera = client.gameRenderer.getMainCamera();
        ClientLevel level = client.level;
        if (!canRender(client) || !camera.isInitialized() || camera.getFluidInCamera() != FogType.NONE) {
            clear();
            return;
        }
        if (client.isPaused()) {
            return;
        }
        Vec3 center = camera.getPosition();
        if (level != currentLevel || lastCamera == null || lastCamera.distanceToSqr(center) > DRAW_RADIUS * DRAW_RADIUS) {
            particles.clear();
            currentLevel = level;
        }
        lastCamera = center;

        float rain = level.getRainLevel(1F);
        if (rain <= 0.001F) {
            particles.clear();
            return;
        }
        float thunder = level.getThunderLevel(1F);
        boolean storm = thunder >= 0.4F;
        if (storm != currentStorm) {
            particles.clear();
            currentStorm = storm;
        }
        Vec3 wind = SandboxWeather.wind(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int available = MAX_PARTICLES;
        for (int index = particles.size() - 1; index >= 0; index--) {
            WeatherParticle particle = particles.get(index);
            if (!particle.advance(level, cursor, wind, center)) {
                particles.set(index, particles.get(particles.size() - 1));
                particles.remove(particles.size() - 1);
            } else {
                available -= particle.style == WeatherStyle.BLIZZARD ? 1 : BLIZZARD_DENSITY_MULTIPLIER;
            }
        }

        int budget = Mth.ceil(rain * Mth.lerp(thunder, 48F, 118F));
        ParticleStatus quality = client.options.particles().get();
        if (quality != ParticleStatus.ALL) {
            budget /= quality == ParticleStatus.DECREASED ? 2 : 4;
        }
        for (int index = 0; index < budget && available >= BLIZZARD_DENSITY_MULTIPLIER; index++) {
            if (spawn(level, cursor, center, wind, storm)) {
                available -= BLIZZARD_DENSITY_MULTIPLIER;
            }
        }
    }

    static void clear() {
        currentLevel = null;
        lastCamera = null;
        particles.clear();
    }

    public static boolean replacesVanilla(Minecraft client) {
        return canRender(client);
    }

    public static void draw(Minecraft client, Vec3 camera, float partialTick, float rain) {
        if (client.level != currentLevel || particles.isEmpty() || rain <= 0.001F
                || client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float daylight = Mth.lerp(client.level.getSkyDarken(partialTick), 0.32F, 1F);
        for (WeatherParticle particle : particles) {
            particle.emit(buffer, camera, partialTick, rain, daylight);
        }
        buffer.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN);
        BufferBuilder.RenderedBuffer vertices = buffer.endOrDiscardIfEmpty();
        if (vertices == null) {
            return;
        }
        drawBatch(vertices, GameRenderer.getPositionColorShader(), null);
    }

    static int sandColor(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(RED_OASIS)) {
            return RED_SAND_COLOR;
        }
        return biome.is(Tags.Biomes.IS_DESERT) || biome.is(STRATIFIED_DESERT) ? SAND_COLOR : 0;
    }

    private static boolean canRender(Minecraft client) {
        return client.level != null && LightingSettings.atmosphereEnabled()
                && SandboxWeather.hasOutdoorWeather(client.level) && !TerrainLighting.isShaderPackActive();
    }

    private static boolean spawn(ClientLevel level, BlockPos.MutableBlockPos cursor, Vec3 camera, Vec3 wind,
            boolean storm) {
        double x = camera.x + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
        double z = camera.z + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
        if ((x - camera.x) * (x - camera.x) + (z - camera.z) * (z - camera.z) > SPAWN_RADIUS * SPAWN_RADIUS
                || !level.hasChunkAt(cursor.set(x, camera.y, z))) {
            return false;
        }
        int roof = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cursor.getX(), cursor.getZ());
        double bottom = Math.max(camera.y - 7, roof + 0.08);
        double top = Math.min(camera.y + 14, level.getMaxBuildHeight() - 0.1);
        if (bottom >= top) {
            return false;
        }
        double y = bottom + RANDOM.nextDouble() * (top - bottom);
        Holder<Biome> biome = level.getBiome(cursor.set(x, y, z));
        int sand = sandColor(biome);
        WeatherStyle style;
        int color;
        if (sand != 0) {
            top = Math.min(top, roof + (storm ? 9 : 3));
            if (bottom >= top) {
                return false;
            }
            y = bottom + RANDOM.nextDouble() * (top - bottom);
            style = storm ? WeatherStyle.SANDSTORM : WeatherStyle.SANDBLOW;
            color = sand;
        } else {
            switch (biome.value().getPrecipitationAt(cursor)) {
                case RAIN -> style = storm ? WeatherStyle.HEAVY_RAIN : WeatherStyle.RAIN;
                case SNOW -> style = storm ? WeatherStyle.BLIZZARD : WeatherStyle.SNOW;
                default -> {
                    return false;
                }
            }
            color = style.color;
        }
        if (!exposedAir(level, cursor.set(x, y, z))) {
            return false;
        }
        double variation = 0.78 + RANDOM.nextDouble() * 0.46;
        particles.add(new WeatherParticle(style, color, variation, new Vec3(x, y, z), wind));
        if (style == WeatherStyle.BLIZZARD) {
            double secondY = cursor.getY() + (y - cursor.getY() < 0.5 ? 0.82 : 0.18);
            Vec3 second = new Vec3(cursor.getX() + RANDOM.nextDouble(), secondY, cursor.getZ() + RANDOM.nextDouble());
            double secondVariation = 0.78 + RANDOM.nextDouble() * 0.46;
            particles.add(new WeatherParticle(style, color, secondVariation, second, wind));
        }
        return true;
    }

    private static boolean exposedAir(ClientLevel level, BlockPos pos) {
        return SandboxWeather.outdoors(level, pos) && level.isEmptyBlock(pos);
    }

    static void drawBatch(BufferBuilder.RenderedBuffer vertices, ShaderInstance shader, ResourceLocation texture) {
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
        int activeTexture = 0;
        int previousTexture = 0;
        if (texture != null) {
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager._activeTexture(activeTexture);
        }

        try {
            if (texture != null) {
                shader.setSampler("Sampler0", Minecraft.getInstance().getTextureManager().getTexture(texture).getId());
            }
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
            if (texture != null) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                GlStateManager._bindTexture(previousTexture);
                GlStateManager._activeTexture(activeTexture);
            }
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

    private enum WeatherStyle {
        RAIN(0.70, -0.88, 0.53, 0.014, 0.57F, 32, 0xA8C1DD),
        HEAVY_RAIN(1.27, -1.02, 0.90, 0.022, 0.66F, 30, 0xA3BEDA),
        SNOW(0.24, -0.10, 0.09, 0.042, 0.78F, 79, 0xF0F5FC),
        BLIZZARD(1.26, 0, 0.18, 0.028, 0.82F, 50, 0xDCE9F6),
        SANDBLOW(0.73, 0, 0.31, 0.045, 0.43F, 46, 0),
        SANDSTORM(1.56, 0, 0.72, 0.074, 0.56F, 35, 0);

        final double windSpeed;
        final double verticalSpeed;
        final double length;
        final double width;
        final float alpha;
        final int lifetime;
        final int color;

        WeatherStyle(double windSpeed, double verticalSpeed, double length, double width,
                float alpha, int lifetime, int color) {
            this.windSpeed = windSpeed;
            this.verticalSpeed = verticalSpeed;
            this.length = length;
            this.width = width;
            this.alpha = alpha;
            this.lifetime = lifetime;
            this.color = color;
        }
    }

    private static final class WeatherParticle {
        private final WeatherStyle style;
        private final int color;
        private final double variation;
        private Vec3 previous;
        private Vec3 position;
        private Vec3 velocity;
        private int age;

        WeatherParticle(WeatherStyle style, int color, double variation, Vec3 position, Vec3 wind) {
            this.style = style;
            this.color = color;
            this.variation = variation;
            this.previous = position;
            this.position = position;
            updateVelocity(wind);
        }

        private void updateVelocity(Vec3 wind) {
            Vec3 drift = wind.scale(style.windSpeed * variation);
            velocity = new Vec3(drift.x, style.verticalSpeed * variation, drift.z);
        }

        boolean advance(ClientLevel level, BlockPos.MutableBlockPos cursor, Vec3 wind, Vec3 camera) {
            if (++age >= style.lifetime || position.distanceToSqr(camera) > DRAW_RADIUS * DRAW_RADIUS) {
                return false;
            }
            updateVelocity(wind);
            Vec3 next = position.add(velocity);
            Vec3 midpoint = position.lerp(next, 0.5);
            if (!exposedAir(level, cursor.set(midpoint.x, midpoint.y, midpoint.z))
                    || !exposedAir(level, cursor.set(next.x, next.y, next.z))) {
                return false;
            }
            previous = position;
            position = next;
            return true;
        }

        void emit(BufferBuilder buffer, Vec3 camera, float partialTick, float rain, float daylight) {
            Vec3 center = previous.lerp(position, partialTick).subtract(camera);
            double distance = center.length();
            float fade = (float) Mth.clamp((DRAW_RADIUS - distance) / 5, 0, 1)
                    * (float) Mth.clamp((distance - 0.24) / 1.2, 0, 1);
            fade *= Mth.clamp((age + partialTick) / 3, 0, 1)
                    * Mth.clamp((style.lifetime - age - partialTick) / 5, 0, 1);
            float alpha = style.alpha * rain * fade;
            if (alpha < 0.016F) {
                return;
            }
            float red = (color >> 16 & 255) * daylight / 255F;
            float green = (color >> 8 & 255) * daylight / 255F;
            float blue = (color & 255) * daylight / 255F;
            if (style == WeatherStyle.BLIZZARD) {
                emitSnowflake(buffer, center, age + partialTick, red, green, blue, alpha);
                return;
            }
            Vec3 direction = velocity.normalize();
            Vec3 side = direction.cross(center);
            if (side.lengthSqr() < 1.0E-7) {
                return;
            }
            side = side.normalize().scale(style.width * variation * 0.5);
            Vec3 front = center.add(direction.scale(style.length * variation * 0.35));
            Vec3 rear = center.subtract(direction.scale(style.length * variation * 0.65));
            vertex(buffer, front.add(side), red, green, blue, alpha);
            vertex(buffer, front.subtract(side), red, green, blue, alpha);
            vertex(buffer, rear.subtract(side), red, green, blue, alpha * 0.19F);
            vertex(buffer, rear.add(side), red, green, blue, alpha * 0.19F);
        }

        private void emitSnowflake(BufferBuilder buffer, Vec3 center, float time, float red, float green, float blue,
                float alpha) {
            Vec3 view = center.normalize();
            Vec3 right = new Vec3(view.z, 0, -view.x);
            if (right.lengthSqr() < 1.0E-7) {
                right = new Vec3(1, 0, 0);
            }
            right = right.normalize();
            Vec3 up = view.cross(right).normalize();
            double radius = style.length * variation * 0.5;
            double halfWidth = style.width * variation * 0.5;
            double rotation = time * 0.10 + variation * 12.0;
            for (int axis = 0; axis < 3; axis++) {
                double angle = rotation + axis * Math.PI / 3.0;
                double cosine = Math.cos(angle);
                double sine = Math.sin(angle);
                double tipX = (right.x * cosine + up.x * sine) * radius;
                double tipY = (right.y * cosine + up.y * sine) * radius;
                double tipZ = (right.z * cosine + up.z * sine) * radius;
                double edgeX = (up.x * cosine - right.x * sine) * halfWidth;
                double edgeY = (up.y * cosine - right.y * sine) * halfWidth;
                double edgeZ = (up.z * cosine - right.z * sine) * halfWidth;
                buffer.vertex(center.x + tipX + edgeX, center.y + tipY + edgeY, center.z + tipZ + edgeZ)
                        .color(red, green, blue, alpha).endVertex();
                buffer.vertex(center.x + tipX - edgeX, center.y + tipY - edgeY, center.z + tipZ - edgeZ)
                        .color(red, green, blue, alpha).endVertex();
                buffer.vertex(center.x - tipX - edgeX, center.y - tipY - edgeY, center.z - tipZ - edgeZ)
                        .color(red, green, blue, alpha).endVertex();
                buffer.vertex(center.x - tipX + edgeX, center.y - tipY + edgeY, center.z - tipZ + edgeZ)
                        .color(red, green, blue, alpha).endVertex();
            }
        }

        private static void vertex(BufferBuilder buffer, Vec3 point, float red, float green, float blue, float alpha) {
            buffer.vertex(point.x, point.y, point.z).color(red, green, blue, alpha).endVertex();
        }
    }
}
