package com.siirio.jemworldbosstiers;

import com.siirio.jemworldbosstiers.balance.BalanceDataLoader;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.balance.EquipmentScaler;
import com.siirio.jemworldbosstiers.balance.EquipmentRefreshPolicy;
import com.siirio.jemworldbosstiers.balance.OrdinaryHostileScaler;
import com.siirio.jemworldbosstiers.balance.ProgressionArmor;
import com.siirio.jemworldbosstiers.network.ClientTierState;
import com.siirio.jemworldbosstiers.network.TierNetwork;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.progression.TierAdvancements;
import com.siirio.jemworldbosstiers.progression.BossDefeatPolicy;
import com.siirio.jemworldbosstiers.encounter.EncounterData;
import com.siirio.jemworldbosstiers.command.TierCommands;
import com.siirio.jemworldbosstiers.event.WorldTierChangedEvent;
import com.siirio.jemworldbosstiers.enchantment.TierEnchantments;
import com.siirio.jemworldbosstiers.loot.TierLoot;
import com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment;
import com.siirio.jemworldbosstiers.revival.ArenaService;
import com.siirio.jemworldbosstiers.revival.RevivalService;
import com.siirio.jemworldbosstiers.revival.ArenaProtection;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import com.siirio.jemworldbosstiers.revival.BossRespawnScheduler;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.api.BossRevivalApi;
import com.siirio.jemworldbosstiers.api.RaidArenaApi;
import com.siirio.jemworldbosstiers.protection.WeaponProtection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

@Mod(JemWorldBossTiers.MOD_ID)
public final class JemWorldBossTiers {
    public static final String MOD_ID = "jem_world_boss_tiers";
    private static final String PROGRESSION_PROJECTILE = "JEMProgressionProjectile";
    private static final int CRITICAL_TIER = 3;
    private static final float CRITICAL_CHANCE = 0.15F;
    private static final float CRITICAL_MULTIPLIER = 1.5F;
    private static final int CRITICAL_PARTICLE_COUNT = 18;
    private static final float HOSTED_BOSS_DAMAGE_STRENGTH = 0.88F;
    private final BossRespawnScheduler bossRespawns = new BossRespawnScheduler();

    public JemWorldBossTiers() {
        BossRevivalApi.install(bossRespawns::reviveNearest);
        RaidArenaApi.installAnchorMigrationHandler(bossRespawns::anchorVerified);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                com.siirio.jemworldbosstiers.encounter.HostedConfig.SPEC);
        TierEnchantments.register(FMLJavaModLoadingContext.get().getModEventBus());
        TierLoot.register(FMLJavaModLoadingContext.get().getModEventBus());
        TierNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RevivalService());
        MinecraftForge.EVENT_BUS.register(bossRespawns);
        MinecraftForge.EVENT_BUS.register(new WeaponProtection());
    }

    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(BalanceDataLoader.bosses());
        event.addListener(BalanceDataLoader.rewards());
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        TierCommands.register(event.getDispatcher());
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public void preserveBossHealthScaling(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && !eventOwned(event.getEntity()) && boss(event.getEntity()).isPresent())
            event.setAmount((float) (event.getAmount() / EncounterScaler.healthAbsorption(event.getEntity())));
    }

    @SubscribeEvent
    public void scaleDamage(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        if (!eventOwned(target)) boss(target).ifPresent(profile -> {
            EncounterScaler.ensureScaled(target, profile);
            if (event.getSource().getEntity() instanceof ServerPlayer player) EncounterData.addParticipant(target.getPersistentData(), player.getUUID());
        });
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player && isProgressionAttack(event, player)) {
            int tier = WorldTierData.get(player.server).tier();
            float amount = event.getAmount() * progressionMultiplier(tier);
            if (tier >= CRITICAL_TIER && player.getRandom().nextFloat() < CRITICAL_CHANCE) {
                amount *= CRITICAL_MULTIPLIER;
                if (target.level() instanceof ServerLevel level) {
                    level.sendParticles(
                            ParticleTypes.CRIT,
                            target.getX(),
                            target.getY() + target.getBbHeight() * 0.5D,
                            target.getZ(),
                            CRITICAL_PARTICLE_COUNT,
                            target.getBbWidth() * 0.4D,
                            target.getBbHeight() * 0.35D,
                            target.getBbWidth() * 0.4D,
                            0.15D
                    );
                }
            }
            event.setAmount(amount);
        }
        if (attacker instanceof LivingEntity livingAttacker && !eventOwned(livingAttacker)) {
            boss(livingAttacker).ifPresent(profile -> {
                int tier = EncounterScaler.ensureScaled(livingAttacker, profile);
                double factor = profile.scalesWithWorldTier() ? com.siirio.jemworldbosstiers.balance.BossTierScaling.boost(tier) : 1;
                if (profile.specialDamageMultiplier() != null
                        && !com.siirio.jemworldbosstiers.api.HostedEncounterApi.isRaid(livingAttacker)) {
                    factor *= profile.specialDamageMultiplier().valueAt(tier);
                }
                factor *= com.siirio.jemworldbosstiers.encounter.HostedEncounters.damageFactor(livingAttacker);
                event.setAmount((float) (event.getAmount() * factor));
            });
        }
        Entity owner = com.siirio.jemworldbosstiers.api.HostedEncounterApi.damageOwner(attacker);
        if (owner == null) owner = com.siirio.jemworldbosstiers.api.HostedEncounterApi.damageOwner(event.getSource().getDirectEntity());
        if (owner instanceof LivingEntity hostedBoss
                && com.siirio.jemworldbosstiers.encounter.HostedEncounters.isHosted(hostedBoss)) {
            event.setAmount(event.getAmount() * HOSTED_BOSS_DAMAGE_STRENGTH);
        }
        ProgressionArmor.apply(event);
    }

    @SubscribeEvent
    public void initializeEncounter(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractArrow arrow) {
            applyProgressionArrow(arrow);
        }
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && !eventOwned(living)) {
            boss(living).ifPresent(profile -> EncounterScaler.ensureScaled(living, profile));
            OrdinaryHostileScaler.apply(living, living.getServer());
        }
    }

    private static boolean eventOwned(LivingEntity entity) {
        return entity.getPersistentData().getBoolean("jem:event_spawned");
    }

    @SubscribeEvent
    public void prepareProgressionArrowImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide() && event.getProjectile() instanceof AbstractArrow arrow) {
            applyProgressionArrow(arrow);
        }
    }

    @SubscribeEvent
    public void rescaleLoadedHostiles(WorldTierChangedEvent event) {
        event.server().execute(() -> OrdinaryHostileScaler.reconcile(event.server()));
    }

    @SubscribeEvent
    public void accelerateProgressionRangedUse(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getItem().getItem() instanceof ProjectileWeaponItem)
                || progressionLevel(event.getItem()) <= 0
                || WorldTierData.get(player.server).tier() < 2) {
            return;
        }
        event.setDuration(Math.max(1, event.getDuration() - 1));
    }

    @SubscribeEvent
    public void finalizeEncounterSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!event.getLevel().isClientSide()) {
            WorldTierApi.finalizeNativeSpawn(event.getEntity(), event.getSpawnType().name());
        }
    }

    @SubscribeEvent
    public void protectBlocksFromBossExplosions(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity source = event.getExplosion().getIndirectSourceEntity();
        if (source == null) {
            source = event.getExplosion().getDirectSourceEntity();
        }
        Entity finalSource = source;
        event.getAffectedBlocks().removeIf(position -> !ArenaProtection.canDestroy(finalSource, level, position));
    }

    @SubscribeEvent
    public void releaseRemovedEncounter(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Entity.RemovalReason reason = living.getRemovalReason();
        if (reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        EncounterData.read(living.getPersistentData()).filter(encounter -> encounter.arenaId() != null)
                .ifPresent(encounter -> ArenaService.unlockAndRelease(WorldTierData.get(level.getServer()), encounter.arenaId(), living.getUUID()));
    }

    @SubscribeEvent
    public void recordBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        com.siirio.jemworldbosstiers.balance.BossProfile profile = boss(event.getEntity()).orElse(null);
        if (profile == null) {
            return;
        }
        if (event.getEntity().getPersistentData().getBoolean("jem:event_spawned") || com.siirio.jemworldbosstiers.api.HostedEncounterApi.isRaid(event.getEntity())) {
            return;
        }
        EncounterData encounter = EncounterData.read(event.getEntity().getPersistentData()).orElse(null);
        WorldTierData data = WorldTierData.get(level.getServer());
        ArenaRecord arena = encounter != null && encounter.arenaId() != null
                ? data.arena(encounter.arenaId()).orElseGet(() -> ArenaService.findOrRegister(event.getEntity(), profile))
                : ArenaService.findOrRegister(event.getEntity(), profile);
        ArenaRecord released = (arena.structureArena() ? arena : arena.withRespawnPosition(event.getEntity().blockPosition()))
                .unlock().release(event.getEntity().getUUID());
        data.putArena(released);
        bossRespawns.scheduleAfterDeath(
                level,
                profile,
                released,
                BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()),
                event.getEntity().blockPosition()
        );
        if (encounter == null) {
            return;
        }
        if (profile.countsTowardWorldTier() && BossDefeatPolicy.canProgress(encounter, event.getSource().is(DamageTypes.GENERIC_KILL))) {
            recordVictory(level.getServer(), profile);
        }
    }

    @SubscribeEvent
    public void recordVictoryAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BalanceRegistry.bossByVictoryAdvancement(event.getAdvancement().getId())
                .filter(com.siirio.jemworldbosstiers.balance.BossProfile::countsTowardWorldTier)
                .ifPresent(profile -> recordVictory(player.server, profile));
    }

    @SubscribeEvent
    public void scaleProgressionAttributes(ItemAttributeModifierEvent event) {
        EquipmentScaler.apply(event);
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public void expireProgressionArmorBoost(TickEvent.PlayerTickEvent event) {
        ProgressionArmor.tick(event);
    }

    @SubscribeEvent
    public void addEquipmentTooltip(ItemTooltipEvent event) {
        if (WeaponProtection.bossWeapon(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.jem_world_boss_tiers.protected_boss_weapon"));
        }
        if (EnchantmentHelper.getEnchantments(event.getItemStack()).containsKey(TierEnchantments.PROGRESSION.get())
                && ProgressionEnchantment.isEligible(event.getItemStack())) {
            String type = ProgressionEnchantment.isArmorEligible(event.getItemStack()) ? "armor" : "weapon";
            event.getToolTip().add(Component.translatable(
                    "tooltip.jem_world_boss_tiers.progression." + type + "." + Math.min(CRITICAL_TIER, ClientTierState.tier())
            ));
        }
    }

    private static boolean isProgressionAttack(LivingHurtEvent event, ServerPlayer player) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow arrow) {
            return arrow.getPersistentData().getBoolean(PROGRESSION_PROJECTILE);
        }
        return direct == player && progressionLevel(player.getMainHandItem()) > 0;
    }

    private static void applyProgressionArrow(AbstractArrow arrow) {
        int tier = arrow.getPersistentData().getInt("JEMProgressionTier");
        if (tier <= 0 && arrow.getOwner() instanceof ServerPlayer player
                && progressionLevel(projectileWeapon(player)) > 0) {
            tier = WorldTierData.get(player.server).tier();
            arrow.getPersistentData().putBoolean(PROGRESSION_PROJECTILE, true);
            arrow.getPersistentData().putInt("JEMProgressionTier", tier);
        }
        int piercing = tier >= 3 ? 3 : tier >= 2 ? 2 : 0;
        if (piercing > arrow.getPierceLevel()) {
            arrow.setPierceLevel((byte) piercing);
        }
    }

    private static ItemStack projectileWeapon(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof ProjectileWeaponItem
                && progressionLevel(player.getMainHandItem()) > 0) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof ProjectileWeaponItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static int progressionLevel(ItemStack stack) {
        if (!ProgressionEnchantment.isEligible(stack)) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(TierEnchantments.PROGRESSION.get(), stack);
    }

    private static float progressionMultiplier(int tier) {
        return switch (tier) {
            case 1 -> 1.0F;
            case 2 -> 1.05F;
            default -> 1.10F;
        };
    }

    private static void syncPlayer(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            reconcileVictories(serverPlayer);
            EquipmentScaler.refresh(serverPlayer);
            TierAdvancements.awardCurrent(serverPlayer, WorldTierData.get(serverPlayer.server).tier());
            TierNetwork.sync(serverPlayer);
        }
    }

    private static void reconcileVictories(ServerPlayer player) {
        WorldTierData data = WorldTierData.get(player.server);
        int previousTier = data.tier();
        boolean recorded = false;
        for (ResourceLocation advancementId : BalanceRegistry.victoryAdvancementIds()) {
            net.minecraft.advancements.Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
            if (advancement == null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                continue;
            }
            com.siirio.jemworldbosstiers.balance.BossProfile profile = BalanceRegistry.bossByVictoryAdvancement(advancementId)
                    .filter(com.siirio.jemworldbosstiers.balance.BossProfile::countsTowardWorldTier)
                    .orElse(null);
            recorded |= profile != null && data.recordDefeat(profile.key());
        }
        if (EquipmentRefreshPolicy.required(previousTier, data.tier())) {
            player.server.getPlayerList().getPlayers().forEach(online -> {
                EquipmentScaler.refresh(online);
                TierAdvancements.awardCurrent(online, data.tier());
            });
            MinecraftForge.EVENT_BUS.post(new WorldTierChangedEvent(player.server, previousTier, data.tier()));
        }
        if (recorded) {
            TierNetwork.syncAll(player.server);
        }
    }

    public static void recordVictory(net.minecraft.server.MinecraftServer server, com.siirio.jemworldbosstiers.balance.BossProfile profile) {
        WorldTierData data = WorldTierData.get(server);
        int previousTier = data.tier();
        if (!data.recordDefeat(profile.key())) {
            return;
        }
        if (EquipmentRefreshPolicy.required(previousTier, data.tier())) {
            server.getPlayerList().getPlayers().forEach(player -> {
                EquipmentScaler.refresh(player);
                TierAdvancements.awardCurrent(player, data.tier());
            });
            MinecraftForge.EVENT_BUS.post(new WorldTierChangedEvent(server, previousTier, data.tier()));
        }
        TierNetwork.syncAll(server);
    }

    public static int overrideWorldTier(net.minecraft.server.MinecraftServer server,int tier) {
        return changeWorldTier(server,data->data.setTierOverride(tier));
    }

    public static int resetWorldTier(net.minecraft.server.MinecraftServer server) {
        return changeWorldTier(server,WorldTierData::clearTierOverride);
    }

    private static int changeWorldTier(net.minecraft.server.MinecraftServer server,java.util.function.Consumer<WorldTierData> change) {
        WorldTierData data=WorldTierData.get(server);
        int previous=data.tier();
        change.accept(data);
        int current=data.tier();
        if(previous!=current) {
            server.getPlayerList().getPlayers().forEach(player->{
                EquipmentScaler.refresh(player);
                TierAdvancements.awardCurrent(player,current);
            });
            MinecraftForge.EVENT_BUS.post(new WorldTierChangedEvent(server,previous,current));
        }
        TierNetwork.syncAll(server);
        return current;
    }

    private static java.util.Optional<com.siirio.jemworldbosstiers.balance.BossProfile> boss(LivingEntity entity) {
        return BalanceRegistry.boss(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }
}
