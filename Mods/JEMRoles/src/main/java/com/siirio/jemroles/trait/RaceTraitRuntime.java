package com.siirio.jemroles.trait;

import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.network.RaceVfxPacket;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceRules;
import com.siirio.jemroles.race.RaceService;
import com.siirio.jemroles.vfx.RaceVfxEmitter;
import com.siirio.jemroles.vfx.RaceVfxId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.ForgeMod;

public final class RaceTraitRuntime {
    private static final UUID UNSTABLE_HEALTH = UUID.fromString("65cc4bf4-fc6c-43be-9d9b-23b7bb8f51d6");
    private static final UUID SCULK_HEALTH = UUID.fromString("5f85aeb5-778c-4a22-a108-cde26538ff58");
    private static final UUID SCULK_ATTACK = UUID.fromString("0e3e3509-88db-437c-81dd-5b62c4124950");
    private static final UUID BLOOM_BARREN = UUID.fromString("2e2f5209-0a0e-4ccb-aee4-7a92c93bf26b");
    private static final UUID SCULK_SUN = UUID.fromString("593bd676-c3dd-4dd3-856c-734c380013fb");
    private static final UUID NETHER_FROZEN = UUID.fromString("e17185a7-99e8-427b-9710-02973629d149");
    private static final UUID WATER_STIFFNESS = UUID.fromString("25a887ba-2e0c-4b4f-9b3f-149137cbab7f");
    private static final UUID GRAVITY_DENIAL = UUID.fromString("2b29cf59-9d68-47bb-9367-c123d6b7bd67");
    private static final Map<UUID, Guardian> GUARDIANS = new ConcurrentHashMap<>();
    private static final Map<UUID, MerchantDiscount> MERCHANT_DISCOUNTS = new ConcurrentHashMap<>();

    private RaceTraitRuntime() {}

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.server.isDedicatedServer()) return;
        RaceService.record(player).ifPresent(record -> {
            if (record.race() == RaceId.BORN_IN_BLOOM && record.livingPath()) placePath(player);
            if (record.race() == RaceId.VOID_WALKERS) voidState(player, record);
            if (record.race() == RaceId.NETHER_HEARTEDS) lavaMovement(player);
            if (record.race() == RaceId.ANCESTORS && player.getHealth() <= player.getMaxHealth() * 0.40F) guardian(player, record);
            if (player.tickCount % 20 != 0) return;
            applyState(player, record);
            if (!player.isSpectator() && player.isAlive()) RaceService.mutate(player, value -> value.addActiveTicks(20L));
            com.siirio.jemroles.network.RolesNetwork.sendStatus(player, false);
        });
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 5 == 0) RaceAbilities.serverTick(event.getServer());
        if (event.getServer().getTickCount() % 20 != 0) return;
        TemporaryPathSavedData.get(event.getServer()).tick(event.getServer());
        long now = event.getServer().overworld().getGameTime();
        GUARDIANS.entrySet().removeIf(entry -> {
            Guardian state = entry.getValue();
            Entity entity = null;
            for (ServerLevel level : event.getServer().getAllLevels()) {
                entity = level.getEntity(state.entity());
                if (entity != null) break;
            }
            if (!(entity instanceof IronGolem golem) || !golem.isAlive() || state.expires() <= now) {
                if (entity instanceof IronGolem golem && golem.isAlive() && state.expires() <= now) {
                    ServerPlayer owner = event.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (owner != null) emit(owner, RaceVfxId.GUARDIAN_DISSOLVE, golem.position(), golem.position().add(0.0D, 2.7D, 0.0D),
                            owner.getId(), golem.getId(), new int[0], List.of(), 1, 24);
                }
                if (entity != null) entity.discard();
                return true;
            }
            var enemy = golem.level().getEntitiesOfClass(Mob.class, golem.getBoundingBox().inflate(16), mob -> mob instanceof Enemy)
                    .stream().min(java.util.Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
            if (enemy != null) golem.setTarget(enemy);
            return false;
        });
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.decoration.ArmorStand stand)) return;
        long expiry = stand.getPersistentData().getLong("JemVoidEchoExpiry");
        if (expiry > 0L && expiry <= stand.level().getGameTime()) stand.discard();
    }

    private static void applyState(ServerPlayer player, RaceRecord record) {
        RaceId race = record.race();
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        remove(player, Attributes.MAX_HEALTH, UNSTABLE_HEALTH, SCULK_HEALTH, BLOOM_BARREN, SCULK_SUN, NETHER_FROZEN);
        remove(player, Attributes.ATTACK_DAMAGE, SCULK_ATTACK);
        remove(player, Attributes.MOVEMENT_SPEED, WATER_STIFFNESS);
        remove(player, ForgeMod.ENTITY_GRAVITY.get(), GRAVITY_DENIAL);
        if (race == RaceId.VOID_WALKERS) {
            add(player, Attributes.MAX_HEALTH, UNSTABLE_HEALTH, "JEM unstable body", -6.0D, AttributeModifier.Operation.ADDITION);
            if (record.cooldown("gravity_active") > player.level().getGameTime())
                add(player, ForgeMod.ENTITY_GRAVITY.get(), GRAVITY_DENIAL, "JEM gravity denial", -0.75D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else if (race == RaceId.BORN_IN_BLOOM) {
            boolean green = RaceEnvironment.green(level, pos);
            if (!green) add(player, Attributes.MAX_HEALTH, BLOOM_BARREN, "JEM severed roots", -8.0D, AttributeModifier.Operation.ADDITION);
            else if (RaceEnvironment.naturalDaylight(level, pos)) {
                float before = player.getHealth();
                player.heal(0.5F);
                if (player.getHealth() > before) emit(player, RaceVfxId.PHOTOSYNTHESIS, player.position(),
                        player.position().add(0.0D, 1.5D, 0.0D), player.getId(), -1,
                        new int[0], List.of(), record.spellTier(), 24);
                player.getFoodData().setExhaustion(Math.max(0.0F, player.getFoodData().getExhaustionLevel() - 1.0F));
            }
        } else if (race == RaceId.INFECTED_BY_SCULK) {
            if (RaceEnvironment.underground(level, pos)) {
                add(player, Attributes.MAX_HEALTH, SCULK_HEALTH, "JEM subterranean health", 10.0D, AttributeModifier.Operation.ADDITION);
                add(player, Attributes.ATTACK_DAMAGE, SCULK_ATTACK, "JEM subterranean attack", 0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            if (RaceEnvironment.naturalDaylight(level, pos))
                add(player, Attributes.MAX_HEALTH, SCULK_SUN, "JEM sunlight decay", -8.0D, AttributeModifier.Operation.ADDITION);
        } else if (race == RaceId.NETHER_HEARTEDS) {
            netherState(player, record);
            if (level.getBiome(pos).value().coldEnoughToSnow(pos))
                add(player, Attributes.MAX_HEALTH, NETHER_FROZEN, "JEM frozen core", -8.0D, AttributeModifier.Operation.ADDITION);
        } else if (race == RaceId.ANCESTORS) {
            if (player.getHealth() <= player.getMaxHealth() * 0.40F) guardian(player, record);
            for (Raider raider : level.getEntitiesOfClass(Raider.class, player.getBoundingBox().inflate(48), Entity::isAlive)) {
                if (raider.getTarget() == null || raider.distanceToSqr(player) < raider.distanceToSqr(raider.getTarget())) raider.setTarget(player);
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void voidState(ServerPlayer player, RaceRecord record) {
        ServerLevel level = player.serverLevel();
        if (RaceEnvironment.safe(player, level, player.blockPosition()) && !player.isInWater()) SafeStore.put(player, level, player.blockPosition());
        if (player.getY() < level.getMinBuildHeight() + 1) {
            BlockPos safe = SafeStore.get(player, level);
            if (safe != null && RaceEnvironment.safe(player, level, safe))
                player.teleportTo(level, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, player.getYRot(), player.getXRot());
        }
        if (player.isInWater() && !waterProtected(player)) {
            if (record.waterExposure() == 0) {
                player.hurt(player.damageSources().drown(), 1.0F);
                BlockPos dry = SafeStore.get(player, level);
                if (dry == null || !dry.closerToCenterThan(player.position(), 100.0D) || !RaceEnvironment.safe(player, level, dry)) {
                    dry = RaceEnvironment.nearestDrySurface(player, level, player.blockPosition(), 32)
                            .or(() -> RaceEnvironment.nearestSafe(player, level, player.blockPosition(), 16, true)).orElse(null);
                }
                if (dry != null) {
                    player.teleportTo(level, dry.getX() + 0.5D, dry.getY(), dry.getZ() + 0.5D, player.getYRot(), player.getXRot());
                    level.playSound(null, dry, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            RaceService.mutate(player, value -> value.waterExposure(value.waterExposure() + 20));
        } else if (record.waterExposure() > 0) RaceService.mutate(player, value -> value.waterExposure(0));
    }

    private static boolean waterProtected(ServerPlayer player) {
        if (player.isPassenger() || player.hasEffect(MobEffects.WATER_BREATHING) || player.hasEffect(MobEffects.CONDUIT_POWER)) return true;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER, player) > 0
                || net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.RESPIRATION, player) > 0;
    }

    private static void netherState(ServerPlayer player, RaceRecord record) {
        boolean lava = player.isInLava();
        if (lava) {
            int exposure = record.lavaExposure() + 20;
            RaceService.mutate(player, value -> value.lavaExposure(exposure));
        } else if (record.lavaExposure() > 0) RaceService.mutate(player, value -> value.lavaExposure(0));
        int stiffness = player.isInWater() && !player.isPassenger() ? Math.min(140, record.waterExposure() + 20) : Math.max(0, record.waterExposure() - 10);
        if (player.isInWater()) {
            RaceService.mutate(player, value -> value.waterExposure(stiffness));
            add(player, Attributes.MOVEMENT_SPEED, WATER_STIFFNESS, "JEM quenched heart", -0.70D * stiffness / 140.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else if (record.waterExposure() > 0) RaceService.mutate(player, value -> value.waterExposure(stiffness));
        if (record.waterExposure() == 0 && stiffness > 0 || record.waterExposure() > 0 && stiffness == 0)
            emit(player, RaceVfxId.WATER_STEAM, player.position(), player.position().add(0.0D, 1.8D, 0.0D),
                    player.getId(), -1, new int[0], List.of(), record.spellTier(), 28);
    }

    private static void lavaMovement(ServerPlayer player) {
        player.clearFire();
        if (!player.isInLava()) {
            if (player.isSwimming() && !player.isInWater()) player.setSwimming(false);
            return;
        }
        player.setSwimming(true);
        Vec3 motion = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        double speed = player.isSprinting() ? 0.085D : 0.055D;
        Vec3 propelled = motion.scale(0.82D).add(look.x * speed, look.y * speed + 0.025D, look.z * speed);
        player.setDeltaMovement(propelled.x, Math.max(-0.25D, Math.min(0.32D, propelled.y)), propelled.z);
        player.hurtMarked = true;
        if (player.tickCount % 10 == 0 && propelled.lengthSqr() > 0.0004D)
            emit(player, RaceVfxId.LAVA_SWIM, player.position(), player.position().subtract(propelled.normalize().scale(1.6D)),
                    player.getId(), -1, new int[0], List.of(), 1, 18);
    }

    private static void guardian(ServerPlayer player, RaceRecord record) {
        long now = player.level().getGameTime();
        if (GUARDIANS.containsKey(player.getUUID()) || !record.ready("guardian", now)) return;
        IronGolem golem = new IronGolem(net.minecraft.world.entity.EntityType.IRON_GOLEM, player.level());
        golem.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        golem.setPlayerCreated(true);
        golem.setInvulnerable(true);
        golem.setCustomName(Component.translatable("jem.roles.guardian"));
        golem.getPersistentData().putBoolean("JemAncestralGuardian", true);
        if (player.level().addFreshEntity(golem)) {
            GUARDIANS.put(player.getUUID(), new Guardian(golem.getUUID(), now + 600));
            RaceService.mutate(player, value -> value.cooldown("guardian", now + RaceRules.GUARDIAN_COOLDOWN.get()));
            emit(player, RaceVfxId.GUARDIAN_FORM, golem.position(), golem.position().add(0.0D, 2.7D, 0.0D),
                    player.getId(), golem.getId(), new int[0], List.of(), record.spellTier(), 30);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !RaceApi.hasRace(player, RaceId.NETHER_HEARTEDS)
                || !event.getSource().is(DamageTypeTags.IS_FIRE)) return;
        event.setCanceled(true);
        player.clearFire();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("JemAncestralGuardian")) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        RaceId race = RaceApi.get(player).map(com.siirio.jemroles.api.RaceSnapshot::race).orElse(null);
        if (race == null) return;
        if (race == RaceId.BORN_IN_BLOOM && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 2.0F);
        if (race == RaceId.NETHER_HEARTEDS && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setCanceled(true);
        if (race == RaceId.INFECTED_BY_SCULK && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.25F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RaceApi.hasRace(player, RaceId.BORN_IN_BLOOM)) {
            RaceService.record(player).ifPresent(record -> {
                long now = player.level().getGameTime();
                if (RaceEnvironment.green(player.serverLevel(), player.blockPosition()) && record.ready("second_blossom", now)
                        && player.getY() > player.serverLevel().getMinBuildHeight()) {
                    event.setCanceled(true);
                    player.setHealth(player.getMaxHealth() * 0.5F);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                    RaceService.mutate(player, value -> value.cooldown("second_blossom", now + RaceRules.SECOND_BLOSSOM_COOLDOWN.get()));
                    emit(player, RaceVfxId.SECOND_BLOSSOM, player.position(), player.position().add(0.0D, 2.2D, 0.0D),
                            player.getId(), -1, new int[0], List.of(), record.spellTier(), 38);
                }
            });
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || event.getEntity() instanceof ServerPlayer) return;
        RaceService.record(killer).ifPresent(record -> {
            if (record.race() == RaceId.INFECTED_BY_SCULK && event.getEntity() instanceof Enemy) {
                int before = record.resonance();
                int after = before + 10;
                RaceService.mutate(killer, value -> value.resonance(after));
                if (crossed(before, after, 25) || crossed(before, after, 50) || crossed(before, after, 75))
                    emit(killer, RaceVfxId.RESONANCE_THRESHOLD, killer.position(), killer.position().add(0.0D, 1.2D, 0.0D),
                            killer.getId(), -1, new int[0], List.of(), record.spellTier(), 22);
            }
            if (record.race() == RaceId.ANCESTORS && (event.getEntity() instanceof Animal || event.getEntity() instanceof AbstractVillager))
                RaceService.mutate(killer, value -> value.cooldown("guardian", killer.level().getGameTime() + RaceRules.GUARDIAN_COOLDOWN.get()));
        });
    }

    @SubscribeEvent
    public static void experience(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("JemAncestralGuardian")) {
            event.setDroppedExperience(0);
            return;
        }
        if (event.getAttackingPlayer() instanceof ServerPlayer player && RaceApi.hasRace(player, RaceId.INFECTED_BY_SCULK))
            event.setDroppedExperience((int)Math.floor(event.getDroppedExperience() * 1.15D));
    }

    @SubscribeEvent
    public static void containerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getContainer() instanceof MerchantMenu menu)
                || !RaceApi.hasRace(player, RaceId.ANCESTORS)) return;
        AbstractVillager merchant = player.serverLevel().getEntitiesOfClass(AbstractVillager.class,
                player.getBoundingBox().inflate(8.0D), candidate -> candidate.getTradingPlayer() == player)
                .stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (merchant == null) return;
        java.util.List<Integer> original = new java.util.ArrayList<>();
        boolean discountedAny = false;
        for (var offer : merchant.getOffers()) {
            original.add(offer.getSpecialPriceDiff());
            ItemStack cost = offer.getCostA();
            if (!cost.is(Items.EMERALD) || offer.getResult().is(Items.EMERALD)) continue;
            int discounted = Math.max(1, (int)Math.ceil(cost.getCount() * 0.80D));
            offer.addToSpecialPriceDiff(discounted - cost.getCount());
            discountedAny |= discounted < cost.getCount();
        }
        MERCHANT_DISCOUNTS.put(player.getUUID(), new MerchantDiscount(merchant.getUUID(), original));
        player.sendMerchantOffers(menu.containerId, merchant.getOffers(), menu.getTraderLevel(),
                merchant.getVillagerXp(), merchant.showProgressBar(), merchant.canRestock());
        if (discountedAny) emit(player, RaceVfxId.MERCHANT_BLESSING, merchant.position(), player.position(),
                player.getId(), merchant.getId(), new int[0], List.of(), 1, 22);
    }

    @SubscribeEvent
    public static void containerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) restoreMerchantDiscount(player);
    }

    @SubscribeEvent
    public static void finishItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItem().is(Items.LAVA_BUCKET)
                || !RaceApi.hasRace(player, RaceId.NETHER_HEARTEDS)) return;
        long now = player.level().getGameTime();
        RaceService.record(player).ifPresent(record -> {
            if (!record.ready("lava_drink", now)) return;
            event.setResultStack(new ItemStack(Items.BUCKET));
            player.heal(5.0F);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F);
            RaceService.mutate(player, value -> value.cooldown("lava_drink", now + RaceRules.LAVA_DRINK_COOLDOWN.get()));
            com.siirio.jemroles.network.RolesNetwork.sendStatus(player, false);
            emit(player, RaceVfxId.LAVA_DRINK_COMPLETE, player.position().add(0.0D, 1.2D, 0.0D), player.position(),
                    player.getId(), -1, new int[0], List.of(), record.spellTier(), 24);
        });
    }

    @SubscribeEvent
    public static void guardianDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("JemAncestralGuardian")) event.getDrops().clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer player)) return;
        RaceId race = RaceApi.get(player).map(com.siirio.jemroles.api.RaceSnapshot::race).orElse(null);
        if (race == null) return;
        LivingEntity actor = event.getEntity();
        boolean protectedTarget = race == RaceId.VOID_WALKERS && endKin(actor) && !endBoss(actor)
                || race == RaceId.BORN_IN_BLOOM && RaceEnvironment.green(player.serverLevel(), player.blockPosition()) && !(actor instanceof EnderMan)
                || race == RaceId.NETHER_HEARTEDS && actor instanceof AbstractPiglin;
        if (protectedTarget) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void projectile(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownEnderpearl pearl) || !pearl.getPersistentData().getBoolean("JemVoidProjectile")
                || !(pearl.getOwner() instanceof ServerPlayer player)) return;
        event.setCanceled(true);
        BlockPos impact = BlockPos.containing(event.getRayTraceResult().getLocation());
        BlockPos safe = RaceEnvironment.nearestSafe(player, player.serverLevel(), impact, 5, true).orElse(null);
        Vec3 impactPosition = event.getRayTraceResult().getLocation();
        emit(player, RaceVfxId.VOID_PROJECTILE_IMPACT, impactPosition, impactPosition,
                player.getId(), pearl.getId(), new int[0], List.of(), 1, 22);
        if (safe != null && RaceApi.hasRace(player, RaceId.VOID_WALKERS)) {
            Vec3 destination = Vec3.atBottomCenterOf(safe);
            emit(player, RaceVfxId.VOID_RESCUE, impactPosition, destination,
                    player.getId(), -1, new int[0], List.of(impactPosition, destination), 1, 32);
            player.teleportTo(player.serverLevel(), safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, player.getYRot(), player.getXRot());
        }
        pearl.discard();
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().is(Blocks.OAK_LEAVES)
                && TemporaryPathSavedData.get(level.getServer()).contains(level.dimension(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreMerchantDiscount(player);
            RaceAbilities.logout(player);
        }
    }

    private static void placePath(ServerPlayer player) {
        Vec3 horizontal = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        if (horizontal.lengthSqr() < 0.0025D) horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        horizontal = horizontal.lengthSqr() < 0.0001D ? Vec3.ZERO : horizontal.normalize();
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        java.util.List<Vec3> placed = new java.util.ArrayList<>();
        for (int forward = 0; forward <= 2; forward++) for (int lateral = -1; lateral <= 1; lateral++) {
            Vec3 projected = player.position().add(horizontal.scale(forward)).add(side.scale(lateral));
            BlockPos below = BlockPos.containing(projected.x, player.getY(), projected.z).below();
            if (placePathBlock(player, below) && placed.size() < RaceVfxPacket.MAX_PATH_POINTS)
                placed.add(Vec3.atCenterOf(below));
        }
        if (!placed.isEmpty() && player.tickCount % 4 == 0)
            emit(player, RaceVfxId.LIVING_PATH_STEP, player.position(), placed.get(placed.size() - 1),
                    player.getId(), -1, new int[0], placed, 1, 18);
    }

    private static boolean placePathBlock(ServerPlayer player, BlockPos below) {
        if (!player.level().getBlockState(below).isAir() || !player.level().getFluidState(below).isEmpty()) return false;
        player.level().setBlock(below, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
        TemporaryPathSavedData.get(player.getServer()).add(player.level().dimension(), below, player.level().getGameTime() + 800);
        return true;
    }

    private static void restoreMerchantDiscount(ServerPlayer player) {
        MerchantDiscount session = MERCHANT_DISCOUNTS.remove(player.getUUID());
        if (session == null) return;
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity found = level.getEntity(session.merchant());
            if (!(found instanceof AbstractVillager merchant)) continue;
            for (int i = 0; i < merchant.getOffers().size() && i < session.specialPrices().size(); i++) {
                var offer = merchant.getOffers().get(i);
                offer.resetSpecialPriceDiff();
                offer.addToSpecialPriceDiff(session.specialPrices().get(i));
            }
            break;
        }
    }

    private static boolean crossed(int before, int after, int threshold) {
        return before < threshold && after >= threshold;
    }

    private static void emit(ServerPlayer player, RaceVfxId id, Vec3 origin, Vec3 target,
                             int sourceEntityId, int targetEntityId, int[] relatedEntityIds,
                             List<Vec3> path, int tier, int duration) {
        RaceVfxEmitter.near(player.serverLevel(), new RaceVfxPacket(id,
                player.serverLevel().dimension().location(), origin, target, sourceEntityId, targetEntityId,
                relatedEntityIds, path, Math.max(0, Math.min(3, tier)), Math.max(1, Math.min(200, duration)),
                player.getRandom().nextLong()));
    }

    private static void remove(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID... ids) {
        var instance = player.getAttribute(attribute);
        if (instance != null) for (UUID id : ids) instance.removeModifier(id);
    }

    private static void add(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name,
                            double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance != null) instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }

    private static boolean endKin(LivingEntity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return RaceRules.END_KIN_ENTITIES.get().contains(id);
    }

    private static boolean endBoss(LivingEntity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return entity instanceof EnderDragon || RaceRules.END_BOSS_ENTITIES.get().contains(id);
    }

    private record Guardian(UUID entity, long expires) {}
    private record MerchantDiscount(UUID merchant, java.util.List<Integer> specialPrices) {}

    private static final class SafeStore {
        private static String key(ServerLevel level) { return "JemRaceSafe_" + level.dimension().location().toString().replace(':', '_'); }
        static void put(ServerPlayer player, ServerLevel level, BlockPos pos) { player.getPersistentData().putLong(key(level), pos.asLong()); }
        static BlockPos get(ServerPlayer player, ServerLevel level) {
            String key = key(level);
            return player.getPersistentData().contains(key) ? BlockPos.of(player.getPersistentData().getLong(key)) : null;
        }
    }
}
