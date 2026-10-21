package com.siirio.jemroles.trait;

import com.siirio.jemroles.network.RaceActionPacket;
import com.siirio.jemroles.network.RaceVfxPacket;
import com.siirio.jemroles.network.RolesNetwork;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceRules;
import com.siirio.jemroles.race.RaceService;
import com.siirio.jemroles.vfx.RaceVfxEmitter;
import com.siirio.jemroles.vfx.RaceVfxId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
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
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_visual_sandbox")) {
            if(action==RaceActionPacket.Action.SIGNATURE)castSignature(player);
            else try {Class.forName("com.siirio.jemvisualsandbox.workbench.RaceExperience").getMethod("utility",ServerPlayer.class,boolean.class).invoke(null,player,action==RaceActionPacket.Action.UTILITY_PRESS);}
            catch(ReflectiveOperationException failure){throw new IllegalStateException("Final race utility",failure);}
            return;
        }
        if (!player.server.isDedicatedServer() || !player.isAlive() || player.isSpectator()) return;
        RaceService.record(player).ifPresent(record -> {
            if (action == RaceActionPacket.Action.SIGNATURE) signature(player, record);
            else if (action == RaceActionPacket.Action.UTILITY_PRESS) utilityPress(player, record);
            else utilityRelease(player, record);
        });
    }

    /** Iron's Spells entry point; uses the same authoritative cooldown, tier and effect path as the V key. */
    public static boolean castSignature(ServerPlayer player) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_visual_sandbox")) {
            try {return (Boolean)Class.forName("com.siirio.jemvisualsandbox.workbench.RaceExperience").getMethod("cast",ServerPlayer.class).invoke(null,player);}
            catch(ReflectiveOperationException failure){throw new IllegalStateException("Final race cast",failure);}
        }
        if (!player.server.isDedicatedServer() || !player.isAlive() || player.isSpectator()) return false;
        return RaceService.record(player).map(record -> signature(player, record)).orElse(false);
    }

    public static int signatureCooldown(RaceId race) {
        return switch (race) {
            case ANCESTORS -> RaceRules.LIFE_CHORUS_COOLDOWN.get();
            case VOID_WALKERS -> RaceRules.RIFT_AMBUSH_COOLDOWN.get();
            case BORN_IN_BLOOM -> RaceRules.VERDANT_DOMINION_COOLDOWN.get();
            case INFECTED_BY_SCULK -> RaceRules.SCULK_VEINS_COOLDOWN.get();
            case NETHER_HEARTEDS -> 1_200;
        };
    }

    /** Read-only native spell preflight; the effect path repeats validation when it actually executes. */
    public static boolean canCastSignature(ServerPlayer player) {
        if (!player.server.isDedicatedServer() || !player.isAlive() || player.isSpectator()) return false;
        return RaceService.record(player).map(record -> {
            long now = player.level().getGameTime();
            if (!record.ready("signature", now)) {
                cooldown(player, record.remaining("signature", now));
                return false;
            }
            return prepareSignature(player, record) != null;
        }).orElse(false);
    }

    private static boolean signature(ServerPlayer player, RaceRecord record) {
        String key = "signature";
        long now = player.level().getGameTime();
        if (!record.ready(key, now)) {
            cooldown(player, record.remaining(key, now));
            syncSignatureCooldown(player);
            return false;
        }
        SignaturePlan plan = prepareSignature(player, record);
        if (plan == null) { syncSignatureCooldown(player); return false; }
        boolean success = switch (record.race()) {
            case ANCESTORS -> lifeChorus(player, record.spellTier());
            case VOID_WALKERS -> riftAmbush(player, record.spellTier(), plan.target(), plan.destination());
            case BORN_IN_BLOOM -> verdantDominion(player, record.spellTier());
            case INFECTED_BY_SCULK -> sculkVeins(player, record.spellTier(), plan);
            case NETHER_HEARTEDS -> infernalEruption(player, record.spellTier(), plan.target());
        };
        if (!success) { syncSignatureCooldown(player); return false; }
        record.cooldown(key, now + signatureCooldown(record.race()));
        record.recordSignatureUse();
        RaceService.mutate(player, ignored -> {});
        RolesNetwork.sendStatus(player, false);
        return true;
    }

    public static void syncSignatureCooldown(ServerPlayer player) {
        if (player.server.isDedicatedServer() && net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks"))
            com.siirio.jemroles.compat.irons.RacialSpellBridge.syncCooldown(player);
    }

    private static SignaturePlan prepareSignature(ServerPlayer player, RaceRecord record) {
        if (record.race() == RaceId.VOID_WALKERS || record.race() == RaceId.NETHER_HEARTEDS) {
            double range = record.race() == RaceId.VOID_WALKERS && record.spellTier() >= 2 ? 32.0D : 24.0D;
            LivingEntity target = target(player, range);
            if (target == null) {
                fail(player, target(player, 64.0D) == null ? "target_required" : "target_too_far");
                return null;
            }
            if (record.race() == RaceId.VOID_WALKERS) {
                if (target instanceof Player && !player.server.isPvpAllowed()) { fail(player, "pvp_disabled"); return null; }
                Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(1.8D));
                BlockPos safe = RaceEnvironment.nearestSafe(player, player.serverLevel(), BlockPos.containing(behind), 3, true).orElse(null);
                if (safe == null) { fail(player, "no_safe_destination"); return null; }
                return new SignaturePlan(target, safe, null, List.of());
            }
            return new SignaturePlan(target, null, null, List.of());
        }
        if (record.race() == RaceId.INFECTED_BY_SCULK) return prepareSculk(player, record.spellTier());
        if (record.race() == RaceId.BORN_IN_BLOOM) {
            double radius = record.spellTier() == 1 ? 6 : record.spellTier() == 2 ? 8 : 10;
            if (player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                    e -> e != player && e.isAlive() && !(e instanceof Animal)).isEmpty()) {
                fail(player, "no_nearby_targets");
                return null;
            }
        }
        return new SignaturePlan(null, null, null, List.of());
    }

    private static void utilityPress(ServerPlayer player, RaceRecord record) {
        long now = player.level().getGameTime();
        switch (record.race()) {
            case VOID_WALKERS -> {
                if (player.isShiftKeyDown()) gravityDenial(player, record, now);
                else {
                    if (!record.ready("void_projectile", now)) { cooldown(player, record.remaining("void_projectile", now)); return; }
                    if (VOID_CHARGE.putIfAbsent(player.getUUID(), now) != null) return;
                    emit(player, RaceVfxId.VOID_PROJECTILE_CHARGE, player.position().add(0.0D, 1.25D, 0.0D),
                            player.position().add(player.getLookAngle().scale(2.0D)).add(0.0D, 1.25D, 0.0D),
                            player.getId(), -1, new int[0], List.of(), record.spellTier(), 30);
                }
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
        if (started == null) return;
        if (!record.ready("void_projectile", now)) { cooldown(player, record.remaining("void_projectile", now)); return; }
        if (now - started < 10) { fail(player, "charge_incomplete"); return; }
        ThrownEnderpearl pearl = new ThrownEnderpearl(player.level(), player);
        pearl.getPersistentData().putBoolean("JemVoidProjectile", true);
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.25F);
        if (!player.level().addFreshEntity(pearl)) { fail(player, "projectile_failed"); return; }
        emit(player, RaceVfxId.VOID_PROJECTILE_RELEASE, player.position().add(0.0D, 1.25D, 0.0D),
                player.position().add(player.getLookAngle().scale(10.0D)).add(0.0D, 1.25D, 0.0D),
                player.getId(), pearl.getId(), new int[0], List.of(), record.spellTier(), 36);
        RaceService.mutate(player, value -> value.cooldown("void_projectile", now + RaceRules.VOID_PROJECTILE_COOLDOWN.get()));
    }

    private static boolean lifeChorus(ServerPlayer player, int tier) {
        double radius = tier >= 2 ? 16.0D : 12.0D;
        List<Integer> affected = new ArrayList<>();
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), LivingEntity::isAlive)) {
            if (target instanceof Animal || target instanceof AbstractVillager || recruit(target)) target.setHealth(target.getMaxHealth());
            else if (target instanceof Player) target.heal(6.0F);
            else continue;
            if (affected.size() < 7) affected.add(target.getId());
            if (tier >= 2) target.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                    .map(MobEffectInstance::getEffect).toList().forEach(target::removeEffect);
            if (tier >= 3 && target.getHealth() >= target.getMaxHealth()) target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        }
        emit(player, RaceVfxId.LIFE_CHORUS, player.position(), player.position(), player.getId(), -1,
                affected.stream().mapToInt(Integer::intValue).toArray(), List.of(), tier, 32);
        return true;
    }

    private static boolean riftAmbush(ServerPlayer player, int tier, LivingEntity target, BlockPos safe) {
        Vec3 departure = player.position();
        Vec3 arrival = Vec3.atBottomCenterOf(safe);
        if (!teleport(player, arrival, target.getYRot())) return false;
        emit(player, RaceVfxId.RIFT_DEPART, departure, arrival, player.getId(), target.getId(),
                new int[0], List.of(), tier, 18);
        if (tier >= 2) spawnVoidEcho(player, departure);
        emit(player, RaceVfxId.RIFT_ARRIVE, arrival, departure, player.getId(), target.getId(),
                new int[0], List.of(), tier, 24);
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
        if (hit > 0) {
            if (tier >= 3) ZONES.add(new Zone(player.serverLevel().dimension(), player.blockPosition(),
                    player.level().getGameTime() + 100, player.getUUID(), ZoneType.GROVE));
            emit(player, RaceVfxId.VERDANT_DOMINION, player.position(), player.position(), player.getId(), -1,
                    rooted.stream().limit(8).mapToInt(LivingEntity::getId).toArray(), List.of(), tier, tier >= 3 ? 100 : 44);
        }
        return hit > 0;
    }

    private static SignaturePlan prepareSculk(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        BlockPos start = nearestSculk(level, player.blockPosition(), 3);
        if (start == null) { fail(player, "sculk_required"); return null; }
        int range = tier == 1 ? 32 : tier == 2 ? 48 : 64;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
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
                if (visited.add(next) && sculk(level, next)) {
                    parent.put(next.immutable(), pos.immutable());
                    queue.addLast(next.immutable());
                }
            }
        }
        BlockPos destination = RaceEnvironment.nearestSafe(player, level, best.above(), 3, true).orElse(null);
        if (destination == null || destination.closerToCenterThan(player.position(), 4.0D)) {
            fail(player, "sculk_exit_required");
            return null;
        }
        return new SignaturePlan(null, destination, start, samplePath(start, best, parent));
    }

    private static boolean sculkVeins(ServerPlayer player, int tier, SignaturePlan plan) {
        Vec3 destination = Vec3.atBottomCenterOf(plan.destination());
        if (!teleport(player, destination, player.getYRot())) return false;
        emit(player, RaceVfxId.SCULK_VEINS, Vec3.atCenterOf(plan.start()), destination,
                player.getId(), -1, new int[0], plan.path(), tier, 40);
        return true;
    }

    private static boolean infernalEruption(ServerPlayer player, int tier, LivingEntity primary) {
        Vec3 line = primary.position().subtract(player.position()).normalize();
        int fissures = tier == 1 ? 1 : 3;
        long now = player.level().getGameTime();
        for (int i = 0; i < fissures; i++) {
            Vec3 center = primary.position().add(line.scale(i * 2.5D));
            int delay = 8 + i * 3;
            ERUPTIONS.add(new Eruption(player.serverLevel().dimension(), center, now + delay, player.getUUID(), tier));
            emit(player, RaceVfxId.INFERNAL_TELEGRAPH, center, center.add(line.scale(2.5D)),
                    player.getId(), primary.getId(), new int[0], List.of(), tier, delay);
        }
        if (tier >= 3) {
            ZONES.add(new Zone(player.serverLevel().dimension(), primary.blockPosition(),
                    player.level().getGameTime() + 100, player.getUUID(), ZoneType.VOLCANIC));
            emit(player, RaceVfxId.VOLCANIC_ZONE, Vec3.atCenterOf(primary.blockPosition()), primary.position(),
                    player.getId(), primary.getId(), new int[0], List.of(), tier, 100);
        }
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
    }

    private static void spawnVoidEcho(ServerPlayer player, Vec3 departure) {
        ArmorStand echo = new ArmorStand(EntityType.ARMOR_STAND, player.serverLevel());
        echo.moveTo(departure.x, departure.y, departure.z, player.getYRot(), player.getXRot());
        echo.setInvisible(true);
        echo.setInvulnerable(true);
        echo.setNoGravity(true);
        echo.setCustomName(Component.literal("Void Echo"));
        echo.setCustomNameVisible(false);
        echo.getPersistentData().putLong("JemVoidEchoExpiry", player.level().getGameTime() + 60);
        if (!player.serverLevel().addFreshEntity(echo)) return;
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, echo.getBoundingBox().inflate(12), entity -> entity instanceof Enemy))
            mob.setTarget(echo);
    }

    public static void serverTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        ERUPTIONS.removeIf(eruption -> {
            ServerLevel level = server.getLevel(eruption.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(eruption.owner);
            if (level == null || owner == null || owner.serverLevel() != level) return true;
            if (now < eruption.impactAt) return false;
            emit(owner, RaceVfxId.INFERNAL_IMPACT, eruption.center, eruption.center.add(0.0D, 4.0D, 0.0D),
                    owner.getId(), -1, new int[0], List.of(), eruption.tier, 24);
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
            return false;
        });
    }

    private static void gravityDenial(ServerPlayer player, RaceRecord record, long now) {
        if (!record.ready("gravity", now)) { cooldown(player, record.remaining("gravity", now)); return; }
        RaceService.mutate(player, value -> {
            value.cooldown("gravity_active", now + RaceRules.GRAVITY_DURATION.get());
            value.cooldown("gravity", now + RaceRules.GRAVITY_COOLDOWN.get());
        });
        emit(player, RaceVfxId.GRAVITY_DENIAL, player.position(), player.position().add(0.0D, 3.0D, 0.0D),
                player.getId(), -1, new int[0], List.of(), record.spellTier(),
                Math.min(200, RaceRules.GRAVITY_DURATION.get()));
    }

    private static void consumeEcho(ServerPlayer player, RaceRecord record) {
        if (record.resonance() < 25) return;
        RaceService.mutate(player, value -> value.resonance(value.resonance() - 25));
        player.heal(6.0F);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
        emit(player, RaceVfxId.ECHO_ABSORB, player.position(), player.position().add(0.0D, 1.2D, 0.0D),
                player.getId(), -1, new int[0], List.of(), record.spellTier(), 28);
    }

    private static void echoVision(ServerPlayer player) {
        List<Integer> revealed = new ArrayList<>();
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(32), entity -> entity != player && entity.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
            if (revealed.size() < 8) revealed.add(target.getId());
        }
        if (revealed.isEmpty()) { fail(player, "no_nearby_entities"); return; }
        emit(player, RaceVfxId.ECHO_VISION, player.position(), player.position(), player.getId(), -1,
                revealed.stream().mapToInt(Integer::intValue).toArray(), List.of(), 1, 36);
    }

    private static void startLavaDrink(ServerPlayer player, RaceRecord record, long now) {
        if (!record.ready("lava_drink", now)) { cooldown(player, record.remaining("lava_drink", now)); return; }
        if (player.isUsingItem()) { fail(player, "already_using_item"); return; }
        InteractionHand hand = player.getMainHandItem().is(Items.LAVA_BUCKET) ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().is(Items.LAVA_BUCKET) ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            player.sendSystemMessage(Component.translatable("jem.roles.lava_drink_requires_bucket"), true);
            return;
        }
        player.startUsingItem(hand);
        emit(player, RaceVfxId.LAVA_DRINK_START, player.position().add(0.0D, 1.2D, 0.0D),
                player.position().add(player.getLookAngle().scale(0.5D)).add(0.0D, 1.4D, 0.0D),
                player.getId(), -1, new int[0], List.of(), record.spellTier(), 32);
    }

    private static boolean recruit(LivingEntity target) {
        String namespace = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace();
        return namespace.equals("recruits") || namespace.equals("workers");
    }

    private static LivingEntity target(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition(), look = player.getViewVector(1.0F), to = from.add(look.scale(range));
        HitResult obstruction = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() != HitResult.Type.MISS) to = obstruction.getLocation();
        Vec3 end = to;
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()).stream()
                .filter(e -> e.getBoundingBox().inflate(0.5D).clip(from, end).isPresent())
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

    private static List<Vec3> samplePath(BlockPos start, BlockPos end, Map<BlockPos, BlockPos> parent) {
        List<BlockPos> reversed = new ArrayList<>();
        BlockPos cursor = end;
        reversed.add(cursor);
        while (!cursor.equals(start) && reversed.size() < 2048) {
            cursor = parent.get(cursor);
            if (cursor == null) break;
            reversed.add(cursor);
        }
        java.util.Collections.reverse(reversed);
        if (reversed.size() <= RaceVfxPacket.MAX_PATH_POINTS)
            return reversed.stream().map(Vec3::atCenterOf).toList();
        List<Vec3> sampled = new ArrayList<>(RaceVfxPacket.MAX_PATH_POINTS);
        for (int i = 0; i < RaceVfxPacket.MAX_PATH_POINTS; i++) {
            int index = (int)Math.round(i * (reversed.size() - 1.0D) / (RaceVfxPacket.MAX_PATH_POINTS - 1.0D));
            sampled.add(Vec3.atCenterOf(reversed.get(index)));
        }
        return List.copyOf(sampled);
    }

    private static void emit(ServerPlayer player, RaceVfxId id, Vec3 origin, Vec3 target,
                             int sourceEntityId, int targetEntityId, int[] relatedEntityIds,
                             List<Vec3> path, int tier, int duration) {
        RaceVfxEmitter.near(player.serverLevel(), new RaceVfxPacket(id,
                player.serverLevel().dimension().location(), origin, target, sourceEntityId, targetEntityId,
                relatedEntityIds, path, Math.max(0, Math.min(3, tier)), Math.max(1, Math.min(200, duration)),
                player.getRandom().nextLong()));
    }

    private static void cooldown(ServerPlayer player, long ticks) {
        player.sendSystemMessage(Component.translatable("jem.roles.cooldown", com.siirio.jemidentity.api.MinecraftDays.format(ticks)), true);
    }

    private static void fail(ServerPlayer player, String reason) {
        player.sendSystemMessage(Component.translatable("jem.roles.ability." + reason), true);
    }

    private static boolean teleport(ServerPlayer player, Vec3 destination, float yaw) {
        // This overload returns the result of the existing hosted-arena teleport gate.
        // A gate may redirect instead of deny, so only the actual requested arrival commits the ability.
        boolean moved = player.teleportTo(player.serverLevel(), destination.x, destination.y, destination.z,
                Set.of(), yaw, player.getXRot());
        if (!moved || player.position().distanceToSqr(destination) > 0.0001D) {
            fail(player, "teleport_blocked");
            return false;
        }
        return true;
    }

    public static void logout(ServerPlayer player) { VOID_CHARGE.remove(player.getUUID()); }

    private enum ZoneType { GROVE, VOLCANIC }
    private record SignaturePlan(LivingEntity target, BlockPos destination, BlockPos start, List<Vec3> path) {}
    private record Eruption(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 center,
                            long impactAt, UUID owner, int tier) {}
    private record Zone(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos center,
                        long expires, UUID owner, ZoneType type) {}
}
