package com.siirio.jemvillagertalking.speech;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class VillagerSpeechEvents {
    private static final int CUSTOM_BOSS_HEALTH = 100;
    private static final String LAST_CHEST_TAG = "JemSpeechLastChest";
    private static final String LAST_DOOR_TAG = "JemSpeechLastDoor";
    private static final String MODE_SWITCH_COUNT_TAG = "JemSpeechModeSwitches";
    private static final AtomicLong OCCURRENCES = new AtomicLong();
    private static final Map<MinecraftServer, Boolean> RESTART_PENDING = new IdentityHashMap<>();

    private VillagerSpeechEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            VillagerReactionTracker.tick(level);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RESTART_PENDING.put(event.getServer(), true);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RESTART_PENDING.remove(event.getServer());
        VillagerReactionRegistry.clearRuntimeState(event.getServer());
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityMounting() instanceof Villager villager)
                || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntityBeingMounted() instanceof Boat) {
            emit(level, villager.blockPosition(), "villager_boat_trapped", villager, null, "");
        } else if (event.getEntityBeingMounted() instanceof Minecart) {
            emit(level, villager.blockPosition(), "villager_minecart_trapped", villager, null, "");
        }
    }

    @SubscribeEvent
    public static void onBell(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getSound() == null) {
            return;
        }
        ResourceLocation sound = ForgeRegistries.SOUND_EVENTS.getKey(event.getSound().value());
        ResourceLocation bell = ForgeRegistries.SOUND_EVENTS.getKey(SoundEvents.BELL_BLOCK);
        if (bell != null && bell.equals(sound)) {
            emit(level, BlockPos.containing(event.getPosition()), "bell_rings", null, null, "");
        }
    }

    @SubscribeEvent
    public static void onTradeOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getContainer() instanceof MerchantMenu) {
            emit(player.serverLevel(), player.blockPosition(), "trade_open", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onTradeComplete(TradeWithVillagerEvent event) {
        if (event.getAbstractVillager() instanceof Villager villager && event.getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), villager.blockPosition(), "trade_complete", villager, player, "");
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Villager villager && attacker instanceof ServerPlayer player) {
            emit(level, villager.blockPosition(), villager.isBaby() ? "child_hurt" : "hurt", villager, player, "");
            return;
        }
        if (event.getEntity() instanceof IronGolem golem) {
            emit(level, golem.blockPosition(), "golem_damaged", golem, attacker, "");
            return;
        }
        if (attacker instanceof ServerPlayer player && event.getEntity() instanceof Mob) {
            emit(level, event.getEntity().blockPosition(), "player_attacks_mob_nearby", event.getEntity(), player, "");
            if (event.getEntity() instanceof Enemy && level.isVillage(player.blockPosition())) {
                emit(level, player.blockPosition(), "player_defends_village", event.getEntity(), player, "");
            }
        } else if (attacker instanceof ServerPlayer player && event.getEntity() instanceof ServerPlayer) {
            emit(level, player.blockPosition(), "players_fight_each_other", event.getEntity(), player, "");
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getNewTarget() instanceof Villager villager) {
            emit(level, villager.blockPosition(), threatEvent(event.getEntity()), event.getEntity(), null, "");
        } else if (event.getEntity() instanceof IronGolem golem && event.getNewTarget() != null) {
            emit(level, golem.blockPosition(), "golem_fights", golem, event.getNewTarget(), "");
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(level, player.blockPosition(), "one_player_dies_near_group", player, killer, "");
            return;
        }
        LivingEntity living = event.getEntity();
        if (living.getMaxHealth() >= CUSTOM_BOSS_HEALTH) {
            String namespace = registryId(living).getNamespace();
            emit(level, living.blockPosition(), namespace.equals("minecraft") ? "boss_defeated" : "custom_boss_killed", living, killer, "");
        } else if (killer instanceof ServerPlayer && event.getEntity() instanceof Enemy) {
            emit(level, event.getEntity().blockPosition(), "danger_ended", event.getEntity(), killer, "");
        }
    }

    @SubscribeEvent
    public static void onUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().isEdible()) {
            emit(level, player.blockPosition(), "player_eats", player, player, "");
        } else if (event.getEntity() instanceof Witch witch) {
            emit(level, witch.blockPosition(), "witch_drinks_potion", witch, null, "");
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack item = event.getEntity().getItem();
            emit(player.serverLevel(), event.getEntity().blockPosition(), tossEvent(item), event.getEntity(), player, "");
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), event.getItem().blockPosition(), "player_picks_item_back_up", event.getItem(), player, "");
        }
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), player.blockPosition(), "player_sleeps", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos position = event.getPos();
        var block = event.getLevel().getBlockState(position).getBlock();
        if (block instanceof DoorBlock) {
            boolean repeated = player.getPersistentData().contains(LAST_DOOR_TAG)
                    && player.getPersistentData().getLong(LAST_DOOR_TAG) == position.asLong();
            player.getPersistentData().putLong(LAST_DOOR_TAG, position.asLong());
            long nearbyVillagers = player.serverLevel().getEntitiesOfClass(
                    Villager.class,
                    new net.minecraft.world.phys.AABB(position).inflate(8.0D),
                    Villager::isAlive
            ).size();
            String eventId = !repeated ? "player_opens_door"
                    : nearbyVillagers >= 2 ? "two_villagers_same_door"
                    : "player_uses_same_door";
            emit(player.serverLevel(), position, eventId, player, player, repeated ? "remembered" : "");
        } else if (block instanceof ChestBlock) {
            boolean repeated = player.getPersistentData().contains(LAST_CHEST_TAG)
                    && player.getPersistentData().getLong(LAST_CHEST_TAG) == position.asLong();
            player.getPersistentData().putLong(LAST_CHEST_TAG, position.asLong());
            if (repeated) {
                emit(player.serverLevel(), position, "player_opens_same_chest", player, player, "remembered");
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getTarget() instanceof IronGolem && player.getItemInHand(event.getHand()).is(Items.IRON_INGOT)) {
            emit(player.serverLevel(), event.getTarget().blockPosition(), "golem_repaired", event.getTarget(), player, "");
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var block = event.getPlacedBlock().getBlock();
        String path = registryId(block).getPath();
        String eventId = block instanceof BedBlock ? "player_places_bed"
                : block instanceof CampfireBlock ? "campfire_placed"
                : path.equals("glowstone") ? "glowstone_placed"
                : path.equals("lantern") ? "lantern_placed_near_villager"
                : path.equals("soul_lantern") ? "soul_lantern_placed"
                : path.equals("sea_lantern") ? "sea_lantern_placed"
                : path.endsWith("torch") ? "torch_placed_near_villager"
                : path.equals("tnt") ? "tnt_placed"
                : !registryId(block).getNamespace().equals("minecraft") ? "modded_block_placed" : null;
        if (eventId != null) {
            emit(level, event.getPos(), eventId, player, player, "");
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var block = event.getState().getBlock();
        String eventId = block instanceof CropBlock ? "player_breaks_crop"
                : !registryId(block).getNamespace().equals("minecraft") ? "modded_block_removed"
                : level.isVillage(event.getPos()) ? "player_breaks_village_block"
                : player.isCreative() ? "player_creative_breaks_instantly"
                : event.getState().getDestroySpeed(level, event.getPos()) == 0.0F ? "instant_block_break" : null;
        if (eventId != null) {
            emit(level, event.getPos(), eventId, player, player, "");
        }
    }

    @SubscribeEvent
    public static void onCropGrowth(BlockEvent.CropGrowEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            emit(level, event.getPos(), "crop_grown", null, null, "");
        }
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            level.getEntitiesOfClass(
                    Villager.class,
                    new net.minecraft.world.phys.AABB(event.getFaceOffsetPos()).inflate(1.0D),
                    Villager::isAlive
            ).stream().findFirst().ifPresent(villager ->
                    emit(level, event.getFaceOffsetPos(), "piston_moves_villager", villager, null, ""));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            emit(level, player.blockPosition(), "player_falls_near_villager", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            emit(level, player.blockPosition(), "player_teleports", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), player.blockPosition(), "player_uses_command", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        emit(player.serverLevel(), player.blockPosition(), "player_returns_after_absence", player, player, "");
        if (RESTART_PENDING.remove(player.server) != null) {
            VillagerReactionRegistry.emitWorld(
                    "after_server_restart",
                    occurrenceId(player.serverLevel(), "after_server_restart"),
                    player.server,
                    player,
                    player,
                    "remembered"
            );
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), player.blockPosition(), "player_logs_out_in_front", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), player.blockPosition(), "player_respawns", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(player.serverLevel(), player.blockPosition(), "player_teleports", player, player, "");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGameModeChanged(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int switches = player.getPersistentData().getInt(MODE_SWITCH_COUNT_TAG) + 1;
        player.getPersistentData().putInt(MODE_SWITCH_COUNT_TAG, switches);
        if (switches >= 2) {
            emit(player.serverLevel(), player.blockPosition(), "player_switches_modes_repeatedly", player, player, "repeat");
        }
        if (event.getCurrentGameMode() == GameType.SURVIVAL && event.getNewGameMode() == GameType.CREATIVE) {
            emit(player.serverLevel(), player.blockPosition(), "player_enters_creative", player, player, "");
        } else if (event.getNewGameMode() == GameType.SPECTATOR) {
            emit(player.serverLevel(), player.blockPosition(), "player_enters_spectator", player, player, "");
        } else if (event.getNewGameMode() == GameType.SURVIVAL) {
            emit(player.serverLevel(), player.blockPosition(), "player_switches_survival", player, player, "");
        }
    }

    @SubscribeEvent
    public static void onEntityJoined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof PrimedTnt) {
            emit(level, entity.blockPosition(), "tnt_ignited", entity, null, "");
        } else if (entity instanceof Vex) {
            emit(level, entity.blockPosition(), "vex_spawned", entity, null, "");
        } else if (entity instanceof LivingEntity living && living.getMaxHealth() >= CUSTOM_BOSS_HEALTH
                && !registryId(entity).getNamespace().equals("minecraft")) {
            emit(level, entity.blockPosition(), "custom_boss_spawned", entity, null, "");
        } else if (entity instanceof Enemy) {
            emit(level, entity.blockPosition(), threatEvent(entity), entity, null, "");
        }
    }

    @SubscribeEvent
    public static void onOptionalJemEvent(Event event) {
        if (!event.getClass().getName().equals("com.siirio.jemworldbosstiers.event.WorldTierChangedEvent")) {
            return;
        }
        try {
            MinecraftServer server = (MinecraftServer) event.getClass().getMethod("server").invoke(event);
            int previousTier = (int) event.getClass().getMethod("previousTier").invoke(event);
            int currentTier = (int) event.getClass().getMethod("currentTier").invoke(event);
            String eventId = currentTier <= previousTier ? "world_tier_changed"
                    : previousTier == 0 ? "world_tier_increases"
                    : "world_tier_increases_again";
            VillagerReactionRegistry.emitWorld(
                    eventId,
                    occurrenceId(server.overworld(), eventId),
                    server,
                    null,
                    null,
                    ""
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Invalid JEM World Tier event contract", exception);
        }
    }

    private static String tossEvent(ItemStack item) {
        String path = registryId(item).getPath();
        if (item.is(Items.EMERALD)) return "drop_emerald";
        if (item.is(Items.DIAMOND)) return "player_drops_diamonds";
        if (item.is(Items.ROTTEN_FLESH)) return "player_drops_rotten_flesh";
        if (item.is(Items.TOTEM_OF_UNDYING)) return "drop_totem";
        if (item.getHoverName() != null && item.hasCustomHoverName()) return "drop_named_item";
        if (item.getItem() instanceof BookItem) return "drop_book";
        if (item.getItem() instanceof PotionItem) return "drop_potion";
        if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) return "drop_shulker_box";
        if (item.getItem() instanceof TieredItem) return item.getItem() instanceof SwordItem ? "player_drops_sword" : "drop_tool";
        if (item.getItem() instanceof ArmorItem) return "drop_armor";
        if (item.getItem() instanceof MapItem) return "player_drops_map";
        if (item.getItem() instanceof BlockItem && item.getCount() >= 16) return "drop_many_blocks";
        if (item.isEdible()) return "drop_food";
        if (path.contains("ingot")) return "drop_ingot";
        if (path.contains("cursed") || path.contains("curse")) return "drop_cursed_item";
        return item.getCount() >= 16 ? "player_drops_many_items" : "player_drops_items";
    }

    private static String threatEvent(Entity entity) {
        if (entity instanceof Ravager) return "ravager_seen";
        if (entity instanceof Pillager) return "pillager_near_village";
        String path = registryId(entity).getPath();
        return switch (path) {
            case "creeper" -> "creeper_near_village";
            case "drowned" -> "drowned_near_village";
            case "skeleton" -> "skeleton_near_village";
            case "spider" -> "spider_near_village";
            case "witch" -> "witch_near_village";
            case "zombie" -> "zombie_near_village";
            default -> "danger";
        };
    }

    private static boolean emit(
            ServerLevel level,
            BlockPos origin,
            String eventId,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        return VillagerReactionRegistry.emitLocal(
                eventId,
                occurrenceId(level, eventId),
                level,
                origin,
                subject,
                actor,
                contextState
        );
    }

    private static String occurrenceId(ServerLevel level, String eventId) {
        return eventId + ":" + level.dimension().location() + ":" + level.getGameTime() + ":" + OCCURRENCES.incrementAndGet();
    }

    private static ResourceLocation registryId(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? new ResourceLocation("minecraft", "unknown") : id;
    }

    private static ResourceLocation registryId(net.minecraft.world.level.block.Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }

    private static ResourceLocation registryId(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }
}
