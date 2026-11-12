package dev.taste.engine.environment;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A depth-tested strip in the native translucent particle sheet. */
final class WeatherParticleStrip extends Particle {
    private final Vec3 axis;
    private final float width;
    private final float length;
    private final float u;
    private final float v;

    WeatherParticleStrip(ClientLevel level, Vec3 point, Vec3 velocity, Vec3 axis, TextureAtlasSprite sprite,
            float width, float length, float opacity, int lifetime) {
        super(level, point.x, point.y, point.z);
        this.axis = axis;
        this.width = width;
        this.length = length;
        this.lifetime = lifetime;
        u = (sprite.getU0() + sprite.getU1()) * 0.5F;
        v = (sprite.getV0() + sprite.getV1()) * 0.5F;
        alpha = opacity;
        xd = velocity.x;
        yd = velocity.y;
        zd = velocity.z;
        hasPhysics = velocity.lengthSqr() > 0;
        setSize(0.02F, 0.02F);
        friction = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (onGround) remove();
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 eye = camera.getPosition();
        Vec3 start = new Vec3(Mth.lerp(partialTick, xo, x) - eye.x,
                Mth.lerp(partialTick, yo, y) - eye.y, Mth.lerp(partialTick, zo, z) - eye.z);
        Vec3 side = start.cross(axis).normalize().scale(width * 0.5);
        if (side.lengthSqr() < 1.0E-10) return;
        Vec3 end = start.add(axis.scale(length));
        int light = getLightColor(partialTick);
        float fade = alpha * Math.max(0, 1 - (age + partialTick) / lifetime);
        vertex(consumer, start.subtract(side), fade, light);
        vertex(consumer, start.add(side), fade, light);
        vertex(consumer, end.add(side), fade * 0.36F, light);
        vertex(consumer, end.subtract(side), fade * 0.36F, light);
    }

    private void vertex(VertexConsumer consumer, Vec3 point, float opacity, int light) {
        consumer.vertex(point.x, point.y, point.z).uv(u, v).color(rCol, gCol, bCol, opacity).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
