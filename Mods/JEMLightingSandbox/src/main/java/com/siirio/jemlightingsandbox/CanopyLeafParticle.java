package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class CanopyLeafParticle extends TextureSheetParticle {
    private static final ResourceLocation GALMIUM_FOLIAGE = ResourceLocation.tryParse(
            "deep_dark_regrowth:block/galmium_leaves_ext");
    private final Shape shape;
    private final boolean gust;
    private final float spin;
    private final float phase;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private int settledAge = -1;

    static boolean spawn(Minecraft client, ClientLevel level, BlockPos source, BlockState state,
                         Vec3 origin, Vec3 wind, boolean gust, boolean snowy) {
        var sprite = CanopyLeaves.GALMIUM.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
                ? client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(GALMIUM_FOLIAGE)
                : client.getBlockRenderer().getBlockModelShaper().getTexture(state, level, source);
        if (!sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)
                || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return false;
        }
        int tint = client.getBlockColors().getColor(state, level, source, 0);
        client.particleEngine.add(new CanopyLeafParticle(level, origin, wind, gust, snowy, shape(state), sprite, tint));
        return true;
    }

    private CanopyLeafParticle(ClientLevel level, Vec3 origin, Vec3 wind, boolean gust, boolean snowy,
                               Shape shape, TextureAtlasSprite sprite, int tint) {
        super(level, origin.x, origin.y, origin.z);
        this.shape = shape;
        this.gust = gust;
        setSprite(sprite);
        lifetime = 120 + random.nextInt(41);
        quadSize = shape.size * (0.78F + random.nextFloat() * 0.44F);
        setSize(quadSize, quadSize);
        gravity = 0F;
        friction = 1F;
        hasPhysics = true;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        spin = (random.nextBoolean() ? 1F : -1F) * (0.032F + random.nextFloat() * 0.018F);
        phase = random.nextFloat() * Mth.TWO_PI;
        double windGain = gust ? 0.10 : 0.035;
        setParticleSpeed(wind.x * windGain, gust ? 0.040 : 0.023, wind.z * windGain);

        float frost = snowy ? 0.16F : 0F;
        float light = 0.88F + random.nextFloat() * 0.12F;
        setColor(Mth.lerp(frost, (tint >> 16 & 255) / 255F * light, 0.89F),
                Mth.lerp(frost, (tint >> 8 & 255) / 255F * light, 0.93F),
                Mth.lerp(frost, (tint & 255) / 255F * light, 0.97F));

        var contents = sprite.contents();
        int texelX = contents.width() / 2;
        int texelY = contents.height() / 2;
        for (int probe = 0; probe < 8; probe++) {
            int x = random.nextInt(contents.width());
            int y = random.nextInt(contents.height());
            if ((sprite.getPixelRGBA(0, x, y) >>> 24) >= 200) {
                texelX = x;
                texelY = y;
                break;
            }
        }
        float crop = shape.crop;
        float u = Mth.clamp((texelX + 0.5F) * 16F / contents.width() - crop * 0.5F, 0F, 16F - crop);
        float v = Mth.clamp((texelY + 0.5F) * 16F / contents.height() - crop * 0.5F, 0F, 16F - crop);
        u0 = sprite.getU(u);
        u1 = sprite.getU(u + crop);
        v0 = sprite.getV(v);
        v1 = sprite.getV(v + crop);
    }

    @Override
    public void tick() {
        if (!LightingSettings.canopyLeavesEnabled()) {
            remove();
            return;
        }
        oRoll = roll;
        if (settledAge < 0) {
            float wake = 1F - Mth.clamp(age / (gust ? 108F : 90F), 0F, 1F);
            float sway = Mth.sin(age * 0.14F + phase);
            var wind = SandboxWeather.wind(level);
            double gain = gust ? 0.098 : 0.036;
            xd = Mth.lerp(0.14, xd, wind.x * (0.6 + 0.4 * wake) * gain + sway * 0.009);
            zd = Mth.lerp(0.14, zd, wind.z * (0.6 + 0.4 * wake) * gain - sway * 0.009);
            yd = Mth.lerp(0.19, yd, (gust ? 0.069 : 0.050) * wake - 0.018 + sway * 0.006);
            roll += spin * (1F + Math.abs(sway) * 0.36F);
        }

        super.tick();
        if (settledAge < 0 && (onGround || (x == xo && y == yo && z == zo))) {
            settledAge = age;
            setParticleSpeed(0, 0, 0);
            lifetime = Math.min(lifetime, age + 30);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (!LightingSettings.canopyLeavesEnabled()) {
            return;
        }
        float time = Math.max(0F, age - 1F + partialTick);
        float fadeIn = Mth.clamp(time / 8F, 0F, 1F);
        float fadeOut = Mth.clamp((lifetime - time) / 24F, 0F, 1F);
        float opacity = fadeIn * fadeOut;
        if (opacity < 0.01F) {
            return;
        }

        Vec3 eye = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - eye.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - eye.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - eye.z);
        var orientation = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(partialTick, oRoll, roll));
        float tumbleTime = settledAge < 0 ? time : Math.min(time, settledAge);
        orientation.rotateY(Mth.sin(tumbleTime * 0.15F + phase) * 1.02F);
        var vertex = new Vector3f();
        float width = quadSize * shape.width;
        int light = getLightColor(partialTick);
        renderVertex(buffer, orientation, vertex, px, py, pz, -width, -quadSize * shape.shoulder, u0, v1, opacity, light);
        renderVertex(buffer, orientation, vertex, px, py, pz, -width * 0.38F, quadSize, u0, v0, opacity, light);
        renderVertex(buffer, orientation, vertex, px, py, pz, width, quadSize * shape.shoulder, u1, v0, opacity, light);
        renderVertex(buffer, orientation, vertex, px, py, pz, width * 0.28F, -quadSize, u1, v1, opacity, light);
    }

    private void renderVertex(VertexConsumer buffer, Quaternionf orientation, Vector3f vertex,
                              float px, float py, float pz, float vx, float vy, float u, float v,
                              float opacity, int light) {
        vertex.set(vx, vy, 0F).rotate(orientation);
        buffer.vertex(px + vertex.x, py + vertex.y, pz + vertex.z).uv(u, v)
                .color(rCol, gCol, bCol, opacity).uv2(light).endVertex();
    }

    private static Shape shape(BlockState state) {
        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES) || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.MANGROVE_LEAVES)
                || state.is(Blocks.AZALEA_LEAVES)) {
            return Shape.BROADLEAF;
        }

        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().replace('/', '_');
        if (path.contains("pale_oak") || path.contains("paleoak")) {
            return Shape.PALE;
        }
        Shape shape = Shape.GENERIC;
        for (String part : path.split("_")) {
            switch (part) {
                case "flower", "flowers", "flowering", "blossom", "blossoms", "cherry", "sakura", "petal", "jacaranda" -> {
                    return Shape.PETAL;
                }
                case "spruce", "pine", "fir", "cedar", "larch", "cypress", "redwood", "hemlock", "sequoia", "conifer", "juniper" ->
                        shape = Shape.NEEDLE;
                case "acacia", "savanna", "dry", "baobab", "palm", "eucalyptus" -> {
                    if (shape == Shape.GENERIC) {
                        shape = Shape.SAVANNA;
                    }
                }
                default -> { }
            }
        }
        return shape;
    }

    private enum Shape {
        BROADLEAF(0.064F, 0.67F, 0.30F, 5F),
        NEEDLE(0.049F, 0.24F, 0.74F, 3F),
        PETAL(0.050F, 0.86F, 0.18F, 4F),
        SAVANNA(0.060F, 0.34F, 0.58F, 4F),
        PALE(0.067F, 0.62F, 0.47F, 5F),
        GENERIC(0.058F, 0.54F, 0.42F, 5F);

        final float size;
        final float width;
        final float shoulder;
        final float crop;

        Shape(float size, float width, float shoulder, float crop) {
            this.size = size;
            this.width = width;
            this.shoulder = shoulder;
            this.crop = crop;
        }
    }
}
