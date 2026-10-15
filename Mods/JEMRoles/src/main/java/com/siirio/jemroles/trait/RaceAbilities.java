package com.siirio.jemroles.trait;

import com.siirio.jemroles.network.RaceActionPacket;
import com.siirio.jemroles.network.RolesNetwork;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceRules;
import com.siirio.jemroles.race.RaceService;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RaceAbilities {
    private static final Map<UUID, Long> VOID_CHARGE = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<Zone> ZONES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Eruption> ERUPTIONS = new CopyOnWriteArrayList<>();

    private RaceAbilities() {}

    public static void handle(ServerPlayer player, RaceActionPacket.Action action) {
        if (!player.server.isDedicatedServer() || !player.isAlive() || player.isSpectator()) return;
        RaceService.record(player).ifPresent(record -> {
            if (action == RaceActionPacket.Action.SIGNATURE) signature(player, record);
            else if (action == RaceActionPacket.Action.UTILITY_PRESS) utilityPress(player, record);
            else utilityRelease(player, record);
        });
    }

    /** Iron's Spells entry point; uses the same authoritative cooldown, tier and effect path as the V key. */
    public static void castSignature(ServerPlayer player) {
        if (!player.server.isDedicatedServer() || !player.isAlive() || player.isSpectator()) return;
        RaceService.record(player).ifPresent(record -> signature(player, record));
    }

    private static void signature(ServerPlayer player, RaceRecord record) {
        String key = "signature";
        long now = player.level().getGameTime();
        int cooldown = switch (record.race()) {
            case ANCESTORS -> RaceRules.LIFE_CHORUS_COOLDOWN.get();
            case VOID_WALKERS -> RaceRules.RIFT_AMBUSH_COOLDOWN.get();
            case BORN_IN_BLOOM -> RaceRules.VERDANT_DOMINION_COOLDOWN.get();
            case INFECTED_BY_SCULK -> RaceRules.SCULK_VEINS_COOLDOWN.get();
            default -> 1_200;
        };
        if (!record.ready(key, now)) { cooldown(player, record.remaining(key, now)); return; }
        boolean success = switch (record.race()) {
            case ANCESTORS -> lifeChorus(player, record.spellTier());
            case VOID_WALKERS -> riftAmbush(player, record.spellTier());
            case BORN_IN_BLOOM -> verdantDominion(player, record.spellTier());
            case INFECTED_BY_SCULK -> sculkVeins(player, record.spellTier());
            case NETHER_HEARTEDS -> infernalEruption(player, record.spellTier());
        };
        if (!success) return;
        record.cooldown(key, now + cooldown);
        record.recordSignatureUse();
        RaceService.mutate(player, ignored -> {});
        RolesNetwork.sendStatus(player, false);
    }

    private static void utilityPress(ServerPlayer player, RaceRecord record) {
        long now = player.level().getGameTime();
        switch (record.race()) {
            case VOID_WALKERS -> {
                if (player.isShiftKeyDown()) gravityDenial(player, record, now);
                else VOID_CHARGE.put(player.getUUID(), now);
            }
            case BORN_IN_BLOOM -> RaceService.mutate(player, value -> value.livingPath(!value.livingPath()));
            case INFECTED_BY_SCULK -> {
                if (record.resonance() >= 25) consumeEcho(player, record);
                else echoVision(player);
            }
            case NETHER_HEARTEDS -> startLavaDrink(player, record, now);
            default -> player.sendSystemMessage(Component.translatable("jem.roles.utility.none"), true);
        }
        RolesNetwork.sendStatus(player, false);
    }

    private static void utilityRelease(ServerPlayer player, RaceRecord record) {
        if (record.race() == RaceId.NETHER_HEARTEDS) {
            if (player.isUsingItem() && player.getUseItem().is(Items.LAVA_BUCKET)) player.stopUsingItem();
            return;
        }
        if (record.race() != RaceId.VOID_WALKERS) return;
        Long started = VOID_CHARGE.remove(player.getUUID());
        long now = player.level().getGameTime();
        if (started == null || now - started < 10 || !record.ready("void_projectile", now)) return;
        ThrownEnderpearl pearl = new ThrownEnderpearl(player.level(), player);
        pearl.getPersistentData().putBoolean("JemVoidProjectile", true);
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.25F);
        player.level().addFreshEntity(pearl);
        RaceService.mutate(player, value -> value.cooldown("void_projectile", now + RaceRules.VOID_PROJECTILE_COOLDOWN.get()));
    }

    private static boolean lifeChorus(ServerPlayer player, int tier) {
        double radius = tier >= 2 ? 16.0D : 12.0D;
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), LivingEntity::isAlive)) {
            if (target instanceof Animal || target instanceof AbstractVillager || recruit(target)) target.setHealth(target.getMaxHealth());
            else if (target instanceof Player) target.heal(6.0F);
            else continue;
            if (tier >= 2) target.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                    .map(MobEffectInstance::getEffect).toList().forEach(target::removeEffect);
            if (tier >= 3 && target.getHealth() >= target.getMaxHealth()) target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        }
        player.serverLevel().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 24, radius / 3, 1, radius / 3, 0.1);
        return true;
    }

    private static boolean riftAmbush(ServerPlayer player, int tier) {
        LivingEntity target = target(player, tier >= 2 ? 32.0D : 24.0D);
        if (target == null || target instanceof Player && !player.server.isPvpAllowed()) return false;
        Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(1.8D));
        BlockPos safe = RaceEnvironment.nearestSafe(player, player.serverLevel(), BlockPos.containing(behind), 3, true).orElse(null);
        if (safe == null) return false;
        if (tier >= 2) spawnVoidEcho(player);
        player.teleportTo(player.serverLevel(), safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, target.getYRot(), player.getXRot());
        player.attack(target);
        if (tier >= 3) for (LivingEntity nearby : player.serverLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4), e -> e != player && e != target))
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        return true;
    }

    private static boolean verdantDominion(ServerPlayer player, int tier) {
        double radius = tier == 1 ? 6 : tier == 2 ? 8 : 10;
        int hit = 0;
        java.util.Set<LivingEntity> rooted = new java.util.HashSet<>();
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !(e instanceof Animal))) {
            root(player, target, 3.0F + tier * 1.5F);
            rooted.add(target);
        }
        if (tier >= 2) for (LivingEntity source : java.util.List.copyOf(rooted))
            for (LivingEntity spread : player.serverLevel().getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(3),
                    entity -> entity != player && entity.isAlive() && !(entity instanceof Animal) && !rooted.contains(entity))) {
                root(player, spread, 2.0F);
                rooted.add(spread);
            }
        hit = rooted.size();
        if (tier >= 3 && hit > 0) ZONES.add(new Zone(player.serverLevel().dimension(), player.blockPosition(),
                player.level().getGameTime() + 100, player.getUUID(), ZoneType.GROVE));
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(), player.getZ(), 20 + tier * 10, radius / 2, 0.5, radius / 2, 0.1);
        return hit > 0;
    }

    private static boolean sculkVeins(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        BlockPos start = nearestSculk(level, player.blockPosition(), 3);
        if (start == null) return false;
        int range = tier == 1 ? 32 : tier == 2 ? 48 : 64;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start); visited.add(start);
        BlockPos best = start;
        double bestAim = -Double.MAX_VALUE;
        while (!queue.isEmpty() && visited.size() < 2048) {
            BlockPos pos = queue.removeFirst();
            if (pos.distManhattan(start) > range) continue;
            if (tier < 3 && pos.distSqr(player.blockPosition()) > best.distSqr(player.blockPosition())) best = pos;
            else if (tier >= 3 && pos.distSqr(player.blockPosition()) >= 16) {
                Vec3 direction = Vec3.atCenterOf(pos).subtract(player.getEyePosition()).normalize();
                double aim = direction.dot(player.getLookAngle()) + Math.min(0.35D, pos.distSqr(player.blockPosition()) / 12_000.0D);
                if (aim > bestAim) { bestAim = aim; best = pos; }
            }
            for (var direction : net.minecraft.core.Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.add(next) && sculk(level, next)) queue.addLast(next);
            }
        }
        BlockPos destination = RaceEnvironment.nearestSafe(player, level, best.above(), 3, true).orElse(null);
        if (destination == null || destination.closerToCenterThan(player.position(), 4.0D)) return false;
        player.teleportTo(level, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, player.getYRot(), player.getXRot());
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.7, 0.5, 0.05);
        return true;
    }

    private static boolean infernalEruption(ServerPlayer player, int tier) {
        LivingEntity primary = target(player, 24.0D);
        if (primary == null) return false;
        Vec3 line = primary.position().subtract(player.position()).normalize();
        int fissures = tier == 1 ? 1 : 3;
        long now = player.level().getGameTime();
        for (int i = 0; i < fissures; i++) ERUPTIONS.add(new Eruption(player.serverLevel().dimension(),
                primary.position().add(line.scale(i * 2.5D)), now + 8 + i * 3L, player.getUUID(), tier));
        if (tier >= 3) ZONES.add(new Zone(player.serverLevel().dimension(), primary.blockPosition(),
                player.level().getGameTime() + 100, player.getUUID(), ZoneType.VOLCANIC));
        return true;
    }

    private static void root(ServerPlayer player, LivingEntity target, float damage) {
        target.hurt(player.damageSources().playerAttack(player), damage);
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.15D, 0.0D, 0.15D));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4));
    }

    private static void erupt(ServerPlayer player, Vec3 center, int tier) {
        double radius = 2.5D;
        AABB area = new AABB(center, center).inflate(radius, 2.0D, radius);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player)) {
            target.hurt(player.damageSources().onFire(), 4.0F + tier * 2.0F);
            target.setSecondsOnFire(3 + tier);
            target.push(0, 0.7D + tier * 0.15D, 0);
        }
        player.serverLevel().sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 35, radius, 0.3, radius, 0.08);
    }

    private static void spawnVoidEcho(ServerPlayer player) {
        ArmorStand echo = new ArmorStand(EntityType.ARMOR_STAND, player.serverLevel());
        echo.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        echo.setInvisible(true);
        echo.setInvulnerable(true);
        echo.setNoGravity(true);
        echo.setCustomName(Component.literal("Void Echo"));
        echo.setCustomNameVisible(true);
        echo.getPersistentData().putLong("JemVoidEchoExpiry", player.level().getGameTime() + 60);
        if (!player.serverLevel().addFreshEntity(echo)) return;
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, echo.getBoundingBox().inflate(12), entity -> entity instanceof Enemy))
            mob.setTarget(echo);
        player.serverLevel().sendParticles(ParticleTypes.PORTAL, echo.getX(), echo.getY() + 1, echo.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
    }

    public static void serverTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        ERUPTIONS.removeIf(eruption -> {
            ServerLevel level = server.getLevel(eruption.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(eruption.owner);
            if (level == null || owner == null || owner.serverLevel() != level) return true;
            for (int layer = 0; layer < 7; layer++) {
                double y = eruption.center.y + layer * 0.55D;
                level.sendParticles(layer < 2 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                        eruption.center.x, y, eruption.center.z, 4, 0.22D, 0.12D, 0.22D, 0.015D);
            }
            if (now < eruption.impactAt) return false;
            erupt(owner, eruption.center, eruption.tier);
            return true;
        });
        ZONES.removeIf(zone -> {
            if (zone.expires <= now) return true;
            ServerLevel level = server.getLevel(zone.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(zone.owner);
            if (level == null || owner == null || owner.serverLevel() != level) return true;
            AABB area = new AABB(zone.center).inflate(zone.type == ZoneType.GROVE ? 8 : 6, 3, zone.type == ZoneType.GROVE ? 8 : 6);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != owner && entity.isAlive() && !(entity instanceof Animal))) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, zone.type == ZoneType.GROVE ? 2 : 1, false, false));
                if (zone.type == ZoneType.VOLCANIC && server.getTickCount() % 20 == 0) target.setSecondsOnFire(2);
            }
            if (server.getTickCount() % 10 == 0) level.sendParticles(zone.type == ZoneType.GROVE ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.FLAME,
                    zone.center.getX() + 0.5D, zone.center.getY() + 0.2D, zone.center.getZ() + 0.5D, 12, 3, 0.2, 3, 0.03);
            return false;
        });
    }

    private static void gravityDenial(ServerPlayer player, RaceRecord record, long now) {
        if (!record.ready("gravity", now)) { cooldown(player, record.remaining("gravity", now)); return; }
        RaceService.mutate(player, value -> {
            value.cooldown("gravity_active", now + RaceRules.GRAVITY_DURATION.get());
            value.cooldown("gravity", now + RaceRules.GRAVITY_COOLDOWN.get());
        });
    }

    private static void consumeEcho(ServerPlayer player, RaceRecord record) {
        if (record.resonance() < 25) return;
        RaceService.mutate(player, value -> value.resonance(value.resonance() - 25));
        player.heal(6.0F);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
    }

    private static void echoVision(ServerPlayer player) {
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(32), entity -> entity != player && entity.isAlive()))
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
        player.serverLevel().sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 32, 8, 3, 8, 0.02);
    }

    private static void startLavaDrink(ServerPlayer player, RaceRecord record, long now) {
        if (!record.ready("lava_drink", now)) { cooldown(player, record.remaining("lava_drink", now)); return; }
        InteractionHand hand = player.getMainHandItem().is(Items.LAVA_BUCKET) ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().is(Items.LAVA_BUCKET) ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            player.sendSystemMessage(Component.translatable("jem.roles.lava_drink_requires_bucket"), true);
            return;
        }
        player.startUsingItem(hand);
    }

    private static boolean recruit(LivingEntity target) {
        String namespace = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace();
        return namespace.equals("recruits") || namespace.equals("workers");
    }

    private static LivingEntity target(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition(), look = player.getViewVector(1.0F), to = from.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()).stream()
                .filter(e -> e.getBoundingBox().inflate(0.5D).clip(from, to).isPresent())
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static BlockPos nearestSculk(ServerLevel level, BlockPos origin, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius)))
            if (sculk(level, pos)) return pos.immutable();
        return null;
    }

    private static boolean sculk(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.SCULK) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER);
    }

    private static void cooldown(ServerPlayer player, long ticks) {
        player.sendSystemMessage(Component.translatable("jem.roles.cooldown", Math.max(1L, (ticks + 19) / 20)), true);
    }

    public static void logout(ServerPlayer player) { VOID_CHARGE.remove(player.getUUID()); }

    private enum ZoneType { GROVE, VOLCANIC }
    private record Eruption(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 center,
                            long impactAt, UUID owner, int tier) {}
    private record Zone(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos center,
                        long expires, UUID owner, ZoneType type) {}
}
