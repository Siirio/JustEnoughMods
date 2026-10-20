package com.siirio.jemworldbosstiers.diagnostic;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/** Explicit, bounded observations of the native LivingEntity.hurt boundary; never changes combat. */
public final class CombatProbe {
    private static final int MAX_SAMPLES = 1024;
    private static Session active;
    private static final Deque<Frame> frames = new ArrayDeque<>();

    private CombatProbe() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jemcombat")
                .requires(source -> source.hasPermission(2) && source.getServer().isDedicatedServer())
                .then(Commands.literal("start").then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                .executes(context -> start(context.getSource(),
                                        EntityArgument.getEntity(context, "target"),
                                        IntegerArgumentType.getInteger(context, "seconds"))))))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource()))));
    }

    private static int start(CommandSourceStack source, Entity entity, int seconds) {
        if (active != null) {
            source.sendFailure(Component.literal("A capture is active; stop it before starting another."));
            return 0;
        }
        if (!(entity instanceof LivingEntity target)) {
            source.sendFailure(Component.literal("Select one living combat target."));
            return 0;
        }
        active = new Session(source.getServer(), target, seconds);
        source.sendSuccess(() -> Component.literal("Combat capture started for " + target.getUUID()
                + ": " + seconds + " seconds, at most " + MAX_SAMPLES + " hurt calls. No combat state changed."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(active == null ? "No combat capture active."
                : "Capturing target " + active.target + "; samples=" + active.samples.size()
                + ", remaining ticks=" + Math.max(0, active.endTick - active.server.getTickCount())), false);
        return active == null ? 0 : 1;
    }

    private static int stop(CommandSourceStack source) {
        if (active == null) {
            source.sendFailure(Component.literal("No combat capture active."));
            return 0;
        }
        return save(source, "operator");
    }

    /** Called only on the dedicated server thread, before native hit processing. */
    public static void beginHit(LivingEntity target, DamageSource source, float requested) {
        Session session = active;
        if (session == null || session.saveFailed || !session.target.equals(target.getUUID())) return;
        boolean nested = !frames.isEmpty();
        JsonObject row = new JsonObject();
        row.addProperty("tick", session.server.getTickCount() - session.startTick);
        row.addProperty("requested_at_living_hurt_entry", finite(requested));
        row.addProperty("damage_type", source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("unregistered"));
        row.add("attacker", identity(source.getEntity()));
        row.add("direct_entity", identity(source.getDirectEntity()));
        if (source.getEntity() instanceof LivingEntity attacker) row.add("attacker_loadout", loadout(attacker));
        row.add("target_loadout", loadout(target));
        row.addProperty("world_tier", WorldTierData.get(session.server).tier());
        row.addProperty("dimension", target.level().dimension().location().toString());
        row.addProperty("difficulty", target.level().getDifficulty().getSerializedName());
        row.addProperty("nested_in_observed_target_hit", nested);
        row.addProperty("health_before", finite(target.getHealth()));
        row.addProperty("absorption_before", finite(target.getAbsorptionAmount()));
        row.addProperty("invulnerable_time_before", target.invulnerableTime);
        frames.push(new Frame(session, target.getUUID(), target.getHealth(), target.getAbsorptionAmount(), nested, row));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void hurtStage(LivingHurtEvent event) {
        if (!frames.isEmpty() && frames.peek().target.equals(event.getEntity().getUUID())) {
            frames.peek().row.addProperty("living_hurt_event_amount_at_lowest", finite(event.getAmount()));
            frames.peek().row.addProperty("living_hurt_event_cancelled_at_lowest", event.isCanceled());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void damageStage(LivingDamageEvent event) {
        if (!frames.isEmpty() && frames.peek().target.equals(event.getEntity().getUUID())) {
            frames.peek().row.addProperty("living_damage_event_amount_at_lowest", finite(event.getAmount()));
            frames.peek().row.addProperty("living_damage_event_cancelled_at_lowest", event.isCanceled());
        }
    }

    /** Return value and actual net pool losses are deliberately distinct observations. */
    public static void finishHit(LivingEntity target, boolean returned) {
        if (frames.isEmpty() || !frames.peek().target.equals(target.getUUID())) return;
        Frame frame = frames.pop();
        if (frame.session != active) return;
        double healthLoss = Math.max(0, frame.health - target.getHealth());
        double absorptionLoss = Math.max(0, frame.absorption - target.getAbsorptionAmount());
        frame.row.addProperty("hurt_returned", returned);
        frame.row.addProperty("health_after", finite(target.getHealth()));
        frame.row.addProperty("absorption_after", finite(target.getAbsorptionAmount()));
        frame.row.addProperty("net_health_loss", finite(healthLoss));
        frame.row.addProperty("net_absorption_loss", finite(absorptionLoss));
        frame.row.addProperty("alive_after", target.isAlive());
        frame.row.addProperty("invulnerable_time_after", target.invulnerableTime);
        if (active.samples.size() < MAX_SAMPLES) active.samples.add(frame.row);
        else active.truncated = true;
        if (Float.isFinite(frame.health) && Float.isFinite(frame.absorption)
                && Float.isFinite(target.getHealth()) && Float.isFinite(target.getAbsorptionAmount()))
            active.totals.observe(frame.nested, returned, frame.health, target.getHealth(), frame.absorption, target.getAbsorptionAmount());
        else active.invalidEvidence = true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null || active.saveFailed) return;
        if (!frames.isEmpty()) {
            active.invalidEvidence = true;
            frames.clear();
            save(active.server.createCommandSourceStack(), "unpaired_hurt_boundary");
            return;
        }
        if (active.server.getTickCount() >= active.endTick || active.samples.size() >= MAX_SAMPLES) {
            save(active.server.createCommandSourceStack(), active.samples.size() >= MAX_SAMPLES ? "sample_limit" : "duration");
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        if (active != null && active.server == event.getServer())
            save(active.server.createCommandSourceStack(), "server_stopping");
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        active = null;
        frames.clear();
    }

    private static int save(CommandSourceStack source, String reason) {
        Session session = active;
        if (session == null) return 0;
        int ticks = Math.max(0, session.server.getTickCount() - session.startTick);
        JsonObject result = new JsonObject();
        result.addProperty("schema", 1);
        result.addProperty("capture", session.id.toString());
        result.addProperty("started_utc", session.started.toString());
        result.addProperty("target_uuid", session.target.toString());
        result.addProperty("stop_reason", reason);
        result.addProperty("duration_ticks", ticks);
        result.addProperty("truncated", session.truncated);
        result.addProperty("boundary_pairing_complete_and_pools_finite", !session.invalidEvidence);
        result.addProperty("outer_hurt_calls", session.totals.outerCalls());
        result.addProperty("outer_hurt_returned_true", session.totals.returnedTrue());
        result.addProperty("outer_calls_with_net_pool_loss", session.totals.poolLossCalls());
        result.addProperty("observed_net_health_loss", finite(session.totals.healthLoss()));
        result.addProperty("observed_net_absorption_loss", finite(session.totals.absorptionLoss()));
        if (ticks > 0 && !session.invalidEvidence) {
            result.addProperty("observed_net_health_loss_per_game_second", session.totals.healthLossPerGameSecond(ticks));
            result.addProperty("observed_pool_loss_calls_per_game_second", session.totals.poolLossCallsPerGameSecond(ticks));
        }
        result.addProperty("semantics", "Native LivingEntity.hurt calls only; outer-call net health/absorption losses include nested hits exactly once. Net losses are not pre-mitigation damage. Healing, death prevention or pool restoration within the same call can mask damage. Game-second rates include idle capture time and are not wall-clock DPS or cooldown measurements. Player/mod overrides rejecting before super.hurt and direct setHealth damage are outside this boundary. No Curios/capability snapshot; selected target and participating attacker equipment only; stack SNBT capped at 2048 characters. No spell ID inference.");
        result.add("target_initial", session.initial);
        result.add("samples", session.samples);
        Path path = session.server.getWorldPath(LevelResource.ROOT).resolve("jem-combat-captures").resolve(session.id + ".json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(result), StandardCharsets.UTF_8);
            active = null;
            frames.clear();
            source.sendSuccess(() -> Component.literal("Combat capture saved: " + path.toAbsolutePath()), false);
            return 1;
        } catch (IOException failure) {
            // Retain the bounded capture for an explicit retry; disable further sampling.
            session.saveFailed = true;
            frames.clear();
            source.sendFailure(Component.literal("Could not save capture: " + failure.getMessage() + "; use /jemcombat stop to retry."));
            return 0;
        }
    }

    private static JsonObject identity(Entity entity) {
        JsonObject value = new JsonObject();
        if (entity != null) {
            value.addProperty("uuid", entity.getUUID().toString());
            value.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        }
        return value;
    }

    private static JsonObject loadout(LivingEntity entity) {
        JsonObject value = identity(entity);
        value.addProperty("health", finite(entity.getHealth()));
        value.addProperty("absorption", finite(entity.getAbsorptionAmount()));
        value.addProperty("max_health", finite(entity.getMaxHealth()));
        value.addProperty("armor", entity.getArmorValue());
        value.addProperty("armor_toughness", finite(entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
        if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null)
            value.addProperty("attack_damage_attribute", finite(entity.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (entity.getAttribute(Attributes.ATTACK_SPEED) != null)
            value.addProperty("attack_speed_attribute", finite(entity.getAttributeValue(Attributes.ATTACK_SPEED)));
        JsonObject gear = new JsonObject();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String nbt = entity.getItemBySlot(slot).save(new CompoundTag()).toString();
            gear.addProperty(slot.getName(), nbt.length() <= 2048 ? nbt : nbt.substring(0, 2048) + " [TRUNCATED]");
        }
        value.add("equipment_snbt", gear);
        JsonArray effects = new JsonArray();
        entity.getActiveEffects().stream().limit(64).forEach(effect -> {
            JsonObject row = new JsonObject();
            row.addProperty("id", BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).toString());
            row.addProperty("amplifier", effect.getAmplifier());
            row.addProperty("duration", effect.getDuration());
            effects.add(row);
        });
        value.add("effects", effects);
        return value;
    }

    private static Double finite(double value) { return Double.isFinite(value) ? value : null; }

    private record Frame(Session session, UUID target, float health, float absorption, boolean nested, JsonObject row) {}

    private static final class Session {
        final UUID id = UUID.randomUUID();
        final Instant started = Instant.now();
        final MinecraftServer server;
        final UUID target;
        final int startTick;
        final JsonObject initial;
        final JsonArray samples = new JsonArray();
        int endTick;
        final CombatCaptureTotals totals = new CombatCaptureTotals();
        boolean truncated;
        boolean saveFailed;
        boolean invalidEvidence;

        Session(MinecraftServer server, LivingEntity target, int seconds) {
            this.server = server;
            this.target = target.getUUID();
            startTick = server.getTickCount();
            endTick = startTick + seconds * 20;
            initial = loadout(target);
        }
    }
}
