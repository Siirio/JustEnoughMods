package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;
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

    public static boolean isHosted(LivingEntity boss) {
        return boss.getPersistentData().contains(ROOT);
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

    public static UUID participantSession(ServerPlayer player) {
        var boss=encounter(player.getUUID());
        return boss!=null && !boss.getPersistentData().getBoolean("jem_solo") && players(boss).size()>1 && ALIVE.equals(participant(boss,player.getUUID()).getString("State")) ? boss.getUUID() : null;
    }
    public static void revived(ServerPlayer helper) {
        var boss=encounter(helper.getUUID());
        if(boss!=null) {
            var entry=participant(boss,helper.getUUID());
            entry.putBoolean("Supported",true);
            entry.putInt("Revives",entry.getInt("Revives")+1);
        }
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreed, boolean raid, int radius) {
        return start(boss, agreed, raid, radius, null);
    }

    public static boolean start(LivingEntity boss, Collection<UUID> agreed, boolean raid, int radius, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        CompoundTag entries = new CompoundTag();
        for (UUID id : new LinkedHashSet<>(agreed)) {
            ServerPlayer player = boss.getServer().getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive() || player.isSpectator() || encounter(id) != null) {
                continue;
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
        HostedScaling scaling = HostedConfig.scale(WorldTierApi.profile(boss).orElseThrow().key().toString(), entries.size(), raid);
        double health = scaling.health();
        double damage = scaling.damage();
        CompoundTag state = new CompoundTag();
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
        HostedLobby.release(boss);
        ACTIVE.put(boss.getUUID(), boss);
        BossPeriodicEffects.begin(boss);
        if(boss instanceof Mob mob) {
            mob.setNoAi(false);
            entries.getAllKeys().stream().map(UUID::fromString)
                    .map(boss.getServer().getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull)
                    .filter(player->player.isAlive()&&!player.isSpectator()&&player.level()==boss.level())
                    .min(Comparator.comparingDouble(player->boss.distanceToSqr(player)))
                    .ifPresent(mob::setTarget);
        }
        return true;
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
            boolean offline = entry.contains("DisconnectUntil");
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
        Set<UUID> result = new LinkedHashSet<>();
        for (String id : players(boss).getAllKeys()) {
            CompoundTag entry = players(boss).getCompound(id);
            if (entry.getBoolean("Supported") || entry.getDouble("Contribution") >= data(boss).getDouble("StartingHealth") * HostedConfig.MINIMUM_CONTRIBUTION.get()) {
                result.add(UUID.fromString(id));
            }
        }
        return result;
    }

    private static boolean inside(LivingEntity boss,ServerPlayer player) {
        if(player.level()!=boss.level()) return false;
        var bounds=data(boss).getIntArray("Bounds");
        if(bounds.length==6) return new net.minecraft.world.level.levelgen.structure.BoundingBox(bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],bounds[5]).isInside(player.blockPosition());
        return player.distanceToSqr(boss)<=(double)data(boss).getInt("Radius")*data(boss).getInt("Radius");
    }

    public static void contribute(ServerPlayer player, double amount) {
        LivingEntity boss = encounter(player.getUUID());
        if (boss != null && ALIVE.equals(participant(boss, player.getUUID()).getString("State")) && player.level() == boss.level()
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
        LivingEntity boss = target instanceof ServerPlayer player ? encounter(player.getUUID()) : isHosted(target) ? target : null;
        Entity resolved = HostedEncounterApi.damageOwner(event.getSource().getEntity());
        if (resolved == null) resolved = HostedEncounterApi.damageOwner(event.getSource().getDirectEntity());
        LivingEntity sourceBoss = resolved instanceof ServerPlayer sourcePlayer ? encounter(sourcePlayer.getUUID())
                : resolved instanceof LivingEntity living && isHosted(living) ? living : null;
        if (resolved != null && (boss != null || sourceBoss != null)) {
            if (boss != sourceBoss || target instanceof ServerPlayer && resolved instanceof ServerPlayer) {
                event.setCanceled(true);
                return;
            }
            if (resolved instanceof ServerPlayer sourcePlayer && !inside(boss, sourcePlayer)
                    || target instanceof ServerPlayer targetPlayer && !inside(boss, targetPlayer)) {
                event.setCanceled(true);
                return;
            }
        }
        if (boss != null) {
            Entity attacker = HostedEncounterApi.damageOwner(event.getSource().getEntity());
            if (target instanceof ServerPlayer && attacker instanceof ServerPlayer teammate && players(boss).contains(teammate.getStringUUID())) {
                event.setCanceled(true);
                return;
            }
            if (target instanceof ServerPlayer player) {
                CompoundTag entry = participant(boss, player.getUUID());
                if (DOWNED.equals(entry.getString("State")) || now(boss) < entry.getLong("ImmuneUntil")) {
                    event.setCanceled(true);
                }
            } else if (attacker instanceof ServerPlayer player && !ALIVE.equals(participant(boss, player.getUUID()).getString("State"))) {
                event.setCanceled(true);
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity active = encounter(player.getUUID());
            if (active != null && DOWNED.equals(participant(active, player.getUUID()).getString("State"))) {
                event.setCanceled(true);
            }
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isHosted(attacker) && target instanceof ServerPlayer player
                && !ALIVE.equals(participant(attacker, player.getUUID()).getString("State"))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        var mob = event.getEntity();
        var target = event.getNewTarget();
        if (mob.level().isClientSide || !isHosted(mob) || target == null) return;
        if (!(target instanceof ServerPlayer player) || !ALIVE.equals(participant(mob, player.getUUID()).getString("State"))
                || !inside(mob, player) || player.isSpectator()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        Entity owner = HostedEncounterApi.damageOwner(event.getSource().getEntity());
        if (owner == null) owner = HostedEncounterApi.damageOwner(event.getSource().getDirectEntity());
        if (isHosted(event.getEntity()) && owner instanceof ServerPlayer player && event.getAmount() > 0
                && ALIVE.equals(participant(event.getEntity(), player.getUUID()).getString("State"))) {
            double amount = Math.min(event.getAmount(), event.getEntity().getHealth());
            var entry = participant(event.getEntity(), player.getUUID());
            entry.putDouble("BossDamage", entry.getDouble("BossDamage") + amount);
            contribute(player, amount);
        }
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof LivingEntity boss
                && isHosted(boss) && boss == encounter(player.getUUID())) {
            contribute(player, Math.min(event.getAmount(), player.getMaxHealth()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            if (isHosted(event.getEntity())) {
                LivingEntity boss = event.getEntity();
                data(boss).putLong("VictoryAt", now(boss));
                if (!HostedEncounterApi.isRaid(boss)) {
                    eligible(boss).forEach(id -> EncounterData.addParticipant(boss.getPersistentData(), id));
                    WorldTierApi.profile(boss).ifPresent(profile -> eligible(boss).forEach(id -> profile.victoryAdvancementIds()
                            .forEach(advancement -> HostedRewards.get(boss.getServer()).award(boss.getServer(), id, advancement))));
                }
            }
            return;
        }
        LivingEntity boss = encounter(player.getUUID());
        if (boss == null) {
            return;
        }
        CompoundTag entry = participant(boss, player.getUUID());
        if (!net.minecraftforge.fml.ModList.get().isLoaded("playerrevive") && !boss.getPersistentData().getBoolean("jem_solo") && players(boss).size() > 1 && ALIVE.equals(entry.getString("State")) && !entry.getBoolean("DownedUsed")) {
            event.setCanceled(true);
            entry.putInt("Downs", entry.getInt("Downs") + 1);
            entry.putBoolean("DownedUsed", true);
            entry.putString("State", DOWNED);
            entry.putLong("BleedoutUntil", now(boss) + HostedConfig.BLEEDOUT.get());
            entry.putDouble("X", player.getX());
            entry.putDouble("Y", player.getY());
            entry.putDouble("Z", player.getZ());
            player.getPersistentData().putUUID(DOWNED_MARKER, boss.getUUID());
            player.setHealth(1);
        } else loseLife(event, boss, player, entry);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        LivingEntity boss = event.getEntity();
        if (!isHosted(boss) || !(boss.level() instanceof ServerLevel level)) {
            return;
        }
        List<UUID> recipients = eligible(boss).stream().sorted().toList();
        boolean raid = HostedEncounterApi.isRaid(boss);
        if (raid) event.getDrops().removeIf(drop -> drop.getItem().is(FORBIDDEN));
        if (recipients.isEmpty()) {
            close(boss, true);
            return;
        }
        List<ItemStack> loot = new ArrayList<>();
        for (var drop : event.getDrops()) {
            ItemStack stack = drop.getItem().copy();
            if (raid && stack.is(RESOURCES)) {
                stack.setCount(stack.getCount() * HostedConfig.RESOURCE_MULTIPLIER.get());
            }
            loot.add(stack);
        }
        if (raid) {
            for (UUID recipient : recipients) {
                List<RewardGroup> groups = new ArrayList<>();
                if (!loot.isEmpty()) groups.add(new RewardGroup("reward.jem_world_boss_tiers.native_loot", loot));
                groups.add(new RewardGroup("reward.jem_world_boss_tiers.raid_cache",
                        RaidRewardPool.roll(level, WorldTierApi.currentTier(level.getServer()))));
                PendingRewardContainer.enqueueGrouped(level.getServer(), recipient, boss.getStringUUID(),
                        "reward.jem_world_boss_tiers.raid_loot", groups);
            }
        } else {
            HostedRewards.get(level.getServer()).distribute(level.getServer(), recipients, loot, boss.getStringUUID());
        }
        event.getDrops().clear();
        close(boss, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void experience(LivingExperienceDropEvent event) {
        LivingEntity boss = event.getEntity();
        if (!isHosted(boss) || event.getDroppedExperience() <= 0 || boss.getServer() == null) return;
        int experience = event.getDroppedExperience();
        eligible(boss).stream().map(boss.getServer().getPlayerList()::getPlayer).filter(java.util.Objects::nonNull)
                .forEach(player -> com.siirio.jemworldbosstiers.api.ExperienceRewardApi.give(player, experience));
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % HostedConfig.DELIVERY_INTERVAL.get() == 0) {
            event.getServer().getPlayerList().getPlayers().forEach(player -> HostedRewards.get(player.server).deliver(player));
        }
        for (LivingEntity boss : List.copyOf(ACTIVE.values())) {
            if (data(boss).contains("VictoryAt")) {
                continue;
            }
            boolean remains = false;
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
                    entry.putString("State", ALIVE);
                    if(player!=null) loseLife(null,boss,player,entry);
                    else entry.putString("State",ELIMINATED);
                } else if (entry.contains("DisconnectUntil") && now(boss) >= entry.getLong("DisconnectUntil")) {
                    entry.putString("State", ELIMINATED);
                }
                if (player != null && DOWNED.equals(entry.getString("State"))) {
                    player.setDeltaMovement(0, 0, 0);
                    if (player.level() == boss.level() && player.distanceToSqr(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")) > 0.01) {
                        player.connection.teleport(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z"), player.getYRot(), player.getXRot());
                    }
                    player.stopUsingItem();
                }
                remains |= !ELIMINATED.equals(entry.getString("State"));
            }
            if (!remains) {
                close(boss, false);
            } else {
                List<ServerPlayer> effectTargets=players(boss).getAllKeys().stream()
                        .map(UUID::fromString)
                        .map(event.getServer().getPlayerList()::getPlayer)
                        .filter(java.util.Objects::nonNull)
                        .filter(player->ALIVE.equals(participant(boss,player.getUUID()).getString("State"))&&inside(boss,player))
                        .toList();
                BossPeriodicEffects.tick(boss,effectTargets);
            }
        }
    }

    private static void loseLife(LivingDeathEvent event, LivingEntity boss, ServerPlayer player, CompoundTag entry) {
        if(event!=null) event.setCanceled(true);
        player.getPersistentData().remove(DOWNED_MARKER);
        int livesLost=entry.getInt("LivesLost")+1;
        entry.putInt("LivesLost",livesLost);
        if(livesLost<3) {
            entry.putString("State",ALIVE);
            entry.putLong("ImmuneUntil",now(boss)+HostedConfig.IMMUNITY.get());
            player.setHealth(Math.max(1F,player.getMaxHealth()*.5F));
            player.teleportTo((ServerLevel)boss.level(),boss.getX(),boss.getY(),boss.getZ(),player.getYRot(),player.getXRot());
            player.fallDistance=0;
            return;
        }
        entry.putString("State",ELIMINATED);
        var key=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(entry.getString("OriginDimension")));
        var level=player.server.getLevel(key);
        if(level!=null) player.teleportTo(level,entry.getDouble("OriginX"),entry.getDouble("OriginY"),entry.getDouble("OriginZ"),entry.getFloat("OriginYaw"),entry.getFloat("OriginPitch"));
        player.setHealth(Math.max(1F,player.getMaxHealth()*.5F));
        player.fallDistance=0;
    }

    private static void close(LivingEntity boss, boolean success) {
        boolean raid = HostedEncounterApi.isRaid(boss);
        Set<UUID> eligible = success ? eligible(boss) : Set.of();
        ACTIVE.remove(boss.getUUID());
        BossPeriodicEffects.clear(boss);
        for (String id : players(boss).getAllKeys()) {
            CompoundTag entry = players(boss).getCompound(id);
            if (DOWNED.equals(entry.getString("State")) || entry.getBoolean("BleedoutDeath")) {
                HostedRewards.get(boss.getServer()).resolveDowned(boss.getServer(), UUID.fromString(id), success && !entry.getBoolean("BleedoutDeath"));
            }
        }
        MinecraftForge.EVENT_BUS.post(new HostedEncounterClosedEvent(boss, raid, success, eligible));
        boss.getPersistentData().remove(ROOT);
        if (!success && boss.isAlive()) {
            boolean preserveRaid=raid&&HostedEncounterApi.preservesRaidBoss(boss);
            if (raid&&!preserveRaid) {
                boss.discard();
            } else {
                applyScale(boss, Attributes.MAX_HEALTH, 1);
                applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
                boss.setHealth(boss.getMaxHealth());
                if (boss instanceof Mob mob) {
                    mob.setTarget(null);
                }
                if(preserveRaid) {
                    HostedEncounterApi.restorePreparedRaid(boss);
                    HostedLobby.hold(boss);
                }
            }
        }
    }

    public static boolean cancel(LivingEntity boss) {
        if (!isHosted(boss)) return false;
        close(boss, false);
        return true;
    }

    public static boolean leave(ServerPlayer player) {
        LivingEntity boss = encounter(player.getUUID());
        if (boss == null) return false;
        CompoundTag entry = participant(boss, player.getUUID());
        if (ELIMINATED.equals(entry.getString("State"))) return false;
        entry.putString("State", ELIMINATED);
        entry.remove("BleedoutUntil");
        entry.remove("DisconnectUntil");
        player.getPersistentData().remove(DOWNED_MARKER);
        var level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(entry.getString("OriginDimension"))));
        if (level != null) player.teleportTo(level, entry.getDouble("OriginX"), entry.getDouble("OriginY"), entry.getDouble("OriginZ"), entry.getFloat("OriginYaw"), entry.getFloat("OriginPitch"));
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;
        return true;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LivingEntity boss = encounter(event.getEntity().getUUID());
        if (boss != null) {
            participant(boss, event.getEntity().getUUID()).putLong("DisconnectUntil", now(boss) + HostedConfig.GRACE.get());
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
                    entry.putString("State", ELIMINATED);
                }
                entry.remove("DisconnectUntil");
            } else if (player.getPersistentData().hasUUID(DOWNED_MARKER)) {
                player.getPersistentData().remove(DOWNED_MARKER);
                player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
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
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity boss && isHosted(boss)) {
            applyScale(boss, Attributes.ATTACK_DAMAGE, 1);
            ACTIVE.put(boss.getUUID(), boss);
            for (String id : players(boss).getAllKeys()) {
                CompoundTag entry = players(boss).getCompound(id);
                if (boss.getServer().getPlayerList().getPlayer(UUID.fromString(id)) == null && !entry.contains("DisconnectUntil")) {
                    entry.putLong("DisconnectUntil", now(boss) + HostedConfig.GRACE.get());
                }
            }
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity boss && ACTIVE.containsKey(boss.getUUID())) {
            if (boss.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                ACTIVE.remove(boss.getUUID());
            } else {
                close(boss, data(boss).contains("VictoryAt"));
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppingEvent event) {
        ACTIVE.clear();
    }
}
