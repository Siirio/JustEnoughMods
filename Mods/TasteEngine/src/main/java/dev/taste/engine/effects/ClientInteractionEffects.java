package dev.taste.engine.effects;

import dev.taste.engine.render.OwnedParticlePass;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.environment.ClientWeatherParticles.WaterRipple;
import dev.taste.engine.render.GlStateScope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Consumes client observations and owns only cosmetic particles and overlays. */
public final class ClientInteractionEffects {
    public record PortalCell(BlockPos position, Direction.Axis axis, AABB bounds, boolean end) { }

    private record Seen(LivingEntity entity, Vec3 position, boolean wet, int hurtTime, double travel,
            boolean left, long tick) { }
    private record Footprint(BlockPos receiver, BlockState state, Vec3 point, double dx, double dz, long born) { }
    private record Explosion(Vec3 point, float power, long tick) { }
    private enum Family { STEP, IMPACT, EXPLOSION, WATER, PORTAL, LEAF, PETAL }
    private record Owned(Particle particle, Family family) { }

    private static final int MAX_PARTICLES = 192;
    private static final int MAX_RINGS = 12;
    private static final int RIPPLE_TICKS = 24;
    private static final int RING_TICKS = 12;
    private final Deque<Owned> particles = new ArrayDeque<>();
    private final Deque<Footprint> footprints = new ArrayDeque<>();
    private final Deque<Explosion> pending = new ArrayDeque<>();
    private final Deque<Explosion> rings = new ArrayDeque<>();
    private final Deque<WaterRipple> ripples = new ArrayDeque<>();
    private final Map<Integer, Seen> entities = new HashMap<>();
    private final Map<Long, List<PortalCell>> portalSections = new LinkedHashMap<>();
    private final RandomSource random = RandomSource.create();
    private ClientLevel level;
    private long lastTick = Long.MIN_VALUE;
    private Vec3 eye = Vec3.ZERO;
    private int births;
    private int particleLimit;
    private double particleDistanceSquared;

    public void tick(Minecraft client, ConfigSnapshot settings, boolean graphicsActive, int weatherParticles,
            boolean freezeBirths) {
        if (level != client.level) {
            clear();
            level = client.level;
        }
        if (level == null || !graphicsActive || !settings.requestedEnabled()) {
            clear();
            return;
        }
        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return;
        eye = camera.getPosition();
        long tick = level.getGameTime();
        boolean interaction = enabled(settings, "interaction.enabled");
        boolean vegetation = enabled(settings, "vegetation.enabled");
        boolean portals = enabled(settings, "portals.enabled");
        boolean reducedMotion = enabled(settings, "accessibility.reducedMotion");
        boolean decorate = enabled(settings, "particles.enabled")
                && client.options.particles().get() != ParticleStatus.MINIMAL;
        particleLimit = decorate ? Math.min(MAX_PARTICLES, settings.value("particles.budget").getAsInt()) : 0;
        if (client.options.particles().get() == ParticleStatus.DECREASED) particleLimit /= 2;
        int totalLimit = settings.value("particles.budget").getAsInt();
        if (client.options.particles().get() == ParticleStatus.DECREASED) totalLimit /= 2;
        particleLimit = Math.min(particleLimit, Math.max(0, totalLimit - weatherParticles));
        float distance = Math.min(36, value(settings, "particles.distance"));
        particleDistanceSquared = distance * distance;
        particles.removeIf(entry -> {
            boolean keep = particleLimit > 0 && entry.particle().isAlive() && switch (entry.family()) {
                case STEP -> interaction && enabled(settings, "interaction.footstepDust");
                case IMPACT -> interaction && value(settings, "interaction.impactFlash") > 0
                        && value(settings, "accessibility.flashLimit") > 0;
                case EXPLOSION -> interaction && enabled(settings, "interaction.explosionSmoke");
                case WATER -> interaction && enabled(settings, "interaction.waterSplashes");
                case PORTAL -> portals && enabled(settings, "portals.particles");
                case LEAF -> vegetation && enabled(settings, "vegetation.fallingLeaves");
                case PETAL -> vegetation && enabled(settings, "vegetation.petals");
            };
            if (!keep) entry.particle().remove();
            return !keep;
        });
        while (particles.size() > particleLimit) particles.removeFirst().particle().remove();
        boolean decals = interaction && enabled(settings, "interaction.snowFootprints");
        int decalLimit = decals ? settings.value("interaction.maxDecals").getAsInt() : 0;
        double lifetime = value(settings, "interaction.decalLifetime") * 20.0;
        footprints.removeIf(footprint -> tick - footprint.born() >= lifetime
                || !level.hasChunkAt(footprint.receiver()) || level.getBlockState(footprint.receiver()) != footprint.state());
        while (footprints.size() > decalLimit) footprints.removeFirst();
        if (!interaction || !enabled(settings, "interaction.explosionRing") || reducedMotion) rings.clear();
        else rings.removeIf(ring -> tick - ring.tick() >= RING_TICKS);
        if (!interaction || !enabled(settings, "interaction.waterSplashes")) ripples.clear();
        else ripples.removeIf(ripple -> tick - ripple.bornTick() >= RIPPLE_TICKS);
        if (!portals) portalSections.clear();

        if (freezeBirths || client.isPaused() || tick == lastTick) return;
        lastTick = tick;
        births = 14;
        random.setSeed(tick * 0x9E3779B97F4A7C15L ^ level.dimension().location().hashCode());
        if (interaction) {
            explosions(client, settings, tick);
            observeEntities(client, settings, decalLimit, tick);
        } else {
            pending.clear();
            entities.clear();
        }
        if (portals) {
            scanPortals(tick);
            if (enabled(settings, "portals.particles") && tick % 3 == 0) portalParticles(client, tick);
        }
        if (vegetation && !reducedMotion && tick % 4 == 0) foliage(client, settings);
    }

    public void onExplosion(Vec3 point, float power) {
        if (level == null || !Float.isFinite(power) || power <= 0
                || !Double.isFinite(point.x + point.y + point.z) || point.distanceToSqr(eye) > 96.0 * 96.0) return;
        long tick = level.getGameTime();
        if (pending.stream().anyMatch(event -> tick - event.tick() < 3 && event.point().distanceToSqr(point) < 0.01)) return;
        if (pending.size() >= 16) pending.removeFirst();
        pending.addLast(new Explosion(point, Math.min(power, 12), tick));
    }

    public List<WaterRipple> waterRipples() {
        return List.copyOf(ripples);
    }

    public List<PortalCell> portalCells() {
        if (level != Minecraft.getInstance().level) return List.of();
        return portalSections.values().stream().flatMap(List::stream).limit(96).toList();
    }

    private void explosions(Minecraft client, ConfigSnapshot settings, long tick) {
        while (!pending.isEmpty()) {
            Explosion explosion = pending.removeFirst();
            if (tick - explosion.tick() > 3) continue;
            if (enabled(settings, "interaction.explosionRing") && !enabled(settings, "accessibility.reducedMotion")) {
                if (rings.size() >= MAX_RINGS) rings.removeFirst();
                rings.addLast(explosion);
            }
            if (!enabled(settings, "interaction.explosionSmoke")) continue;
            int count = Math.min(10, 3 + (int) (explosion.power() * 1.5));
            for (int i = 0; i < count && births > 4; i++) {
                double angle = i * 6.283185 / count + random.nextFloat() * 0.25;
                Vec3 direction = new Vec3(Math.cos(angle), 0.4 + random.nextDouble() * 0.5, Math.sin(angle));
                Vec3 point = explosion.point().add(direction.scale(0.3 + explosion.power() * 0.13));
                Particle smoke = spawn(client, Family.EXPLOSION, ParticleTypes.CLOUD, point, direction.scale(0.045),
                        24 + random.nextInt(16), 0.55F + explosion.power() * 0.16F);
                if (smoke != null) smoke.setColor(0.53F, 0.50F, 0.47F);
            }
        }
    }

    private void observeEntities(Minecraft client, ConfigSnapshot settings, int decalLimit, long tick) {
        entities.values().removeIf(seen -> tick - seen.tick() > 2);
        AABB bounds = AABB.ofSize(eye, 30, 18, 30);
        int examined = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, LivingEntity::isAlive)) {
            if (examined++ >= 48) break;
            if (entity.isInvisible() || entity.isSpectator() || entity.isPassenger()) continue;
            Seen previous = entities.get(entity.getId());
            Vec3 position = entity.position();
            boolean wet = entity.isInWater();
            if (previous == null || previous.entity() != entity || tick - previous.tick() != 1
                    || previous.position().distanceToSqr(position) > 16.0) {
                entities.put(entity.getId(), new Seen(entity, position, wet, entity.hurtTime, 0, false, tick));
                continue;
            }
            Vec3 movement = position.subtract(previous.position());
            if (!previous.wet() && wet && enabled(settings, "interaction.waterSplashes")) {
                waterEntry(client, entity, movement, tick);
            }
            if (entity.hurtTime >= 8 && entity.hurtTime > previous.hurtTime() && entity != client.getCameraEntity()) {
                impact(client, entity, settings);
            }

            double horizontal = movement.horizontalDistance();
            boolean stepping = entity.onGround() && !wet && horizontal > 0.025;
            double travel = stepping ? previous.travel() + horizontal : 0;
            boolean left = previous.left();
            if (travel >= (entity.isSprinting() ? 0.72 : 0.59)) {
                footstep(client, entity, movement, horizontal, left, settings, decalLimit, tick);
                travel = 0;
                left = !left;
            }
            entities.put(entity.getId(), new Seen(entity, position, wet, entity.hurtTime, travel, left, tick));
        }
    }

    private void footstep(Minecraft client, LivingEntity entity, Vec3 movement, double horizontal, boolean left,
            ConfigSnapshot settings, int decalLimit, long tick) {
        if (decalLimit <= 0 && !enabled(settings, "interaction.footstepDust")) return;
        double dx = movement.x / horizontal;
        double dz = movement.z / horizontal;
        double side = left ? 0.12 : -0.12;
        Vec3 feet = entity.position().add(-dz * side, 0, dx * side);
        BlockPos column = BlockPos.containing(feet);
        if (!level.hasChunkAt(column)) return;
        BlockHitResult hit = level.clip(new ClipContext(feet.add(0, 0.45, 0), feet.add(0, -0.56, 0),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;
        BlockPos receiver = hit.getBlockPos();
        BlockState state = level.getBlockState(receiver);
        Vec3 point = hit.getLocation();
        boolean snow = state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
        if (snow && decalLimit > 0 && fitsFootprint(receiver, state, point)) {
            while (footprints.size() >= decalLimit) footprints.removeFirst();
            footprints.addLast(new Footprint(receiver, state, point, dx, dz, tick));
        }
        if (!enabled(settings, "interaction.footstepDust") || !state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || !state.getFluidState().isEmpty() || level.isRainingAt(receiver.above())) return;
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(receiver);
        for (int i = 0; i < 3 && births > 4; i++) {
            Vec3 velocity = new Vec3(-dx * 0.035 + (random.nextDouble() - 0.5) * 0.035, 0.08 + random.nextDouble() * 0.06,
                    -dz * 0.035 + (random.nextDouble() - 0.5) * 0.035);
            spawn(client, Family.STEP, option, point.add(0, 0.06, 0), velocity, 8 + random.nextInt(5), 0.39F);
        }
    }

    private boolean fitsFootprint(BlockPos position, BlockState state, Vec3 point) {
        double localX = point.x - position.getX();
        double localY = point.y - position.getY();
        double localZ = point.z - position.getZ();
        for (AABB bounds : state.getShape(level, position).toAabbs()) {
            if (Math.abs(bounds.maxY - localY) < 0.01
                    && localX >= bounds.minX + 0.18 && localX <= bounds.maxX - 0.18
                    && localZ >= bounds.minZ + 0.18 && localZ <= bounds.maxZ - 0.18) return true;
        }
        return false;
    }

    private void waterEntry(Minecraft client, LivingEntity entity, Vec3 movement, long tick) {
        if (movement.lengthSqr() < 0.0036) return;
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + 0.12, entity.getZ());
        if (!level.hasChunkAt(pos)) return;
        FluidState water = level.getFluidState(pos);
        if (!water.is(FluidTags.WATER)) return;
        int steps = 0;
        while (steps++ < 4 && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            pos = pos.above();
            water = level.getFluidState(pos);
        }
        if (level.getFluidState(pos.above()).is(FluidTags.WATER)) return;
        Vec3 point = new Vec3(entity.getX(), pos.getY() + water.getHeight(level, pos) + 0.018, entity.getZ());
        if (point.distanceToSqr(eye) > 28.0 * 28.0) return;
        float strength = (float) Math.min(1, movement.length() * 1.6);
        if (ripples.size() >= 12) ripples.removeFirst();
        ripples.addLast(new WaterRipple(point.x, point.y, point.z, tick, strength, true));
        for (int i = 0; i < 5 && births > 4; i++) {
            double angle = i * 1.2566 + random.nextDouble() * 0.4;
            Vec3 direction = new Vec3(Math.cos(angle) * 0.09, 0.08 + strength * 0.08, Math.sin(angle) * 0.09);
            spawn(client, Family.WATER, ParticleTypes.SPLASH, point, direction, 8 + random.nextInt(4), 0.64F);
        }
    }

    private void impact(Minecraft client, LivingEntity entity, ConfigSnapshot settings) {
        float strength = Math.min(value(settings, "interaction.impactFlash"), value(settings, "accessibility.flashLimit"));
        if (strength <= 0 || births <= 5) return;
        Vec3 point = entity.position().add(0, entity.getBbHeight() * 0.63, 0);
        Particle flash = spawn(client, Family.IMPACT, ParticleTypes.CRIT, point, new Vec3(0, 0.055, 0), 4,
                0.24F + strength * 0.85F);
        if (flash != null) flash.setColor(0.91F, 0.78F, 0.57F);
    }

    private void scanPortals(long tick) {
        int centerX = SectionPos.blockToSectionCoord(eye.x);
        int centerY = SectionPos.blockToSectionCoord(eye.y);
        int centerZ = SectionPos.blockToSectionCoord(eye.z);
        portalSections.keySet().removeIf(key -> Math.abs(SectionPos.x(key) - centerX) > 1
                || Math.abs(SectionPos.y(key) - centerY) > 1 || Math.abs(SectionPos.z(key) - centerZ) > 1);
        for (int i = 0; i < 3; i++) {
            int offset = Math.floorMod(tick * 3 + i, 27);
            int x = centerX + offset % 3 - 1;
            int z = centerZ + (offset / 3) % 3 - 1;
            int y = centerY + offset / 9 - 1;
            if (y < level.getMinSection() || y >= level.getMaxSection()) continue;
            long key = SectionPos.asLong(x, y, z);
            LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk == null) {
                portalSections.remove(key);
                continue;
            }
            LevelChunkSection section = chunk.getSections()[y - level.getMinSection()];
            if (!section.maybeHas(ClientInteractionEffects::portal)) {
                portalSections.remove(key);
                continue;
            }
            java.util.ArrayList<PortalCell> cells = new java.util.ArrayList<>();
            for (int by = 0; by < 16 && cells.size() < 96; by++) {
                for (int bz = 0; bz < 16 && cells.size() < 96; bz++) {
                    for (int bx = 0; bx < 16 && cells.size() < 96; bx++) {
                        BlockState state = section.getBlockState(bx, by, bz);
                        if (!portal(state)) continue;
                        BlockPos position = new BlockPos(x * 16 + bx, y * 16 + by, z * 16 + bz);
                        boolean end = state.is(Blocks.END_PORTAL);
                        Direction.Axis axis = end ? Direction.Axis.Y : state.getValue(NetherPortalBlock.AXIS);
                        var shape = state.getShape(level, position);
                        if (!shape.isEmpty()) cells.add(new PortalCell(position, axis, shape.bounds(), end));
                    }
                }
            }
            portalSections.put(key, List.copyOf(cells));
        }
    }

    private void portalParticles(Minecraft client, long tick) {
        List<PortalCell> cells = portalCells();
        if (cells.isEmpty()) return;
        for (int i = 0; i < Math.min(cells.size(), 2); i++) {
            PortalCell cell = cells.get(Math.floorMod(tick * 7 + i * 13, cells.size()));
            if (!level.hasChunkAt(cell.position()) || !portal(level.getBlockState(cell.position()))) continue;
            Vec3 point = Vec3.atCenterOf(cell.position()).add(random.nextDouble() - 0.5,
                    random.nextDouble() - 0.5, random.nextDouble() - 0.5);
            Vec3 direction = new Vec3((random.nextDouble() - 0.5) * 0.045, 0.015,
                    (random.nextDouble() - 0.5) * 0.045);
            spawn(client, Family.PORTAL, ParticleTypes.REVERSE_PORTAL, point, direction, 15 + random.nextInt(12), 0.54F);
        }
    }

    private void foliage(Minecraft client, ConfigSnapshot settings) {
        boolean leaves = enabled(settings, "vegetation.fallingLeaves");
        boolean petals = enabled(settings, "vegetation.petals");
        if ((!leaves && !petals) || particleLimit <= 0) return;
        BlockPos center = BlockPos.containing(eye);
        double angle = level.getGameTime() * 0.0025;
        for (int i = 0; i < 7 && births > 2; i++) {
            BlockPos pos = center.offset(random.nextInt(21) - 10, random.nextInt(9) + 1, random.nextInt(21) - 10);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES) || !level.getBlockState(pos.below()).isAir()) continue;
            Vec3 point = new Vec3(pos.getX() + random.nextDouble(), pos.getY() - 0.06, pos.getZ() + random.nextDouble());
            Vec3 motion = new Vec3(Math.cos(angle) * 0.025, -0.028, Math.sin(angle) * 0.025);
            if (petals && state.is(Blocks.CHERRY_LEAVES)) {
                spawn(client, Family.PETAL, ParticleTypes.CHERRY_LEAVES, point, motion, 38, 0.73F);
            } else if (leaves && canSpawn(point)) {
                TerrainParticle particle = new FallingLeaf(level, point, motion, state, pos);
                client.particleEngine.add(particle);
                remember(particle, Family.LEAF);
            }
        }
    }

    private Particle spawn(Minecraft client, Family family, ParticleOptions option, Vec3 point, Vec3 velocity,
            int lifetime, float size) {
        if (!canSpawn(point)) return null;
        Particle particle = client.particleEngine.createParticle(option, point.x, point.y, point.z,
                velocity.x, velocity.y, velocity.z);
        if (particle == null) return null;
        particle.setParticleSpeed(velocity.x, velocity.y, velocity.z);
        particle.setLifetime(lifetime);
        particle.scale(size);
        remember(particle, family);
        return particle;
    }

    private boolean canSpawn(Vec3 point) {
        return births > 0 && particles.size() < particleLimit && eye.distanceToSqr(point) <= particleDistanceSquared;
    }

    private void remember(Particle particle, Family family) {
        float emission = switch (family) {
            case IMPACT -> 1.1F;
            case PORTAL -> 0.75F;
            default -> 0.0F;
        };
        OwnedParticlePass.track(particle, family == Family.EXPLOSION, emission);
        particles.addLast(new Owned(particle, family));
        births--;
    }

    public void render(Camera camera, Matrix4f pose, float partialTick, ConfigSnapshot settings) {
        if (level == null || level != Minecraft.getInstance().level || !enabled(settings, "interaction.enabled")
                || footprints.isEmpty() && rings.isEmpty()) return;
        boolean decals = enabled(settings, "interaction.snowFootprints") && !footprints.isEmpty();
        boolean explosionRings = enabled(settings, "interaction.explosionRing")
                && !enabled(settings, "accessibility.reducedMotion") && !rings.isEmpty();
        if (!decals && !explosionRings) return;
        double tick = level.getGameTime() + partialTick;
        Vec3 cameraPosition = camera.getPosition();
        try (GlStateScope ignored = GlStateScope.capture()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            if (decals) {
                double lifetime = value(settings, "interaction.decalLifetime") * 20.0;
                for (Footprint footprint : footprints) {
                    if (footprint.point().distanceToSqr(cameraPosition) > 32.0 * 32.0) continue;
                    float alpha = 0.22F * Math.min(1.0F, (float) ((1.0 - (tick - footprint.born()) / lifetime) * 4));
                    if (alpha <= 0) continue;
                    Vec3 point = footprint.point().subtract(cameraPosition).add(0, 0.0035, 0);
                    footprintQuad(buffer, pose, footprint, point, 0.02, 0.16, 0.075, alpha);
                    footprintQuad(buffer, pose, footprint, point, -0.12, -0.015, 0.054, alpha * 0.85F);
                }
            }
            if (explosionRings) {
                float flash = value(settings, "accessibility.flashLimit");
                for (Explosion explosion : rings) ring(buffer, pose, explosion, cameraPosition, tick, flash);
            }
            BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
            if (rendered != null) BufferUploader.drawWithShader(rendered);
        }
    }

    private static void footprintQuad(BufferBuilder buffer, Matrix4f pose, Footprint footprint, Vec3 point,
            double start, double end, double width, float alpha) {
        double x = footprint.dx();
        double z = footprint.dz();
        Vec3 side = new Vec3(-z * width, 0, x * width);
        Vec3 heel = point.add(x * start, 0, z * start);
        Vec3 toe = point.add(x * end, 0, z * end);
        vertex(buffer, pose, heel.subtract(side), 0.36F, 0.43F, 0.53F, alpha);
        vertex(buffer, pose, heel.add(side), 0.36F, 0.43F, 0.53F, alpha);
        vertex(buffer, pose, toe.add(side), 0.36F, 0.43F, 0.53F, alpha);
        vertex(buffer, pose, toe.subtract(side), 0.36F, 0.43F, 0.53F, alpha);
    }

    private static void ring(BufferBuilder buffer, Matrix4f pose, Explosion explosion, Vec3 eye, double tick, float flash) {
        if (explosion.point().distanceToSqr(eye) > 40.0 * 40.0) return;
        double progress = Math.max(0, Math.min(1, (tick - explosion.tick()) / RING_TICKS));
        float alpha = (float) (0.26 * flash * (1 - progress) * (1 - progress));
        double radius = (0.12 + (1 - Math.pow(1 - progress, 2)) * explosion.power() * 1.35);
        Vec3 center = explosion.point().subtract(eye).add(0, 0.08, 0);
        for (int segment = 0; segment < 24; segment++) {
            double a = segment * Math.PI / 12;
            double b = (segment + 1) * Math.PI / 12;
            Vec3 first = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 second = new Vec3(Math.cos(b), 0, Math.sin(b));
            vertex(buffer, pose, center.add(first.scale(radius)), 0.81F, 0.72F, 0.62F, alpha);
            vertex(buffer, pose, center.add(first.scale(radius + 0.16)), 0.81F, 0.72F, 0.62F, 0);
            vertex(buffer, pose, center.add(second.scale(radius + 0.16)), 0.81F, 0.72F, 0.62F, 0);
            vertex(buffer, pose, center.add(second.scale(radius)), 0.81F, 0.72F, 0.62F, alpha);
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, Vec3 point, float r, float g, float b, float a) {
        buffer.vertex(pose, (float) point.x, (float) point.y, (float) point.z).color(r, g, b, a).endVertex();
    }

    private void clear() {
        for (Owned entry : particles) entry.particle().remove();
        particles.clear();
        footprints.clear();
        pending.clear();
        rings.clear();
        ripples.clear();
        entities.clear();
        portalSections.clear();
        lastTick = Long.MIN_VALUE;
    }

    private static boolean portal(BlockState state) {
        return state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL);
    }

    private static boolean enabled(ConfigSnapshot settings, String id) {
        return settings.value(id).getAsBoolean();
    }

    private static float value(ConfigSnapshot settings, String id) {
        return settings.value(id).getAsFloat();
    }

    private static final class FallingLeaf extends TerrainParticle {
        FallingLeaf(ClientLevel level, Vec3 point, Vec3 velocity, BlockState state, BlockPos source) {
            super(level, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z, state, source);
            setParticleSpeed(velocity.x, velocity.y, velocity.z);
            gravity = 0.037F;
            friction = 0.98F;
            setLifetime(42);
            scale(0.31F);
        }
    }
}
