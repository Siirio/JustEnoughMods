package com.siirio.jemvillagertalking.speech;

import com.mojang.logging.LogUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public final class VillagerReactionTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TRACK_INTERVAL_TICKS = 20;
    private static final int MAX_VILLAGERS_PER_LEVEL = 24;
    private static final int TRACK_RADIUS = 64;
    private static final int PERCEPTION_REARM_TICKS = 100;
    private static final int AMBIENT_MIN_TICKS = 600;
    private static final int AMBIENT_VARIANCE_TICKS = 600;
    private static final int STILL_STATE_TICKS = 200;
    private static final int JOB_SITE_COMPLAINT_DELAY_TICKS = 600;
    private static final int BLOCK_SCAN_RADIUS = 4;
    private static final int BRIGHT_LIGHT_LEVEL = 14;
    private static final int DARK_LIGHT_LEVEL = 7;
    private static final int CUSTOM_BOSS_HEALTH = 100;
    private static final int CROWDED_VILLAGERS = 12;
    private static final String STATE_KNOWN_PREFIX = "JemSpeechStateKnown.";
    private static final String STATE_VALUE_PREFIX = "JemSpeechStateValue.";
    private static final String STATE_REPEAT_PREFIX = "JemSpeechStateRepeat.";
    private static final String MOTION_KNOWN_TAG = "JemSpeechMotionKnown";
    private static final String MOTION_VALUE_TAG = "JemSpeechMotionValue";
    private static final Set<String> WORLD_STATE_EVENTS = Set.of(
            "clear_after_rain", "clear_weather", "evening", "morning", "night", "noon", "rain", "snow", "sunset"
    );
    private static final Set<String> SOCIAL_SCENES = Set.of(
            "agreement", "argument", "awkward_chat", "awkward_silence", "complaint", "compliment", "gossip",
            "gossip_villager", "greeting", "joke", "meeting", "parent_family_chat", "price_chat", "rumor", "smalltalk", "tease",
            "two_villagers_comment", "villager_discusses_prices", "work_chat"
    );
    private static final Map<MinecraftServer, TrackingState> STATES = new IdentityHashMap<>();

    private VillagerReactionTracker() {
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % TRACK_INTERVAL_TICKS != 0L) {
            return;
        }
        TrackingState state = STATES.computeIfAbsent(level.getServer(), ignored -> new TrackingState());
        List<Villager> villagers = nearbyVillagers(level);
        trackWorldStates(level, state, now);
        for (ServerPlayer player : level.players()) {
            trackPlayerStates(level, player, state, now);
        }
        for (Villager villager : villagers) {
            trackVillagerStates(level, villager, state, now);
            trackPerception(level, villager, state, now);
            scheduleAmbient(level, villager, state, now);
        }
        state.expirePerception(now);
        state.removeMissing(level, villagers, level.players());
    }

    public static void clear(MinecraftServer server) {
        STATES.remove(server);
    }

    private static List<Villager> nearbyVillagers(ServerLevel level) {
        Set<Villager> villagers = new LinkedHashSet<>();
        for (ServerPlayer player : level.players()) {
            villagers.addAll(level.getEntitiesOfClass(
                    Villager.class,
                    player.getBoundingBox().inflate(TRACK_RADIUS),
                    Villager::isAlive
            ));
            if (villagers.size() >= MAX_VILLAGERS_PER_LEVEL) {
                break;
            }
        }
        return villagers.stream().limit(MAX_VILLAGERS_PER_LEVEL).toList();
    }

    private static void trackWorldStates(ServerLevel level, TrackingState state, long now) {
        ServerPlayer observer = level.players().stream().findFirst().orElse(null);
        if (observer == null) {
            return;
        }
        long dayTime = level.getDayTime() % 24000L;
        track(state, level, observer, "clear_after_rain", level.isRaining(), now);
        track(state, level, observer, "clear_weather", !level.isRaining() && !level.isThundering(), now);
        track(state, level, observer, "rain", level.isRaining() && !coldEnoughToSnow(level, observer.blockPosition()), now);
        track(state, level, observer, "snow", level.isRaining() && coldEnoughToSnow(level, observer.blockPosition()), now);
        track(state, level, observer, "morning", dayTime < 1000L, now);
        track(state, level, observer, "noon", dayTime >= 5500L && dayTime < 6500L, now);
        track(state, level, observer, "sunset", dayTime >= 11500L && dayTime < 12500L, now);
        track(state, level, observer, "evening", dayTime >= 12000L && dayTime < 13500L, now);
        track(state, level, observer, "night", dayTime >= 13000L && dayTime < 23000L, now);
    }

    private static void trackPlayerStates(ServerLevel level, ServerPlayer player, TrackingState state, long now) {
        PlayerMotion motion = state.playerMotion.computeIfAbsent(player.getUUID(), ignored -> new PlayerMotion(player.blockPosition(), now));
        if (!motion.position.equals(player.blockPosition()) || player.getDeltaMovement().lengthSqr() > 0.0001D) {
            motion.position = player.blockPosition();
            motion.stillSince = now;
        }
        track(state, level, player, "creative_flight", player.isCreative() && player.getAbilities().flying, now);
        track(state, level, player, "player_enters_creative", player.isCreative(), now);
        track(state, level, player, "player_enters_spectator", player.isSpectator(), now);
        track(state, level, player, "spectator_flight", player.isSpectator() && player.getAbilities().flying, now);
        track(state, level, player, "player_switches_survival", !player.isCreative() && !player.isSpectator(), now);
        track(state, level, player, "player_afk_long", now - motion.stillSince >= STILL_STATE_TICKS * 3L, now);
        track(state, level, player, "player_freezes_motionless", now - motion.stillSince >= STILL_STATE_TICKS, now);
        track(state, level, player, "player_glowing", player.hasEffect(MobEffects.GLOWING), now);
        track(state, level, player, "player_invisible", player.isInvisible(), now);
        track(state, level, player, "player_poisoned", player.hasEffect(MobEffects.POISON), now);
        track(state, level, player, "player_strength", player.hasEffect(MobEffects.DAMAGE_BOOST), now);
        track(state, level, player, "player_has_bad_omen", player.hasEffect(MobEffects.BAD_OMEN), now);
        track(state, level, player, "player_has_fire_resistance", player.hasEffect(MobEffects.FIRE_RESISTANCE), now);
        track(state, level, player, "player_has_hero_effect", player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), now);
        track(state, level, player, "player_has_invisibility", player.hasEffect(MobEffects.INVISIBILITY), now);
        track(state, level, player, "player_has_nausea", player.hasEffect(MobEffects.CONFUSION), now);
        track(state, level, player, "player_has_speed", player.hasEffect(MobEffects.MOVEMENT_SPEED), now);
    }

    private static void trackVillagerStates(ServerLevel level, Villager villager, TrackingState state, long now) {
        PlayerMotion motion = state.villagerMotion.computeIfAbsent(villager.getUUID(), ignored -> new PlayerMotion(villager.blockPosition(), now));
        if (!motion.position.equals(villager.blockPosition()) || villager.getDeltaMovement().lengthSqr() > 0.0001D) {
            motion.position = villager.blockPosition();
            motion.stillSince = now;
        }
        boolean danger = !level.getEntitiesOfClass(
                LivingEntity.class,
                villager.getBoundingBox().inflate(24),
                entity -> entity instanceof Enemy && entity.isAlive()
        ).isEmpty();
        boolean barrier = nearbyBlock(level, villager.blockPosition(), block -> registryPath(block).contains("barrier"));
        boolean lightOn = level.getMaxLocalRawBrightness(villager.blockPosition()) > DARK_LIGHT_LEVEL;
        boolean stuck = villager.getNavigation().isStuck() || now - motion.stillSince >= STILL_STATE_TICKS;
        boolean employed = villager.getVillagerData().getProfession() != VillagerProfession.NONE
                && villager.getVillagerData().getProfession() != VillagerProfession.NITWIT;
        boolean jobSiteBlocked = employed
                && villager.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)
                && villager.getNavigation().isStuck();
        int solidSides = solidHorizontalSides(level, villager.blockPosition());
        track(state, level, villager, "danger", danger, now);
        track(state, level, villager, "danger_ended", danger, now);
        track(state, level, villager, "custom_barrier_appears", barrier, now);
        track(state, level, villager, "custom_barrier_disappears", barrier, now);
        track(state, level, villager, "custom_raid_started", level.getRaidAt(villager.blockPosition()) != null, now);
        track(state, level, villager, "raid_starts", level.getRaidAt(villager.blockPosition()) != null, now);
        track(state, level, villager, "raid_wall_appears", barrier && level.getRaidAt(villager.blockPosition()) != null, now);
        track(state, level, villager, "light_turns_off", lightOn, now);
        track(state, level, villager, "light_turns_on", !lightOn, now);
        track(state, level, villager, "too_many_lights", level.getMaxLocalRawBrightness(villager.blockPosition()) >= BRIGHT_LIGHT_LEVEL, now);
        track(state, level, villager, "path_blocked", stuck, now);
        track(state, level, villager, "pathfinding_fail", villager.getNavigation().isStuck(), now);
        track(state, level, villager, "villager_path_loop", stuck, now);
        track(state, level, villager, "villager_stuck", stuck, now);
        track(state, level, villager, "villager_stuck_corner", stuck && solidSides >= 2, now);
        track(state, level, villager, "villager_trapped_one_block", solidSides >= 4, now);
        track(state, level, villager, "villager_enclosed", solidSides >= 3, now);
        track(state, level, villager, "villager_in_glass_box", enclosedBy(level, villager.blockPosition(), block -> registryPath(block).contains("glass")), now);
        track(state, level, villager, "villager_roof_trapped", stuck && !level.getBlockState(villager.blockPosition().above(2)).isAir(), now);
        track(state, level, villager, "villager_underground_cell",
                !level.canSeeSky(villager.blockPosition()) && villager.getY() < level.getSeaLevel() - 8, now);
        track(state, level, villager, "bed_missing", !hasNearbyBlock(level, villager.blockPosition(), BedBlock.class), now);
        track(state, level, villager, "campfire_smokes_house",
                level.isVillage(villager.blockPosition()) && hasNearbyBlock(level, villager.blockPosition(), CampfireBlock.class), now);
        track(state, level, villager, "door_stuck", stuck && hasNearbyBlock(level, villager.blockPosition(), DoorBlock.class), now);
        track(state, level, villager, "fence_blocks_exit", stuck && hasNearbyBlock(level, villager.blockPosition(), FenceBlock.class), now);
        track(state, level, villager, "trapdoor_blocks_exit", stuck && hasNearbyBlock(level, villager.blockPosition(), TrapDoorBlock.class), now);
        trackDelayedJobSite(level, villager, state, jobSiteBlocked, now);
        track(state, level, villager, "modded_machine_too_loud",
                nearbyBlock(level, villager.blockPosition(), block -> !registryId(block).getNamespace().equals("minecraft")
                        && machineLike(registryId(block).getPath())), now);
        track(state, level, villager, "village_expands", level.isVillage(villager.blockPosition()), now);
        track(state, level, villager, "pillager_crossbow",
                !level.getEntitiesOfClass(Pillager.class, villager.getBoundingBox().inflate(24), Entity::isAlive).isEmpty(), now);
        trackEffect(state, level, villager, "villager_gets_invisibility", MobEffects.INVISIBILITY, now);
        trackEffect(state, level, villager, "villager_gets_jump_boost", MobEffects.JUMP, now);
        trackEffect(state, level, villager, "villager_gets_levitation", MobEffects.LEVITATION, now);
        trackEffect(state, level, villager, "villager_gets_night_vision", MobEffects.NIGHT_VISION, now);
        trackEffect(state, level, villager, "villager_gets_poison", MobEffects.POISON, now);
        trackEffect(state, level, villager, "villager_gets_regeneration", MobEffects.REGENERATION, now);
        trackEffect(state, level, villager, "villager_gets_resistance", MobEffects.DAMAGE_RESISTANCE, now);
        trackEffect(state, level, villager, "villager_gets_slowness", MobEffects.MOVEMENT_SLOWDOWN, now);
        trackEffect(state, level, villager, "villager_gets_speed", MobEffects.MOVEMENT_SPEED, now);
        trackEffect(state, level, villager, "villager_gets_weakness", MobEffects.WEAKNESS, now);
    }

    private static void trackDelayedJobSite(
            ServerLevel level,
            Villager villager,
            TrackingState state,
            boolean blocked,
            long now
    ) {
        if (!blocked) {
            state.jobSiteBlockedSince.remove(villager.getUUID());
            track(state, level, villager, "job_site_blocked", false, now);
            return;
        }
        long blockedSince = state.jobSiteBlockedSince.computeIfAbsent(villager.getUUID(), ignored -> now);
        track(state, level, villager, "job_site_blocked", now - blockedSince >= JOB_SITE_COMPLAINT_DELAY_TICKS, now);
    }

    private static void trackEffect(
            TrackingState state,
            ServerLevel level,
            Villager villager,
            String eventId,
            MobEffect effect,
            long now
    ) {
        track(state, level, villager, eventId, villager.hasEffect(effect), now);
    }

    private static void track(
            TrackingState state,
            ServerLevel level,
            Entity subject,
            String eventId,
            boolean current,
            long now
    ) {
        VillagerSpeechCatalog.EventDefinition event = VillagerSpeechCatalog.event(eventId);
        if (event == null) {
            return;
        }
        boolean worldState = WORLD_STATE_EVENTS.contains(event.id());
        StateKey key = new StateKey(
                level.dimension().location().toString(),
                worldState ? UUID.nameUUIDFromBytes(level.dimension().location().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)) : subject.getUUID(),
                event.id()
        );
        Boolean previous;
        if (worldState) {
            previous = state.values.put(key, current);
        } else {
            String knownTag = STATE_KNOWN_PREFIX + event.id();
            previous = subject.getPersistentData().getBoolean(knownTag)
                    ? subject.getPersistentData().getBoolean(STATE_VALUE_PREFIX + event.id())
                    : null;
            subject.getPersistentData().putBoolean(knownTag, true);
            subject.getPersistentData().putBoolean(STATE_VALUE_PREFIX + event.id(), current);
        }
        long nextRepeat = worldState
                ? state.nextSparseRepeat.getOrDefault(key, 0L)
                : subject.getPersistentData().getLong(STATE_REPEAT_PREFIX + event.id());
        boolean emit = switch (event.activation().mode()) {
            case STATE_ENTER -> previous != null && !previous && current;
            case STATE_EXIT -> previous != null && previous && !current;
            case STATE_ENTER_WITH_SPARSE_REPEAT -> previous != null && current && (!previous
                    || nextRepeat <= now);
            default -> false;
        };
        if (event.activation().mode() == VillagerSpeechCatalog.ActivationMode.STATE_ENTER_WITH_SPARSE_REPEAT && current) {
            if (worldState) {
                state.nextSparseRepeat.put(key, now + event.cooldowns().eventTicks());
            } else {
                subject.getPersistentData().putLong(STATE_REPEAT_PREFIX + event.id(), now + event.cooldowns().eventTicks());
            }
        }
        if (emit) {
            LOGGER.debug(
                    "[REACTION] event={} activation={} previous={} current={} transition=EMIT",
                    event.id(),
                    event.activation().mode(),
                    previous,
                    current
            );
            VillagerReactionRegistry.emitLocal(
                    event.id(),
                    "state:" + key.subjectId() + ":" + now,
                    level,
                    subject.blockPosition(),
                    subject,
                    subject instanceof Player ? subject : null,
                    ""
            );
        } else if (current && previous != null && previous && now % 100L == 0L) {
            LOGGER.debug(
                    "[REACTION] event={} activation={} previous=true current=true event not emitted reason=STATE_ALREADY_TRUE",
                    event.id(),
                    event.activation().mode()
            );
        }
    }

    private static void trackPerception(ServerLevel level, Villager villager, TrackingState state, long now) {
        List<Entity> subjects = level.getEntities(
                villager,
                villager.getBoundingBox().inflate(TRACK_RADIUS),
                Entity::isAlive
        ).stream().sorted(Comparator.comparingDouble(villager::distanceToSqr)).toList();
        for (Entity subject : subjects) {
            trackSemanticMotion(level, subject, now);
        }
        for (VillagerSpeechCatalog.EventDefinition event : VillagerSpeechCatalog.events()) {
            if (event.activation().mode() != VillagerSpeechCatalog.ActivationMode.PERCEPTION_ENTER) {
                continue;
            }
            Entity subject = perceptionSubject(event.id(), level, villager, subjects);
            if (subject == null) {
                continue;
            }
            int radius = event.perception().distanceOr(TRACK_RADIUS);
            if (villager.distanceToSqr(subject) > radius * radius) {
                continue;
            }
            if (event.perception().requiresLineOfSight() && subject != villager && !villager.hasLineOfSight(subject)) {
                continue;
            }
            PerceptionKey key = new PerceptionKey(villager.getUUID(), event.id(), subject.getUUID());
            Long lastSeen = state.perceptions.put(key, now);
            if (lastSeen == null || now - lastSeen > PERCEPTION_REARM_TICKS) {
                VillagerReactionRegistry.emitLocal(
                        event.id(),
                        "perception:" + villager.getUUID() + ":" + subject.getUUID() + ":" + now,
                        level,
                        subject.blockPosition(),
                        subject,
                        subject instanceof Player ? subject : null,
                        "first"
                );
            }
        }
    }

    private static void trackSemanticMotion(ServerLevel level, Entity subject, long now) {
        ResourceLocation id = registryId(subject);
        String path = id.getPath();
        String eventId = path.contains("contraption") ? "create_contraption_moves"
                : !id.getNamespace().equals("minecraft") && machineLike(path) ? "modded_machine_starts"
                : null;
        if (eventId == null) {
            return;
        }
        boolean moving = subject.getDeltaMovement().lengthSqr() > 0.0001D;
        boolean known = subject.getPersistentData().getBoolean(MOTION_KNOWN_TAG);
        boolean previous = subject.getPersistentData().getBoolean(MOTION_VALUE_TAG);
        subject.getPersistentData().putBoolean(MOTION_KNOWN_TAG, true);
        subject.getPersistentData().putBoolean(MOTION_VALUE_TAG, moving);
        if (moving && (!known || !previous)) {
            VillagerReactionRegistry.emitLocal(
                    eventId,
                    "motion:" + subject.getUUID() + ":" + now,
                    level,
                    subject.blockPosition(),
                    subject,
                    null,
                    ""
            );
        }
    }

    private static Entity perceptionSubject(
            String eventId,
            ServerLevel level,
            Villager villager,
            List<Entity> subjects
    ) {
        ServerPlayer player = subjects.stream().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).findFirst().orElse(null);
        int players = (int) subjects.stream().filter(Player.class::isInstance).count();
        int villagers = (int) subjects.stream().filter(Villager.class::isInstance).count() + 1;
        if (eventId.equals("crowded_trade_hall")) return villagers >= CROWDED_VILLAGERS ? villager : null;
        if (eventId.equals("large_player_group_arrives") || eventId.equals("three_players_arrive")) return players >= 3 ? player : null;
        if (eventId.equals("players_surround_villager")) return players >= 3 ? player : null;
        if (eventId.equals("players_all_stare")) return players >= 2 && subjects.stream().filter(Player.class::isInstance)
                .map(Player.class::cast).allMatch(candidate -> lookingAt(candidate, villager)) ? player : null;
        if (eventId.equals("player_camera_stares")) return player != null && lookingAt(player, villager) ? player : null;
        if (eventId.equals("players_arguing_nearby") || eventId.equals("players_trade_nearby")) return players >= 2 ? player : null;
        if (eventId.equals("players_mine_nearby") || eventId.equals("player_attacks_mob_nearby")) return null;
        if (eventId.equals("raid_near_village")) return level.getRaidAt(villager.blockPosition()) != null ? villager : null;
        if (eventId.equals("near_house")) return level.isVillage(villager.blockPosition()) ? villager : null;
        if (eventId.equals("near_bell") || eventId.equals("meeting_point")) return nearbyBlock(level, villager.blockPosition(), block -> block == Blocks.BELL) ? villager : null;
        if (eventId.equals("near_farm")) return nearbyBlock(level, villager.blockPosition(), block -> block == Blocks.FARMLAND) ? villager : null;
        if (eventId.equals("near_well")) return nearbyBlock(level, villager.blockPosition(), block -> block == Blocks.WATER) ? villager : null;
        if (eventId.equals("custom_currency_seen")) return heldSemanticItem(player, "coin", "currency", "token");
        if (eventId.equals("custom_eye_item_seen")) return heldSemanticItem(player, "eye");
        if (eventId.equals("emerald_seen")) return player != null && player.getMainHandItem().is(Items.EMERALD) ? player : null;
        if (eventId.equals("illager_banner_seen")) return player != null && registryPath(player.getMainHandItem()).contains("banner") ? player : null;
        for (Entity subject : subjects) {
            if (matchesEntityEvent(eventId, villager, subject, level)) {
                return subject;
            }
        }
        return null;
    }

    private static boolean matchesEntityEvent(String eventId, Villager villager, Entity subject, ServerLevel level) {
        if (eventId.equals("custom_boss_nearby")) {
            return subject instanceof Enemy && subject instanceof LivingEntity living
                    && living.getMaxHealth() >= CUSTOM_BOSS_HEALTH
                    && !registryId(subject).getNamespace().equals("minecraft");
        }
        if (eventId.equals("child_danger")) return villager.isBaby() && subject instanceof Enemy;
        if (eventId.equals("baby_villager_near_cat")) return villager.isBaby() && subject instanceof Cat;
        if (eventId.equals("child_bee_nearby")) return villager.isBaby() && subject instanceof Bee bee && !bee.isAngry();
        if (eventId.equals("bee_angry_nearby")) return subject instanceof Bee bee && bee.isAngry();
        if (eventId.equals("child_frog_nearby")) return villager.isBaby() && registryPath(subject).equals("frog");
        if (eventId.equals("child_rabbit_nearby")) return villager.isBaby() && registryPath(subject).equals("rabbit");
        if (eventId.equals("golem_near_child")) return villager.isBaby() && subject instanceof IronGolem;
        if (eventId.equals("cat_on_bed")) return subject instanceof Cat && hasNearbyBlock(level, subject.blockPosition(), BedBlock.class);
        if (eventId.equals("chicken_in_house")) return registryPath(subject).equals("chicken") && level.isVillage(subject.blockPosition());
        if (eventId.equals("pig_in_path")) return registryPath(subject).equals("pig") && subject.distanceToSqr(villager) <= 9.0D;
        if (eventId.equals("witch_near_hut")) return registryPath(subject).equals("witch");
        String target = entityTarget(eventId);
        if (target.isBlank()) {
            return false;
        }
        String path = registryPath(subject);
        if (target.equals("donkey")) return subject instanceof AbstractChestedHorse;
        if (target.equals("golem")) return subject instanceof IronGolem;
        return path.equals(target);
    }

    private static String entityTarget(String eventId) {
        String value = eventId;
        for (String suffix : List.of("_near_village", "_nearby", "_seen")) {
            if (value.endsWith(suffix)) {
                value = value.substring(0, value.length() - suffix.length());
                break;
            }
        }
        return switch (value) {
            case "allay", "armadillo", "axolotl", "bee", "bogged", "breeze", "camel", "cat", "chicken",
                    "cow", "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "enderman",
                    "endermite", "evoker", "fox", "frog", "glow_squid", "goat", "golem", "guardian",
                    "happy_ghast", "hoglin", "horse", "llama", "magma_cube", "mooshroom", "panda", "parrot",
                    "phantom", "pig", "piglin", "pillager", "polar_bear", "rabbit", "ravager", "sheep",
                    "shulker", "silverfish", "skeleton", "slime", "sniffer", "spider", "strider", "turtle",
                    "vex", "vindicator", "warden", "witch", "wolf", "zoglin", "zombie" -> value;
            default -> "";
        };
    }

    private static void scheduleAmbient(ServerLevel level, Villager villager, TrackingState state, long now) {
        long readyAt = state.nextAmbient.getOrDefault(villager.getUUID(), 0L);
        if (readyAt > now) {
            return;
        }
        String eventId = ambientEvent(level, villager);
        if (eventId != null) {
            VillagerReactionRegistry.emitLocal(
                    eventId,
                    "ambient:" + villager.getUUID() + ":" + now,
                    level,
                    villager.blockPosition(),
                    villager,
                    null,
                    ""
            );
        }
        state.nextAmbient.put(villager.getUUID(), now + AMBIENT_MIN_TICKS
                + Math.floorMod(villager.getUUID().hashCode() + (int) now, AMBIENT_VARIANCE_TICKS));
    }

    private static String ambientEvent(ServerLevel level, Villager villager) {
        if (villager.isBaby()) {
            return level.random.nextBoolean() ? "child_family_chat" : "child_playing";
        }
        long time = level.getDayTime() % 24000L;
        if (time >= 12000L && time < 13500L && level.random.nextBoolean()) {
            return "evening_chat";
        }
        ResourceLocation profession = net.minecraftforge.registries.ForgeRegistries.VILLAGER_PROFESSIONS
                .getKey(villager.getVillagerData().getProfession());
        if (profession != null && time >= 2000L && time < 9000L) {
            String work = profession.getPath() + "_work_comment";
            if (VillagerSpeechCatalog.event(work) != null) {
                return work;
            }
        }
        List<Villager> partners = level.getEntitiesOfClass(
                Villager.class,
                villager.getBoundingBox().inflate(8),
                candidate -> candidate != villager && candidate.isAlive()
        );
        if (!partners.isEmpty()) {
            List<String> scenes = SOCIAL_SCENES.stream().sorted().toList();
            return scenes.get(level.random.nextInt(scenes.size()));
        }
        return villager.getVillagerData().getProfession() == VillagerProfession.NITWIT
                ? "nitwit_work_comment"
                : villager.getVillagerData().getProfession() == VillagerProfession.NONE
                ? "unemployed_work_comment"
                : "odd_behavior";
    }

    private static Entity heldSemanticItem(ServerPlayer player, String... tokens) {
        if (player == null) {
            return null;
        }
        String path = registryPath(player.getMainHandItem());
        for (String token : tokens) {
            if (path.contains(token)) {
                return player;
            }
        }
        return null;
    }

    private static boolean lookingAt(Player player, Entity target) {
        var direction = player.getLookAngle().normalize();
        var toTarget = target.getEyePosition().subtract(player.getEyePosition()).normalize();
        return direction.dot(toTarget) > 0.96D && player.hasLineOfSight(target);
    }

    private static boolean nearbyBlock(ServerLevel level, BlockPos center, java.util.function.Predicate<Block> predicate) {
        for (BlockPos position : BlockPos.betweenClosed(
                center.offset(-BLOCK_SCAN_RADIUS, -2, -BLOCK_SCAN_RADIUS),
                center.offset(BLOCK_SCAN_RADIUS, 2, BLOCK_SCAN_RADIUS)
        )) {
            if (predicate.test(level.getBlockState(position).getBlock())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyBlock(ServerLevel level, BlockPos center, Class<? extends Block> type) {
        return nearbyBlock(level, center, type::isInstance);
    }

    private static boolean enclosedBy(ServerLevel level, BlockPos center, java.util.function.Predicate<Block> predicate) {
        for (BlockPos position : List.of(center.north(), center.south(), center.east(), center.west(), center.above())) {
            if (!predicate.test(level.getBlockState(position).getBlock())) {
                return false;
            }
        }
        return true;
    }

    private static int solidHorizontalSides(ServerLevel level, BlockPos center) {
        int solid = 0;
        for (BlockPos position : List.of(center.north(), center.south(), center.east(), center.west())) {
            if (!level.getBlockState(position).getCollisionShape(level, position).isEmpty()) {
                solid++;
            }
        }
        return solid;
    }

    private static boolean coldEnoughToSnow(ServerLevel level, BlockPos position) {
        return level.getBiome(position).value().coldEnoughToSnow(position);
    }

    private static boolean machineLike(String path) {
        return path.contains("machine") || path.contains("contraption") || path.contains("engine")
                || path.contains("press") || path.contains("mixer") || path.contains("crusher");
    }

    private static ResourceLocation registryId(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }

    private static ResourceLocation registryId(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id == null ? new ResourceLocation("minecraft", "unknown") : id;
    }

    private static String registryPath(Block block) {
        return registryId(block).getPath();
    }

    private static String registryPath(Entity entity) {
        return registryId(entity).getPath();
    }

    private static String registryPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    private record StateKey(String dimension, UUID subjectId, String eventId) {
    }

    private record PerceptionKey(UUID villagerId, String eventId, UUID subjectId) {
    }

    private static final class PlayerMotion {
        private BlockPos position;
        private long stillSince;

        private PlayerMotion(BlockPos position, long stillSince) {
            this.position = position;
            this.stillSince = stillSince;
        }
    }

    private static final class TrackingState {
        private final Map<StateKey, Boolean> values = new HashMap<>();
        private final Map<StateKey, Long> nextSparseRepeat = new HashMap<>();
        private final Map<PerceptionKey, Long> perceptions = new HashMap<>();
        private final Map<UUID, Long> nextAmbient = new HashMap<>();
        private final Map<UUID, PlayerMotion> playerMotion = new HashMap<>();
        private final Map<UUID, PlayerMotion> villagerMotion = new HashMap<>();
        private final Map<UUID, Long> jobSiteBlockedSince = new HashMap<>();

        private void expirePerception(long now) {
            Iterator<Map.Entry<PerceptionKey, Long>> iterator = perceptions.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue() > PERCEPTION_REARM_TICKS) {
                    iterator.remove();
                }
            }
        }

        private void removeMissing(ServerLevel level, List<Villager> villagers, List<ServerPlayer> players) {
            Set<UUID> visibleVillagers = villagers.stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            Set<UUID> visiblePlayers = players.stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            villagerMotion.keySet().removeIf(id -> !visibleVillagers.contains(id));
            jobSiteBlockedSince.keySet().removeIf(id -> !visibleVillagers.contains(id));
            nextAmbient.keySet().removeIf(id -> !visibleVillagers.contains(id));
            playerMotion.keySet().removeIf(id -> !visiblePlayers.contains(id));
            String dimension = level.dimension().location().toString();
            values.keySet().removeIf(key -> key.dimension().equals(dimension)
                    && !WORLD_STATE_EVENTS.contains(key.eventId())
                    && !visibleVillagers.contains(key.subjectId()) && !visiblePlayers.contains(key.subjectId()));
            nextSparseRepeat.keySet().retainAll(values.keySet());
        }
    }
}
