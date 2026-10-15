package com.siirio.jemroles.client.vfx;

import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.world.options.LodestoneItemCrumbsParticleOptions;
import team.lodestar.lodestone.systems.particle.world.options.LodestoneTerrainParticleOptions;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

public final class RaceVfxParticles {
    private RaceVfxParticles() {}

    static void onAccepted(ClientLevel level, RaceVfxInstance instance) {
        Vec3 origin = instance.packet().origin();
        int tier = Math.max(1, instance.packet().tier());
        switch (instance.packet().id()) {
            case LIFE_CHORUS, GUARDIAN_FORM, GUARDIAN_DISSOLVE, MERCHANT_BLESSING ->
                    ancestralMotes(level, origin, 8 + tier * 3, instance.random());
            case RIFT_DEPART, RIFT_ARRIVE, VOID_PROJECTILE_CHARGE, VOID_PROJECTILE_RELEASE,
                    VOID_PROJECTILE_IMPACT, VOID_RESCUE, GRAVITY_DENIAL ->
                    voidFragments(level, origin, 8 + tier * 3, instance.random());
            case VERDANT_DOMINION, LIVING_PATH_STEP, SECOND_BLOSSOM, PHOTOSYNTHESIS ->
                    leafFragments(level, origin, 7 + tier * 3, instance.random());
            case SCULK_VEINS, ECHO_VISION, ECHO_ABSORB, RESONANCE_THRESHOLD ->
                    sculkVibrations(level, origin, 8 + tier * 3, instance.random());
            case INFERNAL_IMPACT -> {
                embers(level, origin, 14 + tier * 5, instance.random());
                terrainDebris(level, origin, 6 + tier * 2, instance.random());
            }
            case WATER_STEAM -> steam(level, origin, 7 + tier * 2, instance.random());
            case INFERNAL_TELEGRAPH, VOLCANIC_ZONE, LAVA_DRINK_START, LAVA_DRINK_COMPLETE,
                    LAVA_SWIM -> embers(level, origin, 7 + tier * 3, instance.random());
        }
    }

    static void tick(ClientLevel level, RaceVfxInstance instance, long now) {
        long age = now - instance.startedTick();
        if (age <= 0L) return;
        switch (instance.packet().id()) {
            case VOLCANIC_ZONE -> {
                if (age % 8L == 0L) embers(level, instance.packet().origin(), 3, instance.random());
            }
            case GRAVITY_DENIAL -> {
                if (age % 5L == 0L) voidFragments(level, instance.packet().origin(), 2, instance.random());
            }
            case LAVA_SWIM -> {
                if (age % 3L == 0L) embers(level, instance.packet().origin(), 2, instance.random());
            }
            default -> { }
        }
    }

    public static void ancestralMotes(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        spawnWorld(level, position, requested, random, LodestoneParticleRegistry.WISP_PARTICLE,
                RaceVfxPalettes.ANCESTOR, RaceVfxPalettes.ANCESTOR_LIGHT, 0.08F, 0.34F, 24, 0.018D);
    }

    public static void voidFragments(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        spawnWorld(level, position, requested, random, LodestoneParticleRegistry.SPARK_PARTICLE,
                RaceVfxPalettes.VOID, RaceVfxPalettes.VOID_LIGHT, 0.10F, 0.28F, 18, 0.012D);
    }

    public static void leafFragments(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        int count = RaceVfxController.claimParticles(requested);
        for (int i = 0; i < count; i++) {
            WorldParticleBuilder.create(new LodestoneItemCrumbsParticleOptions(
                            LodestoneParticleRegistry.ITEM_PARTICLE, new ItemStack(Items.OAK_LEAVES)))
                    .setScaleData(scale(0.22F, 0.34F, 0.0F))
                    .setTransparencyData(alpha())
                    .setLifetime(22 + random.nextInt(10))
                    .setRandomOffset(0.8D, 0.35D, 0.8D)
                    .setMotion((random.nextDouble() - 0.5D) * 0.06D, 0.025D + random.nextDouble() * 0.05D,
                            (random.nextDouble() - 0.5D) * 0.06D)
                    .enableNoClip()
                    .spawn(level, position.x, position.y + 0.15D, position.z);
        }
    }

    public static void sculkVibrations(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        spawnWorld(level, position, requested, random, LodestoneParticleRegistry.TWINKLE_PARTICLE,
                RaceVfxPalettes.SCULK, RaceVfxPalettes.SCULK_LIGHT, 0.08F, 0.30F, 18, 0.014D);
    }

    public static void embers(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        spawnWorld(level, position, requested, random, LodestoneParticleRegistry.EXTRUDING_SPARK_PARTICLE,
                RaceVfxPalettes.NETHER, RaceVfxPalettes.NETHER_LIGHT, 0.08F, 0.24F, 16, 0.026D);
    }

    public static void steam(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        spawnWorld(level, position, requested, random, LodestoneParticleRegistry.SMOKE_PARTICLE,
                0xA9C3C8, 0xEEF7F4, 0.12F, 0.42F, 22, 0.032D);
    }

    public static void terrainDebris(ClientLevel level, Vec3 position, int requested, RandomSource random) {
        int count = RaceVfxController.claimParticles(requested);
        BlockPos blockPos = BlockPos.containing(position).below();
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) return;
        for (int i = 0; i < count; i++) {
            WorldParticleBuilder.create(new LodestoneTerrainParticleOptions(
                            LodestoneParticleRegistry.TERRAIN_PARTICLE, state, blockPos))
                    .setScaleData(scale(0.18F, 0.30F, 0.0F))
                    .setTransparencyData(alpha())
                    .setLifetime(14 + random.nextInt(8))
                    .setRandomOffset(0.45D, 0.08D, 0.45D)
                    .setMotion((random.nextDouble() - 0.5D) * 0.12D, 0.08D + random.nextDouble() * 0.12D,
                            (random.nextDouble() - 0.5D) * 0.12D)
                    .setGravityStrength(0.55F)
                    .spawn(level, position.x, position.y + 0.1D, position.z);
        }
    }

    private static void spawnWorld(ClientLevel level, Vec3 position, int requested, RandomSource random,
                                   RegistryObject<? extends LodestoneWorldParticleType> type,
                                   int startColor, int endColor, float startScale, float middleScale,
                                   int lifetime, double rise) {
        int count = RaceVfxController.claimParticles(requested);
        for (int i = 0; i < count; i++) {
            WorldParticleBuilder.create(type)
                    .setColorData(colors(startColor, endColor))
                    .setScaleData(scale(startScale, middleScale, 0.0F))
                    .setTransparencyData(alpha())
                    .setLifetime(lifetime + random.nextInt(8))
                    .setRandomOffset(0.65D, 0.45D, 0.65D)
                    .setMotion((random.nextDouble() - 0.5D) * 0.045D, rise + random.nextDouble() * 0.035D,
                            (random.nextDouble() - 0.5D) * 0.045D)
                    .enableNoClip()
                    .enableCull()
                    .spawn(level, position.x, position.y + 0.15D, position.z);
        }
    }

    private static GenericParticleData scale(float start, float middle, float end) {
        return GenericParticleData.create(start, middle, end)
                .setEasing(Easing.QUAD_OUT, Easing.QUAD_IN).build();
    }

    private static GenericParticleData alpha() {
        return GenericParticleData.create(0.0F, 0.88F, 0.0F)
                .setEasing(Easing.QUAD_OUT, Easing.QUAD_IN).build();
    }

    private static ColorParticleData colors(int start, int end) {
        return ColorParticleData.create(color(start), color(end)).setEasing(Easing.SINE_IN_OUT).build();
    }

    private static Color color(int rgb) { return new Color(rgb); }
}
