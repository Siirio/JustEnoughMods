package dev.taste.engine.environment;

import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.effects.PhotoCapture;
import dev.taste.engine.render.OwnedParticlePass;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Adds bounded decoration through the native particle engine. */
public final class ClientWeatherParticles {
    private enum Kind {
        RAIN, SPLASH, RAIN_MIST, SNOW, HAIL, BUBBLE, LAVA_EMBER, LAVA_MIST, CAMPFIRE_SMOKE,
        DUST, INDOOR_DUST, FIREFLY, POLLEN, BREATH, DRIP, SPARK, DEBRIS, TRAIL
    }

    private record Owned(Kind kind, Particle particle, Vec3 origin) { }
    private record Contact(LivingEntity entity, int hurtTime, long tick) { }
    private record BlockTarget(ClientLevel level, BlockPos position, BlockState state, long expires) { }
    public record WaterRipple(double x, double y, double z, long bornTick, float strength, boolean entity) {
        public WaterRipple(double x, double y, double z, long bornTick, float strength) {
            this(x, y, z, bornTick, strength, false);
        }
    }

    private static final int MAX_ACTIVE = 384;
    private static final int MAX_BIRTHS_PER_TICK = 16;
    private static final int MAX_RIPPLES = 12;
    private static final int MAX_INDOOR_DUST = 16;
    private static final int RIPPLE_TICKS = 24;
    private static final int CELL_SIZE = 4;
    private static final double RAIN_FALL_SPEED = 0.67;
    private static final double RAIN_WIND_SPEED = 0.07;
    private static final ResourceLocation STRIP_SPRITE = new ResourceLocation("minecraft", "generic_0");
    private static final Set<EntityType<?>> TRAIL_PROJECTILES = Set.of(
            EntityType.ARROW, EntityType.SPECTRAL_ARROW, EntityType.TRIDENT, EntityType.SNOWBALL, EntityType.EGG);
    private final Deque<Owned> owned = new ArrayDeque<>();
    private final Deque<WaterRipple> ripples = new ArrayDeque<>();
    private final Map<Integer, Contact> contacts = new HashMap<>();
    private final EnvironmentResolver environments = new EnvironmentResolver();
    private final RandomSource random = RandomSource.create();
    private final int[] familyCounts = new int[Kind.values().length];
    private ClientLevel world;
    private BlockTarget blockTarget;
    private boolean precipitationAvailable;
    private long lastTick = Long.MIN_VALUE;
    private int limit;
    private int births;
    private double distanceSquared;

    public void tick(Minecraft client, ConfigSnapshot settings, boolean graphicsActive, boolean freezeBirths) {
        ClientLevel level = client.level;
        if (world != level) {
            clear();
            world = level;
            lastTick = Long.MIN_VALUE;
        }
        if (!graphicsActive || level == null || !settings.requestedEnabled()) {
            clear();
            return;
        }
        boolean rainRequested = rainRequested(settings);
        boolean snowRequested = enabled(settings, "weather.snowEnabled") && number(settings, "weather.snowDensity") > 0;
        boolean decorate = enabled(settings, "particles.enabled")
                && client.options.particles().get() != ParticleStatus.MINIMAL;
        if (!decorate) clearParticles();
        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || camera.getEntity() == null) return;

        long tick = level.getGameTime();
        Vec3 eye = camera.getPosition();
        EnvironmentSnapshot environment = environments.resolve(client, camera, 1.0F,
                number(settings, "general.visualTime"), PhotoCapture.instance());
        EnumSet<Kind> kinds = decorate ? activeKinds(settings, environment) : EnumSet.noneOf(Kind.class);
        boolean rippleEnabled = enabled(settings, "weather.ripples");
        if (rippleEnabled) ripples.removeIf(ripple -> tick - ripple.bornTick() >= RIPPLE_TICKS);
        else ripples.clear();
        limit = decorate ? Math.min(MAX_ACTIVE, settings.value("particles.budget").getAsInt()) : 0;
        if (client.options.particles().get() == ParticleStatus.DECREASED) limit /= 2;
        double distance = number(settings, "particles.distance");
        distanceSquared = distance * distance;
        prune(kinds, eye);
        TextureAtlasSprite strip = kinds.contains(Kind.RAIN) || kinds.contains(Kind.TRAIL) ? stripSprite(client) : null;
        precipitationAvailable = (!rainRequested || strip != null && limit > 0) && (!snowRequested || limit > 0);
        if (freezeBirths || client.isPaused() || lastTick == tick || (kinds.isEmpty() && !rippleEnabled)) return;
        lastTick = tick;
        births = decorate ? MAX_BIRTHS_PER_TICK : 0;

        if (kinds.contains(Kind.DEBRIS)) debris(client, level, eye, tick);
        else blockTarget = null;
        if (kinds.contains(Kind.SPARK)) sparks(client, level, eye, tick);
        else contacts.clear();
        if ((tick & 1) == 0 && environment.rain() > 0.0F && environment.hasSky()) {
            weather(client, level, camera, settings, environment, kinds, eye, tick, strip, rippleEnabled);
        }
        if (kinds.contains(Kind.BUBBLE) && (tick & 3) == 0) bubbles(client, level, eye, tick);
        if (tick % 3 == 0) emitters(client, level, kinds, eye, tick);
        if (tick % 5 == 0) ambient(client, level, environment, settings, kinds, eye, tick);
        if (kinds.contains(Kind.BREATH) && tick % 12 == 0) breath(client, level, eye);
        if (kinds.contains(Kind.TRAIL) && strip != null) trails(client, level, eye, strip);
    }

    public List<WaterRipple> waterRipples() {
        return List.copyOf(ripples);
    }

    public int ownedCount() {
        return owned.size();
    }

    public boolean ownsPrecipitation() {
        return precipitationAvailable;
    }

    public void watchBreak(ClientLevel level, BlockPos position) {
        if (!level.hasChunkAt(position)) return;
        BlockState state = level.getBlockState(position);
        if (!state.isAir()) blockTarget = new BlockTarget(level, position.immutable(), state, level.getGameTime() + 6);
    }

    public void cancelBreak(BlockPos position) {
        if (blockTarget != null && blockTarget.position().equals(position)) blockTarget = null;
    }

    private void weather(Minecraft client, ClientLevel level, Camera camera, ConfigSnapshot settings,
            EnvironmentSnapshot environment, EnumSet<Kind> kinds, Vec3 eye, long tick,
            TextureAtlasSprite strip, boolean rippleEnabled) {
        boolean impacts = kinds.contains(Kind.SPLASH) || kinds.contains(Kind.RAIN_MIST) || rippleEnabled;
        if (!impacts && strip == null && !kinds.contains(Kind.SNOW) && !kinds.contains(Kind.HAIL)) return;
        float rain = environment.rain();
        float wind = number(settings, "weather.windStrength");
        double windX = environment.windX() * wind;
        double windZ = environment.windZ() * wind;
        for (int i = 0; i < 12 && (births > 0 || rippleEnabled); i++) {
            BlockPos column = sampleColumn(eye, tick, i, 3);
            if (!level.hasChunkAt(column)) continue;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            if (surface <= level.getMinBuildHeight() || Math.abs(surface - eye.y) > 16.0) continue;
            BlockPos top = column.atY(surface);
            Biome.Precipitation precipitation = level.getBiome(top).value().getPrecipitationAt(top);
            double x = top.getX() + 0.2 + random.nextDouble() * 0.6;
            double z = top.getZ() + 0.2 + random.nextDouble() * 0.6;
            if (precipitation == Biome.Precipitation.SNOW && kinds.contains(Kind.SNOW)
                    && chance(rain * number(settings, "weather.snowDensity") * 0.16F)) {
                Vec3 birth = new Vec3(x, Math.max(surface + 1.0, eye.y + 3.5), z);
                spawn(client, Kind.SNOW, ParticleTypes.SNOWFLAKE, eye, birth,
                        new Vec3(windX * 0.035, -0.07, windZ * 0.035), 44,
                        number(settings, "weather.snowSize") / 0.035F);
            }
            if (precipitation != Biome.Precipitation.RAIN) continue;
            if (strip != null && kinds.contains(Kind.RAIN) && chance(rain * number(settings, "weather.rainDensity") * 0.56F)) {
                Vec3 velocity = new Vec3(windX * RAIN_WIND_SPEED, -RAIN_FALL_SPEED, windZ * RAIN_WIND_SPEED);
                Vec3 point = new Vec3(x, Math.max(surface + 1.2, eye.y + 5.0 + random.nextDouble() * 3.0), z);
                streak(client, level, Kind.RAIN, eye, point, velocity, velocity.normalize().scale(-1), strip,
                        number(settings, "weather.rainWidth"), number(settings, "weather.rainLength"),
                        rain * number(settings, "weather.rainOpacity"), 24);
            }
            if (kinds.contains(Kind.HAIL) && chance(environment.thunder() * 0.18F)) {
                Vec3 birth = new Vec3(x, Math.max(surface + 1.0, eye.y + 3.5), z);
                spawn(client, Kind.HAIL, ParticleTypes.SNOWFLAKE, eye, birth,
                        new Vec3(windX * 0.06, -0.25, windZ * 0.06), 20, 0.5F);
            }
            boolean splash = kinds.contains(Kind.SPLASH) && chance(rain * number(settings, "weather.splashDensity") * 0.22F);
            boolean mist = kinds.contains(Kind.RAIN_MIST) && chance(rain * number(settings, "weather.rainMist") * 0.07F);
            boolean ripple = rippleEnabled && chance(rain * 0.28F);
            if (!splash && !mist && !ripple) continue;
            double windOffset = 5.25 * RAIN_WIND_SPEED / RAIN_FALL_SPEED;
            Vec3 start = new Vec3(x - windX * windOffset, surface + 4.0, z - windZ * windOffset);
            Vec3 end = new Vec3(x, surface - 1.25, z);
            if (!level.hasChunkAt(BlockPos.containing(start)) || !level.hasChunkAt(BlockPos.containing(end))) continue;
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY, camera.getEntity()));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) continue;
            FluidState fluid = level.getFluidState(hit.getBlockPos());
            Vec3 contact = hit.getLocation().add(0, 0.03, 0);
            if (ripple && fluid.is(FluidTags.WATER) && contact.distanceToSqr(eye) <= distanceSquared) {
                if (ripples.size() >= MAX_RIPPLES) ripples.removeFirst();
                ripples.addLast(new WaterRipple(contact.x, contact.y, contact.z, tick, rain));
            }
            if (splash && fluid.isEmpty()) {
                spawn(client, Kind.SPLASH, ParticleTypes.SPLASH, eye, contact, Vec3.ZERO, 8, 0.7F);
            }
            if (mist && !fluid.is(FluidTags.LAVA)) {
                Particle particle = spawn(client, Kind.RAIN_MIST, ParticleTypes.CLOUD, eye, contact,
                        new Vec3(windX * 0.025, 0.016, windZ * 0.025), 22, 0.28F);
                if (particle != null) particle.setColor(0.71F, 0.76F, 0.80F);
            }
        }
    }

    private void emitters(Minecraft client, ClientLevel level, EnumSet<Kind> kinds, Vec3 eye, long tick) {
        boolean lava = kinds.contains(Kind.LAVA_EMBER) || kinds.contains(Kind.LAVA_MIST);
        boolean smoke = kinds.contains(Kind.CAMPFIRE_SMOKE);
        boolean drip = kinds.contains(Kind.DRIP);
        if (!lava && !smoke && !drip) return;
        int baseY = BlockPos.containing(eye).getY();
        for (int i = 0; i < 8 && births > 0; i++) {
            BlockPos column = sampleColumn(eye, tick, 80 + i, 2);
            if (!level.hasChunkAt(column)) continue;
            for (int dy = 5; dy >= -4; dy--) {
                int y = baseY + dy;
                if (y <= level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) continue;
                BlockPos pos = column.atY(y);
                BlockState state = level.getBlockState(pos);
                FluidState fluid = state.getFluidState();
                if (lava && fluid.is(FluidTags.LAVA)) {
                    boolean falling = fluid.hasProperty(FlowingFluid.FALLING) && fluid.getValue(FlowingFluid.FALLING);
                    Vec3 point = Vec3.atCenterOf(pos).add(0, 0.53, 0);
                    if (kinds.contains(Kind.LAVA_EMBER) && level.getBlockState(pos.above()).isAir() && chance(0.4F)) {
                        spawn(client, Kind.LAVA_EMBER, ParticleTypes.LAVA, eye, point, Vec3.ZERO, 20, 0.58F);
                    }
                    if (falling && kinds.contains(Kind.LAVA_MIST) && chance(0.34F)) {
                        lavaMist(client, level, eye, pos);
                    }
                    break;
                }
                if (smoke && CampfireBlock.isLitCampfire(state) && level.getBlockState(pos.above()).isAir()) {
                    spawn(client, Kind.CAMPFIRE_SMOKE, ParticleTypes.SMOKE, eye,
                            Vec3.atCenterOf(pos).add(0, 0.45, 0), new Vec3(0, 0.04, 0), 28, 0.62F);
                    break;
                }
                if (drip && dy > 0 && state.isAir() && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                    spawn(client, Kind.DRIP, ParticleTypes.FALLING_WATER, eye,
                            Vec3.atCenterOf(pos).add(0, 0.43, 0), Vec3.ZERO, 16, 0.60F);
                    break;
                }
            }
        }
    }

    private void lavaMist(Minecraft client, ClientLevel level, Vec3 eye, BlockPos source) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(source.relative(side)).isAir()) continue;
            Vec3 point = Vec3.atCenterOf(source).add(side.getStepX() * 0.56, 0.1, side.getStepZ() * 0.56);
            Particle haze = spawn(client, Kind.LAVA_MIST, ParticleTypes.CLOUD, eye, point,
                    new Vec3(side.getStepX() * 0.026, 0.028, side.getStepZ() * 0.026), 20, 0.44F);
            if (haze != null) haze.setColor(0.92F, 0.57F, 0.34F);
            break;
        }
    }

    private void bubbles(Minecraft client, ClientLevel level, Vec3 eye, long tick) {
        for (int i = 0; i < 4 && births > 0; i++) {
            BlockPos pos = sampleColumn(eye, tick, 128 + i, 1).below(1 + i % 3);
            if (!level.hasChunkAt(pos) || level.getBlockState(pos).is(Blocks.BUBBLE_COLUMN)) continue;
            FluidState water = level.getFluidState(pos);
            if (!water.is(FluidTags.WATER)) continue;
            double height = water.getHeight(level, pos);
            if (height <= 0.15) continue;
            Vec3 point = new Vec3(pos.getX() + random.nextDouble(), pos.getY() + height * 0.5,
                    pos.getZ() + random.nextDouble());
            spawn(client, Kind.BUBBLE, ParticleTypes.BUBBLE, eye, point, new Vec3(0, 0.055, 0), 23, 0.66F);
        }
    }

    private void ambient(Minecraft client, ClientLevel level, EnvironmentSnapshot environment,
            ConfigSnapshot settings, EnumSet<Kind> kinds, Vec3 eye, long tick) {
        if (kinds.contains(Kind.INDOOR_DUST)) indoorDust(client, level, environment, settings, eye, tick);
        boolean dust = kinds.contains(Kind.DUST);
        boolean flies = kinds.contains(Kind.FIREFLY);
        boolean pollen = kinds.contains(Kind.POLLEN);
        if (!dust && !flies && !pollen) return;
        long day = Math.floorMod(level.getDayTime(), 24_000L);
        boolean night = day > 13_000L && day < 23_000L;
        for (int i = 0; i < 5 && births > 0; i++) {
            BlockPos pos = sampleColumn(eye, tick, 160 + i, 2).offset(0, i % 3 - 1, 0);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()) continue;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            boolean outside = pos.getY() >= surface;
            Vec3 point = Vec3.atCenterOf(pos);
            var biome = level.getBiome(pos);
            if (dust && outside && !night && environment.rain() < 0.08F
                    && pos.getY() <= surface + 1 && drySoil(level.getBlockState(pos.atY(surface - 1))) && chance(0.16F)) {
                Vec3 velocity = new Vec3(environment.windX() * 0.012, 0.007, environment.windZ() * 0.012);
                spawn(client, Kind.DUST, ParticleTypes.WHITE_ASH, eye, point, velocity, 38, 0.34F);
            } else if (flies && outside && night && environment.rain() < 0.18F && chance(0.18F)
                    && (biome.is(BiomeTags.IS_FOREST) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP))) {
                Particle fly = spawn(client, Kind.FIREFLY, ParticleTypes.GLOW, eye, point,
                        new Vec3(0.018, 0.012, -0.008), 54, 0.35F);
                if (fly != null) fly.setColor(0.92F, 0.98F, 0.43F);
            } else if (pollen && outside && !night && environment.rain() < 0.08F && chance(0.16F)
                    && (biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.MEADOW) || biome.is(Biomes.CHERRY_GROVE))) {
                Particle grain = spawn(client, Kind.POLLEN, ParticleTypes.WHITE_ASH, eye, point,
                        new Vec3(environment.windX() * 0.012, -0.009, environment.windZ() * 0.012), 48, 0.43F);
                if (grain != null) grain.setColor(0.95F, 0.78F, 0.36F);
            }
        }
    }

    private static boolean drySoil(BlockState state) {
        return state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(BlockTags.TERRACOTTA);
    }

    private void indoorDust(Minecraft client, ClientLevel level, EnvironmentSnapshot environment,
            ConfigSnapshot settings, Vec3 eye, long tick) {
        if (births <= 0 || familyCounts[Kind.INDOOR_DUST.ordinal()] >= MAX_INDOOR_DUST) return;
        double angle = environment.sunAngle();
        Vec3 sun = new Vec3(-Math.sin(angle), Math.cos(angle), 0);
        boolean sunlight = environment.hasSky() && environment.rain() < 0.65F && sun.y > 0.16
                && enabled(settings, "lighting.sunEnabled") && number(settings, "lighting.sunIntensity") > 0;
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos position = sampleColumn(eye, tick, 270 + attempt, 1).offset(0, attempt % 3 - 1, 0);
            if (level.isOutsideBuildHeight(position) || !level.hasChunkAt(position)
                    || !level.getBlockState(position).isAir()) continue;
            if (position.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ())) continue;
            int blockLight = level.getBrightness(LightLayer.BLOCK, position);
            boolean lamp = blockLight >= 8;
            Vec3 point = Vec3.atCenterOf(position).add((random.nextDouble() - 0.5) * 0.4,
                    (random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4);
            boolean beam = sunlight && level.getBrightness(LightLayer.SKY, position) >= 8
                    && sunReaches(client, level, point, sun);
            if (!(lamp || beam) || !chance(beam ? 0.45F : 0.18F + (blockLight - 8) * 0.025F)) continue;
            Vec3 drift = new Vec3((random.nextDouble() - 0.5) * 0.014, 0.004 + random.nextDouble() * 0.004,
                    (random.nextDouble() - 0.5) * 0.014);
            int lifetime = 38 + random.nextInt(20);
            if (spawn(client, Kind.INDOOR_DUST, ParticleTypes.WHITE_ASH, eye, point, drift,
                    lifetime, beam ? 0.32F : 0.27F) != null) return;
        }
    }

    private static boolean sunReaches(Minecraft client, ClientLevel level, Vec3 point, Vec3 sun) {
        Vec3 end = point.add(sun.scale(28));
        BlockPos sky = BlockPos.containing(end);
        if (client.getCameraEntity() == null || !level.hasChunkAt(sky) || !level.canSeeSky(sky)) return false;
        Vec3 start = point;
        for (int layer = 0; layer <= 3; layer++) {
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, client.getCameraEntity()));
            if (hit.getType() == HitResult.Type.MISS) return true;
            BlockPos block = hit.getBlockPos();
            if (!level.getBlockState(block).propagatesSkylightDown(level, block)) return false;
            Vec3 contact = hit.getLocation();
            double exitX = sun.x > 1.0e-5 ? (block.getX() + 1 - contact.x) / sun.x
                    : sun.x < -1.0e-5 ? (block.getX() - contact.x) / sun.x : Double.POSITIVE_INFINITY;
            double exitY = (block.getY() + 1 - contact.y) / sun.y;
            double advance = Math.max(0, Math.min(exitX, exitY)) + 0.01;
            start = contact.add(sun.scale(advance));
            if (start.subtract(point).lengthSqr() >= 28 * 28) return false;
        }
        return false;
    }

    private void breath(Minecraft client, ClientLevel level, Vec3 eye) {
        int considered = 0;
        for (AbstractClientPlayer player : level.players()) {
            if (considered++ >= 32 || births <= 0) break;
            if (!player.isAlive() || player.isInvisible() || player.isInWater() || player.isSpectator()) continue;
            if (player == client.getCameraEntity() && client.options.getCameraType().isFirstPerson()) continue;
            Vec3 face = player.getEyePosition();
            if (face.distanceToSqr(eye) > Math.min(distanceSquared, 144.0)) continue;
            BlockPos pos = BlockPos.containing(face);
            if (!level.hasChunkAt(pos) || !level.getBiome(pos).value().coldEnoughToSnow(pos)) continue;
            Vec3 direction = player.getLookAngle();
            spawn(client, Kind.BREATH, ParticleTypes.CLOUD, eye, face.add(direction.scale(0.37)).add(0, -0.15, 0),
                    direction.scale(0.025).add(0, 0.016, 0), 10, 0.33F);
        }
    }

    private void sparks(Minecraft client, ClientLevel level, Vec3 eye, long tick) {
        contacts.values().removeIf(contact -> tick - contact.tick() > 2);
        AABB bounds = AABB.ofSize(eye, 24.0, 16.0, 24.0);
        int considered = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, LivingEntity::isAlive)) {
            if (considered++ >= 64) break;
            if (entity.isInvisible() || entity == client.getCameraEntity()) continue;
            Contact previous = contacts.put(entity.getId(), new Contact(entity, entity.hurtTime, tick));
            if (entity.hurtTime < 8 || (previous != null && previous.entity() == entity
                    && previous.hurtTime() >= entity.hurtTime)) continue;
            Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.55, 0);
            for (int i = 0; i < 3 && births > 8; i++) {
                double angle = (entity.getId() * 0.41 + tick * 0.81 + i * 2.0944);
                Vec3 direction = new Vec3(Math.cos(angle), 0.55 + 0.1 * i, Math.sin(angle));
                spawn(client, Kind.SPARK, ParticleTypes.CRIT, eye, center.add(direction.scale(0.21)),
                        direction.scale(0.105), 6 + i, 0.42F);
            }
        }
    }

    private void debris(Minecraft client, ClientLevel level, Vec3 eye, long tick) {
        BlockTarget target = blockTarget;
        if (target == null) return;
        if (target.level() != level || tick > target.expires() || !level.hasChunkAt(target.position())) {
            blockTarget = null;
            return;
        }
        BlockState current = level.getBlockState(target.position());
        if (current == target.state()) return;
        blockTarget = null;
        FluidState previousFluid = target.state().getFluidState();
        if (!current.isAir() && (previousFluid.isEmpty() || current != previousFluid.createLegacyBlock())) return;
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, target.state()).setPos(target.position());
        Vec3 center = Vec3.atCenterOf(target.position());
        for (int i = 0; i < 5 && births > 8; i++) {
            double angle = target.position().asLong() * 0x1.0p-32 + i * 1.2566;
            Vec3 direction = new Vec3(Math.cos(angle), 0.65 + 0.1 * i, Math.sin(angle));
            spawn(client, Kind.DEBRIS, particle, eye, center.add(direction.scale(0.18)),
                    direction.scale(0.11), 8 + i, 0.51F);
        }
    }

    private void trails(Minecraft client, ClientLevel level, Vec3 eye, TextureAtlasSprite strip) {
        AABB bounds = AABB.ofSize(eye, 28.0, 20.0, 28.0);
        int considered = 0;
        for (Projectile entity : level.getEntitiesOfClass(Projectile.class, bounds,
                projectile -> TRAIL_PROJECTILES.contains(projectile.getType()))) {
            if (considered++ >= 16 || births <= 0) break;
            if (entity.isInWater() || entity.getDeltaMovement().lengthSqr() < 0.015) continue;
            Vec3 current = entity.position();
            Vec3 backwards = entity.getPosition(0.0F).subtract(current);
            double length = backwards.length();
            if (length < 0.12 || length > 4.0) continue;
            streak(client, level, Kind.TRAIL, eye, current, Vec3.ZERO, backwards.scale(1.0 / length), strip,
                    0.022F, (float) length, 0.36F, 5);
        }
    }

    private void streak(Minecraft client, ClientLevel level, Kind kind, Vec3 eye, Vec3 position,
            Vec3 velocity, Vec3 axis, TextureAtlasSprite sprite, float width, float length, float opacity, int lifetime) {
        if (width <= 0 || length <= 0 || opacity <= 0 || !canSpawn(kind, position, eye)) return;
        WeatherParticleStrip particle = new WeatherParticleStrip(level, position, velocity, axis, sprite,
                width, length, opacity, lifetime);
        client.particleEngine.add(particle);
        remember(kind, particle, position);
    }

    private static TextureAtlasSprite stripSprite(Minecraft client) {
        if (client.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas atlas) {
            TextureAtlasSprite sprite = atlas.getSprite(STRIP_SPRITE);
            if (sprite != null && sprite.contents().name().equals(STRIP_SPRITE)) return sprite;
        }
        return null;
    }

    private Particle spawn(Minecraft client, Kind kind, ParticleOptions option, Vec3 eye,
            Vec3 position, Vec3 velocity, int lifetime, float scale) {
        if (scale <= 0.0F || !canSpawn(kind, position, eye)) return null;
        Particle particle = client.particleEngine.createParticle(option, position.x, position.y, position.z,
                velocity.x, velocity.y, velocity.z);
        if (particle == null) return null;
        if (velocity.lengthSqr() > 0.0) particle.setParticleSpeed(velocity.x, velocity.y, velocity.z);
        particle.setLifetime(lifetime);
        particle.scale(scale);
        remember(kind, particle, position);
        return particle;
    }

    private boolean canSpawn(Kind kind, Vec3 position, Vec3 eye) {
        int maximum = switch (kind) {
            case RAIN -> 176;
            case INDOOR_DUST -> MAX_INDOOR_DUST;
            default -> 64;
        };
        return births > 0 && owned.size() < limit && familyCounts[kind.ordinal()] < maximum
                && position.distanceToSqr(eye) <= distanceSquared;
    }

    private void remember(Kind kind, Particle particle, Vec3 position) {
        boolean soft = switch (kind) {
            case RAIN_MIST, LAVA_MIST, CAMPFIRE_SMOKE, DUST, INDOOR_DUST, POLLEN, BREATH -> true;
            default -> false;
        };
        float emission = switch (kind) {
            case FIREFLY -> 1.35F;
            case LAVA_EMBER -> 1.8F;
            case SPARK -> 1.0F;
            default -> 0.0F;
        };
        OwnedParticlePass.track(particle, soft, emission);
        owned.addLast(new Owned(kind, particle, position));
        familyCounts[kind.ordinal()]++;
        births--;
    }

    private void prune(EnumSet<Kind> kinds, Vec3 eye) {
        Iterator<Owned> iterator = owned.iterator();
        while (iterator.hasNext()) {
            Owned entry = iterator.next();
            if (!entry.particle().isAlive() || !kinds.contains(entry.kind())
                    || entry.origin().distanceToSqr(eye) > distanceSquared) {
                entry.particle().remove();
                familyCounts[entry.kind().ordinal()]--;
                iterator.remove();
            }
        }
        while (owned.size() > limit) {
            Owned entry = owned.removeFirst();
            entry.particle().remove();
            familyCounts[entry.kind().ordinal()]--;
        }
    }

    private void clear() {
        clearParticles();
        ripples.clear();
        precipitationAvailable = false;
    }

    private void clearParticles() {
        for (Owned entry : owned) entry.particle().remove();
        owned.clear();
        java.util.Arrays.fill(familyCounts, 0);
        contacts.clear();
        blockTarget = null;
    }

    private BlockPos sampleColumn(Vec3 eye, long tick, int index, int radius) {
        BlockPos center = BlockPos.containing(eye);
        int width = radius * 2 + 1;
        int cell = (int) Math.floorMod(tick * 13L + index * 11L, (long) width * width);
        int cellX = Math.floorDiv(center.getX(), CELL_SIZE) + cell % width - radius;
        int cellZ = Math.floorDiv(center.getZ(), CELL_SIZE) + cell / width - radius;
        random.setSeed(cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL
                ^ tick * 0x165667B19E3779F9L ^ world.dimension().location().hashCode());
        return new BlockPos(cellX * CELL_SIZE + random.nextInt(CELL_SIZE), center.getY(),
                cellZ * CELL_SIZE + random.nextInt(CELL_SIZE));
    }

    private boolean chance(float probability) {
        return random.nextFloat() < probability;
    }

    private static EnumSet<Kind> activeKinds(ConfigSnapshot settings, EnvironmentSnapshot environment) {
        EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);
        if (enabled(settings, "weather.rainEnabled")) {
            if (rainRequested(settings)) kinds.add(Kind.RAIN);
            if (enabled(settings, "weather.splashes") && number(settings, "weather.splashDensity") > 0) kinds.add(Kind.SPLASH);
            if (number(settings, "weather.rainMist") > 0) kinds.add(Kind.RAIN_MIST);
            if (enabled(settings, "weather.hailStyle")) kinds.add(Kind.HAIL);
        }
        if (enabled(settings, "weather.snowEnabled") && number(settings, "weather.snowDensity") > 0) kinds.add(Kind.SNOW);
        if (environment.underwater() && enabled(settings, "underwater.bubbles")) kinds.add(Kind.BUBBLE);
        if (enabled(settings, "lava.enabled")) {
            if (enabled(settings, "lava.embers") && enabled(settings, "particles.embers")) kinds.add(Kind.LAVA_EMBER);
            if (enabled(settings, "lava.fallMist")) kinds.add(Kind.LAVA_MIST);
        }
        if (enabled(settings, "particles.smoke")) kinds.add(Kind.CAMPFIRE_SMOKE);
        if (enabled(settings, "particles.dust")) kinds.add(Kind.DUST);
        if (enabled(settings, "fog.indoorDust") && !environment.underwater() && !environment.lava()) kinds.add(Kind.INDOOR_DUST);
        if (enabled(settings, "particles.fireflies")) kinds.add(Kind.FIREFLY);
        if (enabled(settings, "particles.pollen")) kinds.add(Kind.POLLEN);
        if (enabled(settings, "particles.breath")) kinds.add(Kind.BREATH);
        if (enabled(settings, "particles.drips")) kinds.add(Kind.DRIP);
        if (enabled(settings, "particles.sparks")) kinds.add(Kind.SPARK);
        if (enabled(settings, "particles.debris")) kinds.add(Kind.DEBRIS);
        if (enabled(settings, "particles.trails")) kinds.add(Kind.TRAIL);
        return kinds;
    }

    private static boolean enabled(ConfigSnapshot settings, String id) {
        return settings.value(id).getAsBoolean();
    }

    private static boolean rainRequested(ConfigSnapshot settings) {
        return enabled(settings, "weather.rainEnabled") && number(settings, "weather.rainDensity") > 0
                && number(settings, "weather.rainLength") > 0 && number(settings, "weather.rainWidth") > 0
                && number(settings, "weather.rainOpacity") > 0;
    }

    private static float number(ConfigSnapshot settings, String id) {
        return settings.value(id).getAsFloat();
    }
}
