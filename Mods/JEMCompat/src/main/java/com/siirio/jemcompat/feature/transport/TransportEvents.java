package com.siirio.jemcompat.feature.transport;

import com.siirio.jemcompat.mixin.transport.EntityStuckSpeedAccessor;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class TransportEvents {
    private static final int TICKS_PER_SECOND = 20;
    private static final double INPUT_EPSILON = 0.001;
    private static final int SUBTERRANODON_FLIGHT_LIMIT = 90 * TICKS_PER_SECOND;
    private static final int SUBTERRANODON_RESET_TIME = 20 * TICKS_PER_SECOND;
    private static final int CANDICORN_FAST_DURATION = 5 * TICKS_PER_SECOND;
    private static final int CANDICORN_RECOVERY_TIME = 10 * TICKS_PER_SECOND;
    private static final double CANDICORN_FAST_SPEED = 14.0;
    private static final double SUBTERRANODON_SPEED = 22.0;
    private static final double SUBTERRANODON_EXHAUSTED_SPEED = 14.0;
    private static final double SKELETON_HORSE_WATER_SPEED = 8.0;
    private static final double ARMORED_WAGON_PROJECTILE_MULTIPLIER = 0.65;
    private static final double NETHERITE_GOLEM_KNOCKBACK_MULTIPLIER = 0.5;
    private static final double LLAMA_CATCHUP_DISTANCE_SQUARED = 36.0;
    private static final double LLAMA_CATCHUP_BONUS = 0.30;
    private static final UUID TERRAIN_SPEED_MODIFIER_ID = UUID.fromString("6a218b65-49a0-49f1-838b-eefcd5ca647e");
    private static final String SUBTERRANODON_FLIGHT_KEY = "JemSubterranodonFlightTicks";
    private static final String SUBTERRANODON_GROUND_KEY = "JemSubterranodonGroundTicks";
    private static final String PIG_BOOST_COOLDOWN_KEY = "JemPigBoostCooldown";
    private static final String CANDICORN_FAST_TICKS_KEY = "JemCandicornFastTicks";
    private static final String CANDICORN_RECOVERY_KEY = "JemCandicornRecoveryTicks";
    private static final Map<ResourceLocation, Double> FIXED_SPEEDS = Map.ofEntries(
            speed("minecraft:happy_ghast", 8.0),
            speed("alexscaves:tremorsaurus", 9.0),
            speed("alexscaves:tremorzilla", 7.0),
            speed("alexsmobs:elephant", 8.0),
            speed("alexsmobs:straddleboard", 14.0),
            speed("alexsmobs:laviathan", 10.0),
            speed("golemoverhaul:netherite_golem", 5.0),
            speed("legendary_monsters:skeloraptor", 13.0),
            speed("whaleborne:hullback", 7.0),
            speed("siegeweapons:small_horse_cart", 8.0),
            speed("siegeweapons:transport_cart", 6.5),
            speed("immersive_aircraft:biplane", 26.0),
            speed("smallships:cog", 8.0),
            speed("smallships:galley", 10.0),
            speed("smallships:brigg", 9.0),
            speed("smallships:drakkar", 11.0)
    );

    private TransportEvents() {
    }

    private static Map.Entry<ResourceLocation, Double> speed(String id, double blocksPerSecond) {
        return Map.entry(new ResourceLocation(id), blocksPerSecond);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null || vehicle.getControllingPassenger() != player) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
        if (id == null) {
            return;
        }
        if (id.equals(new ResourceLocation("alexscaves", "subterranodon"))) {
            tickSubterranodon(vehicle);
            return;
        }
        if (id.equals(new ResourceLocation("alexscaves", "candicorn"))) {
            tickCandicorn(vehicle, player);
            return;
        }
        double speed = conditionalSpeed(id, vehicle);
        if (speed > 0.0) {
            capHorizontalSpeed(vehicle, speed);
        }
        if (vehicle instanceof SkeletonHorse && vehicle.isInWater() && hasMovementInput(player)) {
            ensureMinimumHorizontalSpeed(vehicle, player, SKELETON_HORSE_WATER_SPEED);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (is(entity, "alexscaves:subterranodon")) {
            tickGroundedSubterranodon(entity);
        }
        if (entity instanceof Pig) {
            tickCooldown(entity, PIG_BOOST_COOLDOWN_KEY);
        }
        Entity passenger = entity.getControllingPassenger();
        if (passenger instanceof ServerPlayer && is(entity, "cnc:caribou")
                && entity.level().getBlockState(entity.blockPosition()).is(Blocks.POWDER_SNOW)) {
            ((EntityStuckSpeedAccessor) entity).jemcompat$setStuckSpeedMultiplier(Vec3.ZERO);
        }
        double bonus = entity instanceof Llama llama ? llamaCatchupBonus(llama)
                : passenger instanceof ServerPlayer ? terrainSpeedBonus(entity) : 0.0;
        updateTerrainModifier(entity, bonus);
    }

    @SubscribeEvent
    public static void onPassengerHurt(LivingHurtEvent event) {
        Entity vehicle = event.getEntity().getVehicle();
        if (vehicle != null && is(vehicle, "trotting_wagons:armored_wagon") && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount((float) (event.getAmount() * ARMORED_WAGON_PROJECTILE_MULTIPLIER));
        }
    }

    @SubscribeEvent
    public static void onMountedKnockback(LivingKnockBackEvent event) {
        Entity vehicle = event.getEntity().getVehicle();
        if (vehicle != null && is(vehicle, "golemoverhaul:netherite_golem")) {
            event.setStrength((float) (event.getStrength() * NETHERITE_GOLEM_KNOCKBACK_MULTIPLIER));
        }
    }

    private static void tickSubterranodon(Entity entity) {
        var data = entity.getPersistentData();
        if (!entity.onGround()) {
            data.putInt(SUBTERRANODON_GROUND_KEY, 0);
            data.putInt(SUBTERRANODON_FLIGHT_KEY, data.getInt(SUBTERRANODON_FLIGHT_KEY) + 1);
        }
        boolean exhausted = data.getInt(SUBTERRANODON_FLIGHT_KEY) >= SUBTERRANODON_FLIGHT_LIMIT;
        if (exhausted && entity.getDeltaMovement().y > 0.0) {
            Vec3 movement = entity.getDeltaMovement();
            entity.setDeltaMovement(movement.x, 0.0, movement.z);
        }
        capHorizontalSpeed(entity, exhausted ? SUBTERRANODON_EXHAUSTED_SPEED : SUBTERRANODON_SPEED);
    }

    private static void tickGroundedSubterranodon(Entity entity) {
        if (!entity.onGround()) {
            entity.getPersistentData().putInt(SUBTERRANODON_GROUND_KEY, 0);
            return;
        }
        var data = entity.getPersistentData();
        int grounded = data.getInt(SUBTERRANODON_GROUND_KEY) + 1;
        data.putInt(SUBTERRANODON_GROUND_KEY, grounded);
        if (grounded >= SUBTERRANODON_RESET_TIME) {
            data.putInt(SUBTERRANODON_FLIGHT_KEY, 0);
        }
    }

    private static void tickCandicorn(Entity entity, ServerPlayer player) {
        var data = entity.getPersistentData();
        int recovery = data.getInt(CANDICORN_RECOVERY_KEY);
        if (recovery > 0) {
            data.putInt(CANDICORN_RECOVERY_KEY, recovery - 1);
            player.setSprinting(false);
            entity.setSprinting(false);
            return;
        }
        if (!player.isSprinting() && !entity.isSprinting()) {
            data.putInt(CANDICORN_FAST_TICKS_KEY, 0);
            return;
        }
        int fastTicks = data.getInt(CANDICORN_FAST_TICKS_KEY) + 1;
        data.putInt(CANDICORN_FAST_TICKS_KEY, fastTicks);
        capHorizontalSpeed(entity, CANDICORN_FAST_SPEED);
        if (fastTicks >= CANDICORN_FAST_DURATION) {
            data.putInt(CANDICORN_FAST_TICKS_KEY, 0);
            data.putInt(CANDICORN_RECOVERY_KEY, CANDICORN_RECOVERY_TIME);
            player.setSprinting(false);
            entity.setSprinting(false);
        }
    }

    private static double conditionalSpeed(ResourceLocation id, Entity entity) {
        Double fixed = FIXED_SPEEDS.get(id);
        if (fixed != null) {
            return fixed;
        }
        String key = id.toString();
        return switch (key) {
            case "aquamirae:shellback" -> entity.isInWater() ? 10.0 : 5.0;
            case "blastfromthepast:speartooth" -> isSnowOrIce(entity) ? 12.0 : 9.0;
            case "primal:walrus" -> entity.isInWater() ? 11.0 : isIce(entity) ? 10.0 : 6.0;
            case "spawn:seal" -> entity.isInWater() ? 12.0 : 4.0;
            case "alexscaves:submarine" -> entity.isUnderWater() ? 9.0 : 5.0;
            case "snowyspirit:sled" -> isSnowOrIce(entity) ? 14.0 : 6.0;
            default -> -1.0;
        };
    }

    private static double terrainSpeedBonus(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return 0.0;
        }
        return switch (id.toString()) {
            case "luminous_nether:glider" -> entity.level().dimension() == Level.NETHER ? 0.25 : 0.0;
            case "alexsmobs:endergrade" -> entity.level().dimension() == Level.END ? 0.25 : 0.0;
            case "alexsmobs:komodo_dragon" -> isSwamp(entity) ? 0.20 : 0.0;
            case "blastfromthepast:frostomper" -> isSnowOrIce(entity) ? 0.30 : -0.10;
            case "cnc:caribou" -> isSnowOrIce(entity) ? 0.25 : 0.0;
            case "netherexp:stampede" -> entity.level().dimension() == Level.NETHER ? 0.20 : 0.0;
            case "minecraft:mule", "vinery:mule" -> 0.10;
            case "unearthed_journey:canoe" -> isShallowOrRiver(entity) ? 0.20 : -0.10;
            default -> 0.0;
        };
    }

    private static void updateTerrainModifier(LivingEntity entity, double amount) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier current = speed.getModifier(TERRAIN_SPEED_MODIFIER_ID);
        if (current != null && Double.compare(current.getAmount(), amount) != 0) {
            speed.removeModifier(TERRAIN_SPEED_MODIFIER_ID);
            current = null;
        }
        if (amount != 0.0 && current == null) {
            speed.addTransientModifier(new AttributeModifier(TERRAIN_SPEED_MODIFIER_ID, "JEM transport terrain speed",
                    amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (amount == 0.0 && current != null) {
            speed.removeModifier(TERRAIN_SPEED_MODIFIER_ID);
        }
    }

    private static double llamaCatchupBonus(Llama llama) {
        Llama leader = llama.getCaravanHead();
        return leader != null && llama.distanceToSqr(leader) > LLAMA_CATCHUP_DISTANCE_SQUARED
                ? LLAMA_CATCHUP_BONUS : 0.0;
    }

    private static void capHorizontalSpeed(Entity entity, double blocksPerSecond) {
        Vec3 movement = entity.getDeltaMovement();
        double horizontal = movement.horizontalDistance();
        double maximum = blocksPerSecond / TICKS_PER_SECOND;
        if (horizontal > maximum) {
            double scale = maximum / horizontal;
            entity.setDeltaMovement(movement.x * scale, movement.y, movement.z * scale);
            entity.hasImpulse = true;
        }
    }

    private static void ensureMinimumHorizontalSpeed(Entity entity, ServerPlayer player, double blocksPerSecond) {
        Vec3 movement = entity.getDeltaMovement();
        double minimum = blocksPerSecond / TICKS_PER_SECOND;
        if (movement.horizontalDistanceSqr() >= minimum * minimum) {
            return;
        }
        Vec3 direction = Vec3.directionFromRotation(0.0F, player.getYRot());
        entity.setDeltaMovement(direction.x * minimum, movement.y, direction.z * minimum);
        entity.hasImpulse = true;
    }

    private static boolean hasMovementInput(ServerPlayer player) {
        return Math.abs(player.xxa) > INPUT_EPSILON || Math.abs(player.zza) > INPUT_EPSILON;
    }

    private static boolean isSnowOrIce(Entity entity) {
        return isIce(entity) || entity.level().getBlockState(entity.blockPosition().below()).is(Blocks.SNOW)
                || entity.level().getBlockState(entity.blockPosition().below()).is(Blocks.SNOW_BLOCK)
                || entity.level().getBlockState(entity.blockPosition().below()).is(Blocks.POWDER_SNOW);
    }

    private static boolean isIce(Entity entity) {
        var state = entity.level().getBlockState(entity.blockPosition().below());
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    private static boolean isSwamp(Entity entity) {
        var biome = entity.level().getBiome(entity.blockPosition());
        return biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    private static boolean isShallowOrRiver(Entity entity) {
        var biome = entity.level().getBiome(entity.blockPosition());
        if (biome.is(Biomes.RIVER) || biome.is(Biomes.FROZEN_RIVER)) {
            return true;
        }
        for (int depth = 1; depth <= 3; depth++) {
            if (entity.level().getFluidState(entity.blockPosition().below(depth)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean is(Entity entity, String id) {
        return new ResourceLocation(id).equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    private static void tickCooldown(Entity entity, String key) {
        int cooldown = entity.getPersistentData().getInt(key);
        if (cooldown > 0) {
            entity.getPersistentData().putInt(key, cooldown - 1);
        }
    }

}
