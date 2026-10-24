package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class HostedEncounters {
    private static final String ROOT = "JEMHostedEncounter";
    private static final String ALIVE = "ALIVE";
    private static final String DOWNED = "DOWNED";
    private static final String ELIMINATED = "ELIMINATED";
    private static final String DOWNED_MARKER = "JEMHostedDowned";
    private static final Map<UUID, LivingEntity> ACTIVE = new LinkedHashMap<>();
    private static final TagKey<Item> RESOURCES = TagKey.create(Registries.ITEM, new ResourceLocation(JemWorldBossTiers.MOD_ID, "raid_resources"));
    private static final TagKey<Item> FORBIDDEN = TagKey.create(Registries.ITEM, new ResourceLocation(JemWorldBossTiers.MOD_ID, "raid_forbidden"));

    private HostedEncounters() {
    }

    /** Remove only hosted-owned state when a world runs without dedicated hosting. */
    public static void restoreNative(LivingEntity boss) {
        ACTIVE.remove(boss.getUUID());
        if (boss.getPersistentData().contains(ROOT)) {
            BossPeriodicEffects.clear(boss);
            applyScale(boss, Attributes.MAX_HEALTH, 1);
            applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
            boss.getPersistentData().remove(ROOT);
            boss.setHealth(Math.min(boss.getHealth(), boss.getMaxHealth()));
        }
        if (HostedEncounterApi.preservesRaidBoss(boss)) HostedEncounterApi.restorePreparedRaid(boss);
        else HostedRaidScaling.restore(boss);
        HostedLobby.release(boss);
        HostedGroup.detach(boss);
        boss.getPersistentData().remove("jem_solo");
        boss.getPersistentData().remove(DOWNED_MARKER);
    }

    public static boolean isHosted(LivingEntity boss) {
        return HostedEncounterApi.enabled(boss.getServer()) && boss.getPersistentData().contains(ROOT);
    }

    private static CompoundTag data(LivingEntity boss) {
        return boss.getPersistentData().getCompound(ROOT);
    }

    private static CompoundTag players(LivingEntity boss) {
        return data(boss).getCompound("Players");
    }

    private static CompoundTag participant(LivingEntity boss, UUID player) {
        return players(boss).getCompound(player.toString());
    }

    private static long now(LivingEntity boss) {
        return boss.getServer().overworld().getGameTime();
    }

    private static LivingEntity encounter(UUID player) {
        return ACTIVE.values().stream().filter(boss -> players(boss).contains(player.toString())).findFirst().orElse(null);
    }

    public static boolean participating(ServerPlayer player) {
        LivingEntity boss = encounter(player.getUUID());
        return boss != null && ALIVE.equals(participant(boss, player.getUUID()).getString("State"));
    }

    public static UUID participantSession(ServerPlayer player) {
        var boss=encounter(player.getUUID());
        return boss!=null && !boss.getPersistentData().getBoolean("jem_solo") && players(boss).size()>1 && ALIVE.equals(participant(boss,player.getUUID()).getString("State")) ? boss.getUUID() : null;
    }

    public static boolean keepsInventory(ServerPlayer player) {
        var boss=encounter(player.getUUID());
        return boss!=null && ALIVE.equals(participant(boss,player.getUUID()).getString("State")) && inside(boss,player);
    }

    public static void revived(ServerPlayer helper) {
        var boss=encounter(helper.getUUID());
        if(boss!=null && validTarget(boss, helper) && combatOpen(boss)) {
            var entry=participant(boss,helper.getUUID());
            entry.putBoolean("Supported",true);
            entry.putInt("Revives",entry.getInt("Revives")+1);
        }
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreed, boolean raid, int radius) {
        return start(boss, agreed, raid, radius, null);
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreed, boolean raid, int radius, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        return start(boss, agreed, raid, radius, bounds, null, attempt -> {});
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreed, boolean raid, int radius,
                                net.minecraft.world.level.levelgen.structure.BoundingBox bounds, UUID partyId,
                                java.util.function.Consumer<UUID> beforeRelease) {
        if (com.siirio.jemworldbosstiers.revival.NativeBossStorage.pending(boss)) return false;
        if (!HostedEncounterApi.enabled(boss.getServer()) || raid && !net.minecraftforge.fml.ModList.get().isLoaded("jem_server") || isHosted(boss)) return false;
        CompoundTag entries = new CompoundTag();
        for (UUID id : new LinkedHashSet<>(agreed)) {
            ServerPlayer player = boss.getServer().getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive() || player.isSpectator() || encounter(id) != null) {
                return false;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("State", ALIVE);
            entry.putString("Name", player.getGameProfile().getName());
            entry.putString("OriginDimension", player.serverLevel().dimension().location().toString());
            entry.putDouble("OriginX", player.getX());
            entry.putDouble("OriginY", player.getY());
            entry.putDouble("OriginZ", player.getZ());
            entry.putFloat("OriginYaw", player.getYRot());
            entry.putFloat("OriginPitch", player.getXRot());
            entry.putBoolean("Entered",false);
            ListTag gear = new ListTag();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                gear.add(player.getItemBySlot(slot).save(new CompoundTag()));
            }
            entry.put("Equipment", gear);
            entries.put(id.toString(), entry);
        }
        if (entries.isEmpty()) {
            return false;
        }
        String profileKey = WorldTierApi.profile(boss).orElseThrow().key().toString();
        float originalHealth = boss.getHealth();
        CompoundTag originalNative = boss.getPersistentData().getCompound(EncounterData.ROOT_KEY).copy();
        HostedScaling scaling = raid ? HostedRaidScaling.startScaling(boss, profileKey, entries.size())
                : HostedConfig.scale(profileKey, entries.size(), false);
        double health = scaling.health();
        double damage = scaling.damage();
        CompoundTag state = new CompoundTag();
        state.putUUID("Attempt", UUID.randomUUID());
        state.putUUID("Boss", boss.getUUID());
        if (partyId != null) state.putUUID("Party", partyId);
        if (boss instanceof Mob mob) {
            var lobby = boss.getPersistentData().getCompound("JEMHostedLobby");
            state.putBoolean("NativeNoAi", lobby.contains("NativeNoAi") ? lobby.getBoolean("NativeNoAi") : mob.isNoAi());
            state.putBoolean("CurrentNoAi", state.getBoolean("NativeNoAi"));
        }
        state.put("Players", entries);
        state.putInt("Radius", radius);
        if(bounds!=null) state.putIntArray("Bounds",new int[]{bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()});
        state.putDouble("HealthFactor", health);
        state.putDouble("DamageFactor", damage);
        boss.getPersistentData().put(ROOT, state);
        if (raid) {
            CompoundTag nativeData = boss.getPersistentData().getCompound(EncounterData.ROOT_KEY);
            nativeData.putString("Provenance", EncounterProvenance.RAID_EVENT.name());
            nativeData.putBoolean("ProgressionEligible", false);
            nativeData.putBoolean("Rematch", true);
        }
        applyScale(boss, Attributes.MAX_HEALTH, health);
        applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
        boss.setHealth(boss.getMaxHealth());
        CompoundTag nativeData = boss.getPersistentData().getCompound(EncounterData.ROOT_KEY);
        if (nativeData.getBoolean("HealthSyncPending")) {
            nativeData.putFloat("InitialHealth", boss.getMaxHealth());
        }
        state.putDouble("StartingHealth", boss.getMaxHealth());
        state.put("NativeEncounter", boss.getPersistentData().getCompound(EncounterData.ROOT_KEY).copy());
        boolean acknowledged;
        try {
            acknowledged = HostedSettlement.start(() -> persistAttempt(boss),
                    () -> beforeRelease.accept(state.getUUID("Attempt")),
                    () -> {
                        HostedGroup.stage(boss);
                        HostedLobby.release(boss);
                        HostedGroup.release(boss);
                        ACTIVE.put(boss.getUUID(), boss);
                        BossPeriodicEffects.begin(boss);
                    });
        } catch (RuntimeException failure) {
            acknowledged = false;
            com.mojang.logging.LogUtils.getLogger().error("Hosted Start {} was not acknowledged by its owners", state.getUUID("Attempt"), failure);
        }
        if (!acknowledged) {
            WorldTierData world = WorldTierData.get(boss.getServer());
            world.failHostedAttempt(state.getUUID("Attempt"));
            world.flush(boss.getServer());
            boss.getPersistentData().remove(ROOT);
            ACTIVE.remove(boss.getUUID());
            BossPeriodicEffects.clear(boss);
            HostedGroup.hold(boss);
            boss.getPersistentData().put(EncounterData.ROOT_KEY, originalNative);
            applyScale(boss, Attributes.MAX_HEALTH, 1);
            applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
            boss.setHealth(Math.min(originalHealth, boss.getMaxHealth()));
            HostedLobby.hold(boss);
            return false;
        }
        return true;
    }

    private static String rewardSource(LivingEntity boss) {
        var state = data(boss);
        if (!state.hasUUID("Attempt")) throw new IllegalStateException("Missing canonical encounter attempt");
        return boss.getStringUUID() + ":" + state.getUUID("Attempt");
    }

    public static UUID attemptId(LivingEntity boss) {
        return data(boss).hasUUID("Attempt") ? data(boss).getUUID("Attempt") : null;
    }

    private static boolean persistAttempt(LivingEntity boss) {
        return persistAttempt(boss, true);
    }

    private static boolean persistAttempt(LivingEntity boss, boolean captureNative) {
        UUID attempt = attemptId(boss);
        if (attempt == null) return false;
        if (captureNative && boss instanceof Mob mob && !HostedLobby.held(boss)) data(boss).putBoolean("CurrentNoAi", mob.isNoAi());
        CompoundTag snapshot = new CompoundTag();
        snapshot.putUUID("Boss", boss.getUUID());
        snapshot.putString("Dimension", boss.level().dimension().location().toString());
        snapshot.put("State", data(boss).copy());
        WorldTierData world = WorldTierData.get(boss.getServer());
        world.putHostedAttempt(attempt, snapshot);
        boolean durable = world.flush(boss.getServer());
        if (!durable) com.mojang.logging.LogUtils.getLogger().error("Hosted attempt {} has unacknowledged state. Storage must be restored before stopping this server process", attempt);
        return durable;
    }

    public static double damageFactor(LivingEntity boss) {
        return isHosted(boss) ? Math.max(1, data(boss).getDouble("DamageFactor")) : 1;
    }

    private static void applyScale(LivingEntity boss, Attribute attribute, double factor) {
        var instance = boss.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID id = UUID.nameUUIDFromBytes((ROOT + attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(id);
        if (factor != 1) {
            instance.addPermanentModifier(new AttributeModifier(id, ROOT, factor - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public static List<HostedEncounterApi.ParticipantView> status(LivingEntity boss) {
        List<HostedEncounterApi.ParticipantView> result = new ArrayList<>();
        for (String id : players(boss).getAllKeys()) {
            CompoundTag entry = players(boss).getCompound(id);
            boolean offline = entry.contains("DisconnectUntil") && !ELIMINATED.equals(entry.getString("State"));
            result.add(new HostedEncounterApi.ParticipantView(UUID.fromString(id), offline ? "DISCONNECTED" : entry.getString("State"),
                    DOWNED.equals(entry.getString("State")) || ALIVE.equals(entry.getString("State")) && !entry.getBoolean("DownedUsed"), Math.max(0, entry.getLong(offline ? "DisconnectUntil" : "BleedoutUntil") - now(boss)),
                    entry.getDouble("Contribution"), entry.getDouble("Power")));
        }
        result.sort(Comparator.comparing(value -> value.playerId().toString()));
        return List.copyOf(result);
    }

    public static ListTag results(LivingEntity boss) {
        ListTag rows = new ListTag();
        players(boss).getAllKeys().stream().sorted(Comparator
                .comparingDouble((String id) -> players(boss).getCompound(id).getDouble("BossDamage")).reversed()
                .thenComparing(id -> id)).forEach(id -> {
            var entry = players(boss).getCompound(id);
            var row = new CompoundTag();
            row.putUUID("id", UUID.fromString(id));
            var player=boss.getServer().getPlayerList().getPlayer(UUID.fromString(id));
            row.putString("name", player==null ? entry.getString("Name") : player.getGameProfile().getName());
            row.putDouble("damage", entry.getDouble("BossDamage"));
            row.putInt("revives", entry.getInt("Revives"));
            row.putInt("downs", entry.getInt("Downs"));
            rows.add(row);
        });
        return rows;
    }

    public static Set<UUID> eligible(LivingEntity boss) {
        return HostedSettlement.recipients(data(boss), HostedConfig.MINIMUM_CONTRIBUTION.get());
    }

    private static boolean inside(LivingEntity boss, LivingEntity entity) {
        if (entity.level() != boss.level()) return false;
        var box = entity.getBoundingBox();
        var bounds = data(boss).getIntArray("Bounds");
        if (bounds.length == 6) return box.minX >= bounds[0] && box.minY >= bounds[1] && box.minZ >= bounds[2]
                && box.maxX <= bounds[3] + 1D && box.maxY <= bounds[4] + 1D && box.maxZ <= bounds[5] + 1D;
        double dx = Math.max(Math.abs(box.minX - boss.getX()), Math.abs(box.maxX - boss.getX()));
        double dy = Math.max(Math.abs(box.minY - boss.getY()), Math.abs(box.maxY - boss.getY()));
        double dz = Math.max(Math.abs(box.minZ - boss.getZ()), Math.abs(box.maxZ - boss.getZ()));
        double radius = data(boss).getInt("Radius");
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static LivingEntity combat(LivingEntity entity) {
        Entity owner = HostedEncounterApi.damageOwner(entity);
        if (owner instanceof ServerPlayer player) return encounter(player.getUUID());
        if (owner instanceof LivingEntity living) {
            if (isHosted(living)) return living;
            LivingEntity group = HostedGroup.owner(living);
            if (group != null && isHosted(group)) return group;
        }
        return null;
    }

    private static boolean admitted(LivingEntity boss, LivingEntity entity) {
        if (!entity.isAlive() || !inside(boss, entity)) return false;
        Entity owner = HostedEncounterApi.damageOwner(entity);
        if (owner instanceof ServerPlayer player) return validTarget(boss, player);
        return owner == boss || entity == boss || HostedGroup.owner(entity) == boss;
    }

    public static boolean canInteract(LivingEntity actor, LivingEntity target) {
        if (actor.level().isClientSide || actor.level() != target.level() || !actor.isAlive() || !target.isAlive()) return false;
        if (HostedLobby.held(actor) || HostedLobby.held(target)) return false;
        LivingEntity sourceBoss = combat(actor), targetBoss = combat(target);
        if (sourceBoss == null && targetBoss == null) return true;
        return sourceBoss == targetBoss && admitted(sourceBoss, actor) && admitted(sourceBoss, target);
    }

    public static void contribute(ServerPlayer player, double amount) {
        LivingEntity boss = encounter(player.getUUID());
        if (Double.isFinite(amount) && amount > 0 && boss != null && combatOpen(boss)
                && ALIVE.equals(participant(boss, player.getUUID()).getString("State")) && player.level() == boss.level()
                && inside(boss,player)) {
            CompoundTag entry = participant(boss, player.getUUID());
            entry.putDouble("Contribution", entry.getDouble("Contribution") + amount);
            if (!HostedEncounterApi.isRaid(boss) && eligible(boss).contains(player.getUUID())) {
                EncounterData.addParticipant(boss.getPersistentData(), player.getUUID());
            }
        }
    }

    public static boolean revive(ServerPlayer helper, UUID targetId) {
        LivingEntity boss = encounter(helper.getUUID());
        ServerPlayer target = helper.server.getPlayerList().getPlayer(targetId);
        if (boss == null || boss.getPersistentData().getBoolean("jem_solo") || target == null || helper == target || !helper.isAlive() || helper.isSpectator()
                || !ALIVE.equals(participant(boss, helper.getUUID()).getString("State"))
                || !DOWNED.equals(participant(boss, targetId).getString("State")) || helper.level() != target.level()
                || helper.distanceToSqr(target) > Math.pow(HostedConfig.REVIVE_DISTANCE.get(), 2) || !helper.hasLineOfSight(target)) {
            return false;
        }
        CompoundTag entry = participant(boss, targetId);
        long time = now(boss);
        if (time >= entry.getLong("BleedoutUntil")) {
            return false;
        }
        boolean continuous = entry.hasUUID("Helper") && entry.getUUID("Helper").equals(helper.getUUID()) && time - entry.getLong("LastRevive") <= 2;
        if (!continuous) {
            entry.putLong("ReviveStart", time);
        }
        entry.putUUID("Helper", helper.getUUID());
        entry.putLong("LastRevive", time);
        if (time - entry.getLong("ReviveStart") >= HostedConfig.REVIVE.get()) {
            entry.putString("State", ALIVE);
            entry.remove("BleedoutUntil");
            entry.putLong("ImmuneUntil", time + HostedConfig.IMMUNITY.get());
            target.setHealth((float) (target.getMaxHealth() * HostedConfig.REVIVE_HEALTH.get()));
            target.getPersistentData().remove(DOWNED_MARKER);
            revived(helper);
            if (!HostedEncounterApi.isRaid(boss)) {
                EncounterData.addParticipant(boss.getPersistentData(), helper.getUUID());
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectParticipants(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (HostedEncounterApi.isRaid(target) && !isHosted(target)) {
            event.setCanceled(true);
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source == null) source = event.getSource().getDirectEntity();
        Entity owner = HostedEncounterApi.damageOwner(event.getSource());
        if (!(owner instanceof net.minecraft.world.entity.player.Player)
                && (NativeEncounterSource.denied(event.getSource().getEntity())
                || NativeEncounterSource.denied(event.getSource().getDirectEntity()) || NativeEncounterSource.denied(owner)
                || NativeEncounterSource.denied(NativeEncounterSource.current()))) {
            event.setCanceled(true);
            return;
        }
        if (!(source instanceof LivingEntity) && owner instanceof LivingEntity) source = owner;
        if (source instanceof LivingEntity actor && !canInteract(actor, target)) {
            event.setCanceled(true);
            return;
        }
        LivingEntity boss = combat(target);
        if (boss != null && owner instanceof ServerPlayer attacker
                && HostedEncounterApi.damageOwner(target) instanceof ServerPlayer) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof ServerPlayer player && boss != null) {
            CompoundTag entry = participant(boss, player.getUUID());
            if (!ALIVE.equals(entry.getString("State")) || now(boss) < entry.getLong("ImmuneUntil")) event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        var mob = event.getEntity();
        var target = event.getNewTarget();
        if (mob.level().isClientSide || target == null) return;
        if (NativeEncounterSource.denied(mob)) { event.setCanceled(true); return; }
        Entity owner = HostedEncounterApi.damageOwner(mob);
        var root = owner instanceof LivingEntity living && isHosted(living) ? living
                : isHosted(mob) ? mob : HostedGroup.owner(mob);
        if (mob.level().isClientSide || root == null || !isHosted(root) || target == null) return;
        if (!canInteract(mob, target)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        Entity owner = HostedEncounterApi.damageOwner(event.getSource());
        if (isHosted(event.getEntity()) && owner instanceof ServerPlayer player && event.getAmount() > 0
                && ALIVE.equals(participant(event.getEntity(), player.getUUID()).getString("State"))) {
            double amount = Math.min(event.getAmount(), event.getEntity().getHealth());
            var entry = participant(event.getEntity(), player.getUUID());
            entry.putDouble("BossDamage", entry.getDouble("BossDamage") + amount);
            contribute(player, amount);
        }
        if (event.getEntity() instanceof ServerPlayer player && owner instanceof LivingEntity boss
                && isHosted(boss) && boss == encounter(player.getUUID())) {
            contribute(player, Math.min(event.getAmount(), player.getMaxHealth()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void support(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        Entity owner = HostedEncounterApi.damageOwner(event.getEffectSource());
        if (!(owner instanceof ServerPlayer helper) || !(event.getEntity() instanceof ServerPlayer target) || helper == target) return;
        var effect = event.getEffectInstance();
        var previous = event.getOldEffectInstance();
        if (!effect.getEffect().isBeneficial() || previous != null && previous.getAmplifier() >= effect.getAmplifier()
                && previous.getDuration() >= effect.getDuration()) return;
        // A server-attributed useful buff on a fighting teammate is real support. Mere
        // membership, proximity and self-buffs never qualify an idle reward recipient.
        creditSupport(helper, target);
    }

    public static void supportHealing(Entity healer, LivingEntity recipient, float actualHealing) {
        if (!Float.isFinite(actualHealing) || actualHealing <= 0) return;
        Entity owner = HostedEncounterApi.damageOwner(healer);
        if (owner instanceof ServerPlayer helper && recipient instanceof ServerPlayer target) creditSupport(helper, target);
    }

    private static boolean combatOpen(LivingEntity boss) {
        return !HostedLobby.held(boss) && !data(boss).contains("VictoryAt")
                && !data(boss).getBoolean("FailureClosing") && !data(boss).getBoolean("LinkPending");
    }

    private static void creditSupport(ServerPlayer helper, ServerPlayer target) {
        LivingEntity boss = encounter(helper.getUUID());
        if (helper == target || boss == null || encounter(target.getUUID()) != boss || !combatOpen(boss)
                || !validTarget(boss, helper) || !validTarget(boss, target)) return;
        if (!HostedSettlement.supportEligible(data(boss), helper.getUUID(), target.getUUID())) return;
        participant(boss, helper.getUUID()).putBoolean("Supported", true);
        if (!HostedEncounterApi.isRaid(boss)) EncounterData.addParticipant(boss.getPersistentData(), helper.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            if (isHosted(event.getEntity()) && event.getEntity().isDeadOrDying()
                    && !data(event.getEntity()).contains("VictoryAt") && !data(event.getEntity()).getBoolean("FailureClosing")) {
                LivingEntity boss = event.getEntity();
                data(boss).putLong("VictoryAt", now(boss));
                data(boss).putUUID("Boss", boss.getUUID());
                data(boss).putLong("DeathPosition", boss.blockPosition().asLong());
                data(boss).putBoolean("DeathByGenericKill", event.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL));
                data(boss).putString("EntityType", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString());
                HostedSettlement.freezeRecipients(data(boss), HostedConfig.MINIMUM_CONTRIBUTION.get());
                if (!HostedEncounterApi.isRaid(boss)) {
                    eligible(boss).forEach(id -> EncounterData.addParticipant(boss.getPersistentData(), id));
                }
                data(boss).put("NativeEncounter", boss.getPersistentData().getCompound(EncounterData.ROOT_KEY).copy());
            }
            return;
        }
        LivingEntity boss = encounter(player.getUUID());
        if (boss == null) {
            return;
        }
        CompoundTag entry = participant(boss, player.getUUID());
        // An uncanceled death ends participation; pre-death abilities retain their own event handling.
        eliminate(player, entry);
        HostedSettlement.eliminate(entry, true);
        persistAttempt(boss);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        LivingEntity boss = event.getEntity();
        if (!isHosted(boss) || !(boss.level() instanceof ServerLevel level)) {
            return;
        }
        if (!boss.isDeadOrDying() || !data(boss).contains("VictoryAt")
                || !data(boss).hasUUID("Boss") || !boss.getUUID().equals(data(boss).getUUID("Boss"))) return;
        List<UUID> recipients = eligible(boss).stream().sorted().toList();
        boolean raid = HostedEncounterApi.isRaid(boss);
        UUID attempt = attemptId(boss);
        var world = WorldTierData.get(level.getServer());
        if (attempt == null) throw new IllegalStateException("Missing canonical encounter attempt");
        if (world.hostedSettlementCompleted(attempt) || world.hostedSettlements().containsKey(attempt)) {
            event.getDrops().clear();
            event.setCanceled(true);
            return;
        }
        if (raid) event.getDrops().removeIf(drop -> drop.getItem().is(FORBIDDEN));
        List<ItemStack> loot = new ArrayList<>();
        for (var drop : event.getDrops()) {
            ItemStack stack = drop.getItem().copy();
            if (raid && stack.is(RESOURCES)) {
                stack.setCount(stack.getCount() * HostedConfig.RESOURCE_MULTIPLIER.get());
            }
            loot.add(stack);
        }
        List<com.siirio.jemrewards.RewardGrant> grants = new ArrayList<>();
        for (UUID recipient : recipients) {
            List<com.siirio.jemrewards.RewardGroup> groups = new ArrayList<>();
            if (!loot.isEmpty()) groups.add(new com.siirio.jemrewards.RewardGroup("reward.jem_world_boss_tiers.native_loot", loot));
            if (raid) groups.add(new com.siirio.jemrewards.RewardGroup("reward.jem_world_boss_tiers.raid_cache",
                    RaidRewardPool.roll(level, WorldTierApi.currentTier(level.getServer()))));
            grants.add(new com.siirio.jemrewards.RewardGrant(recipient,
                    raid ? "reward.jem_world_boss_tiers.raid_loot" : "reward.jem_world_boss_tiers.boss_loot", groups));
        }
        CompoundTag snapshot = new CompoundTag();
        snapshot.putUUID("Boss", boss.getUUID());
        snapshot.putUUID("Attempt", attempt);
        snapshot.putString("Dimension", level.dimension().location().toString());
        snapshot.putBoolean("Raid", raid);
        snapshot.put("State", data(boss).copy());
        snapshot.put("Grants", com.siirio.jemrewards.PendingRewardContainer.freezeBatch(rewardSource(boss), grants));
        snapshot.putInt("Experience", data(boss).getInt("CapturedExperience"));
        world.putHostedSettlement(attempt, snapshot);
        world.removeHostedAttempt(attempt);
        // Once captured by the canonical settlement, items never return to native world drops.
        event.getDrops().clear();
        event.setCanceled(true);
        // Even a failed first write remains an immutable in-process intent. A crash before any
        // durable acknowledgment cannot be recovered and must never be reported as success.
        if (!world.flush(level.getServer()))
            com.mojang.logging.LogUtils.getLogger().error("Boss settlement {} is not yet durable. Restore storage before stopping this server process", attempt);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void experience(LivingExperienceDropEvent event) {
        LivingEntity boss = event.getEntity();
        if (!isHosted(boss) || !boss.isDeadOrDying() || !data(boss).contains("VictoryAt")
                || event.getDroppedExperience() <= 0 || boss.getServer() == null) return;
        CompoundTag state = data(boss);
        if (!state.contains("CapturedExperience")) state.putInt("CapturedExperience", event.getDroppedExperience());
        event.setDroppedExperience(0);
        try {
            HostedRewards.get(boss.getServer()).enqueueExperience(boss.getServer(), rewardSource(boss), eligible(boss), state.getInt("CapturedExperience"));
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Personal boss XP intent {} retained for retry. An initial failed write is not crash-durable", rewardSource(boss), failure);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (!HostedEncounterApi.enabled(event.getServer()) || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % HostedConfig.DELIVERY_INTERVAL.get() == 0) {
            HostedRewards.get(event.getServer()).retryExperienceBatches(event.getServer());
            event.getServer().getPlayerList().getPlayers().forEach(player -> {
                try { HostedRewards.get(player.server).deliver(player); }
                catch (RuntimeException failure) {
                    com.mojang.logging.LogUtils.getLogger().error("Personal boss entitlement delivery remains pending for {}", player.getUUID(), failure);
                }
            });
        }
        if (event.getServer().getTickCount() % 20 == 0) retrySettlements(event.getServer());
        for (LivingEntity boss : List.copyOf(ACTIVE.values())) {
            if (data(boss).contains("VictoryAt")) {
                continue;
            }
            if (data(boss).getBoolean("FailureClosing")) {
                if (event.getServer().getTickCount() % 20 == 0) close(boss, false);
                continue;
            }
            if (data(boss).getBoolean("LinkPending")) {
                if (event.getServer().getTickCount() % 20 == 0) {
                    if (recoverPartyLink(boss)) {
                        data(boss).remove("LinkPending");
                        if (persistAttempt(boss)) releaseNative(boss);
                        else data(boss).putBoolean("LinkPending", true);
                    } else if (terminalPartyLink(boss)) {
                        data(boss).remove("LinkPending");
                        data(boss).putBoolean("FailureClosing", true);
                        close(boss, false);
                    }
                }
                continue;
            }
            maintainTarget(boss);
            boolean remains = false;
            boolean present = false;
            for (String id : players(boss).getAllKeys()) {
                CompoundTag entry = players(boss).getCompound(id);
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(UUID.fromString(id));
                boolean downed = DOWNED.equals(entry.getString("State"));
                if(player!=null&&inside(boss,player)) entry.putBoolean("Entered",true);
                if (player != null && entry.getBoolean("Entered") && player.level() != boss.level()) {
                    entry.putString("State", ELIMINATED);
                    if (downed) {
                        entry.putBoolean("BleedoutDeath", true);
                    }
                }
                if (downed && now(boss) >= entry.getLong("BleedoutUntil")) {
                    entry.remove("BleedoutUntil");
                    entry.putBoolean("BleedoutDeath", true);
                    if (player != null) {
                        eliminate(player, entry);
                        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                    } else entry.putString("State",ELIMINATED);
                } else if (entry.contains("DisconnectUntil") && now(boss) >= entry.getLong("DisconnectUntil")) {
                    HostedSettlement.expireAbsence(entry, now(boss));
                }
                if (player != null && DOWNED.equals(entry.getString("State"))) {
                    player.setDeltaMovement(0, 0, 0);
                    if (player.level() == boss.level() && player.distanceToSqr(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")) > 0.01) {
                        player.connection.teleport(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z"), player.getYRot(), player.getXRot());
                    }
                    player.stopUsingItem();
                }
                remains |= !ELIMINATED.equals(entry.getString("State"));
                present |= player != null && player.isAlive() && !player.isSpectator()
                        && ALIVE.equals(entry.getString("State")) && inside(boss, player);
            }
            if (present) data(boss).remove("EmptySince");
            else if (!data(boss).contains("EmptySince")) data(boss).putLong("EmptySince", now(boss));
            if (!remains || !present && now(boss) - data(boss).getLong("EmptySince") >= HostedConfig.GRACE.get()) {
                close(boss, false);
            } else {
                List<ServerPlayer> effectTargets=players(boss).getAllKeys().stream()
                        .map(UUID::fromString)
                        .map(event.getServer().getPlayerList()::getPlayer)
                        .filter(java.util.Objects::nonNull)
                        .filter(player->ALIVE.equals(participant(boss,player.getUUID()).getString("State"))&&inside(boss,player))
                        .toList();
                BossPeriodicEffects.tick(boss,effectTargets);
                if (event.getServer().getTickCount() % 20 == 0) persistAttempt(boss);
            }
        }
    }

    private static void maintainTarget(LivingEntity boss) {
        if (!(boss instanceof Mob mob) || mob.isNoAi()) return;
        LivingEntity current = mob.getTarget();
        // Native AI owns acquiring and choosing targets, including authorized pets and summons.
        if (current != null && !canInteract(boss, current)) mob.setTarget(null);
    }

    private static boolean recoverPartyLink(LivingEntity boss) {
        if (!data(boss).hasUUID("Party")) return true;
        return partyAcknowledgment(boss, "recoverHostedAttempt");
    }

    private static boolean terminalPartyLink(LivingEntity boss) {
        return data(boss).hasUUID("Party") && partyAcknowledgment(boss, "hostedAttemptTerminal");
    }

    private static boolean partyAcknowledgment(LivingEntity boss, String method) {
        try {
            CompoundTag reconciled = data(boss).copy();
            boolean acknowledged = (boolean) Class.forName("com.siirio.jemserver.smp.HostedParties")
                    .getMethod(method, net.minecraft.server.MinecraftServer.class, UUID.class, UUID.class, CompoundTag.class)
                    .invoke(null, boss.getServer(), boss.getUUID(), attemptId(boss), reconciled);
            if (acknowledged) {
                HostedSettlement.mergeEliminations(data(boss), reconciled);
            }
            return acknowledged;
        } catch (ReflectiveOperationException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Hosted Start {} awaits its durable party link", attemptId(boss), failure);
            return false;
        }
    }

    private static void releaseNative(LivingEntity boss) {
        HostedLobby.release(boss);
        HostedGroup.release(boss);
        if (boss instanceof Mob mob && data(boss).contains("NativeNoAi"))
            mob.setNoAi(data(boss).getBoolean(data(boss).contains("CurrentNoAi") ? "CurrentNoAi" : "NativeNoAi"));
    }

    private static void retrySettlements(net.minecraft.server.MinecraftServer server) {
        WorldTierData world = WorldTierData.get(server);
        for (var pending : world.hostedSettlements().entrySet()) {
            UUID attempt = pending.getKey();
            CompoundTag snapshot = pending.getValue();
            String source = snapshot.getUUID("Boss") + ":" + attempt;
            try {
                HostedSettlement.finish(
                        () -> { if (!world.flush(server)) throw new IllegalStateException("storage_unavailable"); },
                        () -> {
                            com.siirio.jemrewards.PendingRewardContainer.enqueueFrozenBatch(server, source, snapshot.getList("Grants", net.minecraft.nbt.Tag.TAG_COMPOUND));
                            HostedRewards.get(server).enqueueExperience(server, source,
                                    HostedSettlement.recipients(snapshot.getCompound("State"), HostedConfig.MINIMUM_CONTRIBUTION.get()),
                                    snapshot.getInt("Experience"));
                        },
                        () -> finishVictory(server, snapshot),
                        () -> {
                            world.completeHostedSettlement(attempt);
                            if (!world.flush(server)) {
                                world.restoreHostedSettlement(attempt, snapshot);
                                throw new IllegalStateException("storage_unavailable");
                            }
                        });
            } catch (RuntimeException failure) {
                com.mojang.logging.LogUtils.getLogger().error("Boss settlement {} remains pending, no terminal success has been acknowledged", attempt, failure);
            }
        }
    }

    private static void finishVictory(net.minecraft.server.MinecraftServer server, CompoundTag snapshot) {
        UUID bossId = snapshot.getUUID("Boss"), attempt = snapshot.getUUID("Attempt");
        ResourceLocation dimension = new ResourceLocation(snapshot.getString("Dimension"));
        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        LivingEntity boss = ACTIVE.get(bossId);
        if (boss == null && level != null && level.getEntity(bossId) instanceof LivingEntity loaded) boss = loaded;
        if (boss != null && !attempt.equals(attemptId(boss))) boss = null;
        CompoundTag state = snapshot.getCompound("State");
        Set<UUID> recipients = HostedSettlement.recipients(state, HostedConfig.MINIMUM_CONTRIBUTION.get());
        boolean raid = snapshot.getBoolean("Raid");
        if (boss != null) data(boss).putBoolean("SettlementClosing", true);
        try {
            // Every listener must acknowledge its own durable outcome before the final receipt.
            MinecraftForge.EVENT_BUS.post(new HostedEncounterClosedEvent(server, bossId, attempt, dimension, state, raid, true, recipients, boss));
            if (!raid) {
                var profileKey = ResourceLocation.tryParse(state.getCompound("NativeEncounter").getString("Profile"));
                var profile = profileKey == null ? java.util.Optional.<com.siirio.jemworldbosstiers.balance.BossProfile>empty()
                        : com.siirio.jemworldbosstiers.balance.BalanceRegistry.bossByKey(profileKey);
                profile.ifPresent(value -> recipients.forEach(id -> value.victoryAdvancementIds()
                        .forEach(advancement -> HostedRewards.get(server).award(server, id, advancement))));
                HostedRewards.get(server).flush(server);
            }
            if (boss != null) {
                BossPeriodicEffects.clear(boss);
                HostedGroup.finish(boss);
                boss.getPersistentData().remove(ROOT);
                ACTIVE.remove(bossId);
            }
        } finally {
            if (boss != null && boss.getPersistentData().contains(ROOT)) data(boss).remove("SettlementClosing");
        }
    }

    private static boolean validTarget(LivingEntity boss, ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && ALIVE.equals(participant(boss, player.getUUID()).getString("State"))
                && inside(boss, player);
    }

    private static void eliminate(ServerPlayer player, CompoundTag entry) {
        player.getPersistentData().remove(DOWNED_MARKER);
        HostedSettlement.eliminate(entry, false);
    }

    private static boolean close(LivingEntity boss, boolean success) {
        if (!isHosted(boss) || data(boss).getBoolean("SettlementClosing") || data(boss).getBoolean("ClosingCallback")) return false;
        // Death callbacks and entity removal cannot bypass the reward transaction.
        if (success || data(boss).contains("VictoryAt")) return false;
        boolean raid = HostedEncounterApi.isRaid(boss);
        boolean preserveRaid = raid && HostedEncounterApi.preservesRaidBoss(boss);
        Set<UUID> eligible = success ? eligible(boss) : Set.of();
        data(boss).putBoolean("FailureClosing", true);
        HostedLobby.hold(boss);
        HostedGroup.hold(boss);
        if (!persistAttempt(boss)) return false;
        for (String id : players(boss).getAllKeys()) {
            CompoundTag entry = players(boss).getCompound(id);
            if (DOWNED.equals(entry.getString("State")) || entry.getBoolean("BleedoutDeath")) {
                HostedRewards.get(boss.getServer()).resolveDowned(boss.getServer(), UUID.fromString(id), success && !entry.getBoolean("BleedoutDeath"));
            }
        }
        data(boss).putBoolean("ClosingCallback", true);
        try {
            MinecraftForge.EVENT_BUS.post(new HostedEncounterClosedEvent(boss, raid, success, eligible));
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Hosted defeat {} remains pending until terminal listeners acknowledge storage", attemptId(boss), failure);
            return false;
        } finally {
            data(boss).remove("ClosingCallback");
        }
        WorldTierData world = WorldTierData.get(boss.getServer());
        UUID attempt = attemptId(boss);
        CompoundTag activeSnapshot = world.hostedAttempts().get(attempt);
        world.failHostedAttempt(attempt);
        if (!world.flush(boss.getServer())) {
            if (activeSnapshot != null) world.restoreHostedAttempt(attempt, activeSnapshot);
            return false;
        }
        ACTIVE.remove(boss.getUUID());
        BossPeriodicEffects.clear(boss);
        boss.getPersistentData().remove(ROOT);
        if (success) HostedGroup.finish(boss);
        else HostedGroup.hold(boss);
        if (!success && boss.isAlive()) {
            if (raid&&!preserveRaid) {
                // The canonical Raid arena owner must persist retirement before removal.
                // Keep legacy synthetic bodies held until its deferred reset acknowledges it.
                HostedLobby.hold(boss);
            } else {
                applyScale(boss, Attributes.MAX_HEALTH, 1);
                applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
                boss.setHealth(boss.getMaxHealth());
                if (boss instanceof Mob mob) {
                    mob.setTarget(null);
                }
                if(preserveRaid) HostedEncounterApi.restorePreparedRaid(boss);
                HostedLobby.hold(boss);
            }
        }
        return true;
    }

    public static boolean cancel(LivingEntity boss) {
        if (!isHosted(boss) || data(boss).contains("VictoryAt") || data(boss).getBoolean("SettlementClosing")) return false;
        return close(boss, false);
    }

    public static boolean leave(ServerPlayer player) {
        LivingEntity boss = encounter(player.getUUID());
        if (boss == null || data(boss).contains("VictoryAt")) return false;
        CompoundTag entry = participant(boss, player.getUUID());
        if (ELIMINATED.equals(entry.getString("State"))) return false;
        HostedSettlement.eliminate(entry, false);
        entry.remove("BleedoutUntil");
        entry.remove("DisconnectUntil");
        player.getPersistentData().remove(DOWNED_MARKER);
        persistAttempt(boss);
        // The party travel owner persists and validates home/origin return separately.
        return true;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LivingEntity boss = encounter(event.getEntity().getUUID());
        if (boss != null) {
            participant(boss, event.getEntity().getUUID()).putLong("DisconnectUntil", now(boss) + HostedConfig.GRACE.get());
            persistAttempt(boss);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HostedRewards.get(player.server).deliver(player);
            LivingEntity boss = encounter(player.getUUID());
            if (boss != null) {
                CompoundTag entry = participant(boss, player.getUUID());
                if (entry.contains("DisconnectUntil") && now(boss) >= entry.getLong("DisconnectUntil")) {
                    HostedSettlement.expireAbsence(entry, now(boss));
                }
                entry.remove("DisconnectUntil");
                persistAttempt(boss);
            } else if (player.getPersistentData().hasUUID(DOWNED_MARKER)) {
                player.getPersistentData().remove(DOWNED_MARKER);
                player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        var receipt = event.getOriginal().getPersistentData().get("JEMHostedXpReceipts");
        if (receipt != null) event.getEntity().getPersistentData().put("JEMHostedXpReceipts", receipt.copy());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LivingEntity boss = encounter(player.getUUID());
            if (boss != null && DOWNED.equals(participant(boss, player.getUUID()).getString("State")) && event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity boss && HostedEncounterApi.enabled(boss.getServer())) {
            if (!boss.getPersistentData().contains(ROOT) && !HostedEncounterApi.canHost(boss)) return;
            var world = WorldTierData.get(boss.getServer());
            var active = world.hostedAttempts().values().stream()
                    .filter(snapshot -> snapshot.hasUUID("Boss") && snapshot.getUUID("Boss").equals(boss.getUUID())
                            && snapshot.getString("Dimension").equals(boss.level().dimension().location().toString()))
                    .findFirst().orElse(null);
            var settlement = world.hostedSettlements().values().stream()
                    .filter(snapshot -> snapshot.getUUID("Boss").equals(boss.getUUID())).findFirst().orElse(null);
            if (settlement != null) boss.getPersistentData().put(ROOT, settlement.getCompound("State").copy());
            else if (active != null) boss.getPersistentData().put(ROOT, active.getCompound("State").copy());
            else if (boss.getPersistentData().contains(ROOT)) {
                // An old entity save cannot reactivate an acknowledged terminal attempt.
                restoreNative(boss);
                HostedLobby.hold(boss);
                return;
            }
            if (!isHosted(boss)) return;
            applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
            ACTIVE.put(boss.getUUID(), boss);
            if (settlement != null) HostedLobby.hold(boss);
            else if (data(boss).getBoolean("FailureClosing")) {
                HostedLobby.hold(boss);
                HostedGroup.hold(boss);
            }
            else if (!recoverPartyLink(boss)) {
                data(boss).putBoolean(terminalPartyLink(boss) ? "FailureClosing" : "LinkPending", true);
                HostedLobby.hold(boss);
                HostedGroup.hold(boss);
            }
            else {
                data(boss).remove("LinkPending");
                applyScale(boss, Attributes.MAX_HEALTH, Math.max(1, data(boss).getDouble("HealthFactor")));
                if (persistAttempt(boss, false)) releaseNative(boss);
                else {
                    data(boss).putBoolean("LinkPending", true);
                    HostedLobby.hold(boss);
                    HostedGroup.hold(boss);
                }
            }
            for (String id : players(boss).getAllKeys()) {
                CompoundTag entry = players(boss).getCompound(id);
                if (boss.getServer().getPlayerList().getPlayer(UUID.fromString(id)) == null && !entry.contains("DisconnectUntil")) {
                    entry.putLong("DisconnectUntil", now(boss) + HostedConfig.GRACE.get());
                }
            }
            if (settlement == null) persistAttempt(boss);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity boss && ACTIVE.containsKey(boss.getUUID())) {
            if (boss.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                if (!data(boss).contains("VictoryAt")) persistAttempt(boss);
                ACTIVE.remove(boss.getUUID());
            } else {
                close(boss, data(boss).contains("VictoryAt"));
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppingEvent event) {
        for (LivingEntity boss : List.copyOf(ACTIVE.values())) {
            if (!data(boss).contains("VictoryAt")) persistAttempt(boss);
        }
        ACTIVE.clear();
    }
}
