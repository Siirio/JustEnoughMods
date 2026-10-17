package com.siirio.jemprofessions.sandbox.core;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import com.siirio.jemprofessions.progress.SourceKind;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Integrated-sandbox effect owner for Architect, Farmer, Fisher, Miner and Explorer. */
public final class SandboxCoreEffects {
    private static final UUID HOTBAR = UUID.fromString("953cabee-0892-48e8-8bb7-a42e422f59b0");
    private static final UUID TRAVEL = UUID.fromString("f2bf018b-00be-44c1-ab40-0a6d9f329fc3");
    private static final Map<UUID, Movement> MOVEMENT = new HashMap<>();
    private static final Set<UUID> VEINS = new HashSet<>();
    private static final ThreadLocal<BreakCapture> BREAK = new ThreadLocal<>();
    private static final ArrayDeque<Runnable> DEFERRED = new ArrayDeque<>();
    private SandboxCoreEffects() { }
    public static void register() { MinecraftForge.EVENT_BUS.register(SandboxCoreEffects.class); MinecraftForge.EVENT_BUS.register(CoreFarming.class); MinecraftForge.EVENT_BUS.register(CoreFishing.class); }
    public static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        if (!SandboxProfessionSupport.allowed(player) || player.isSpectator() || !player.isAlive()) return false;
        return switch (node) {
            case "long_arms_1", "long_arms_2", "long_arms_3", "phantom_3", "phantom_5", "phantom_7", "architect_supply", "architect_command", "architect_hotbar" -> CoreBuilding.activate(player, node, input);
            case "crop_sense", "sweep", "harvest_master", "herd_call", "feed_herd", "quick_shear", "greenhouse_master", "greenhouse_keeper", "greenhouse_abundance" -> CoreFarming.activate(player, node, input);
            case "target_fishing", "rare_bait", "trophy_catch", "double_catch", "steady_line", "net_master" -> CoreFishing.activate(player, node, input);
            case "ore_sense", "ore_memory", "deep_resonance" -> { oreSense(player); yield true; }
            case "wall_climb", "ledge_catch", "portable_camp" -> CoreExploration.activate(player, node, input);
            default -> false;
        };
    }
    static boolean has(ServerPlayer player, ProfessionId profession, String node) { return SandboxProfessionSupport.enabled(player, profession, node); }
    static boolean eligible(ServerPlayer player) { return SandboxProfessionSupport.allowed(player) && !(player instanceof FakePlayer) && !player.isCreative() && !player.isSpectator() && player.isAlive(); }
    static String dimension(ServerPlayer player) { return player.level().dimension().location().toString(); }
    static String position(ServerPlayer player, BlockPos pos) { return dimension(player) + ":" + pos.asLong(); }
    static void tell(ServerPlayer player, String ru, String en) { SandboxProfessionSupport.message(player, ru, en); }
    static void credit(ServerPlayer player, ProfessionId profession, SourceKind source, ResourceLocation id, int amount, String counter, int cap, String receipt) {
        SandboxProfessionSupport.credit(player, profession, source, id, amount, counter, cap, receipt);
    }
    static void defer(Runnable task) { if (DEFERRED.size() < 4096) DEFERRED.addLast(task); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.getServer().isSingleplayer()) return;
        // Mark all placed positions, including creative/fake/modded placements, before a later transformation can make an ore.
        CoreDiscoveryLedger.placed(level, event.getPos());
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) multi.getReplacedBlockSnapshots().forEach(snapshot -> CoreDiscoveryLedger.placed(level, snapshot.getPos()));
        if (!(event.getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        List<BlockPos> positions = event instanceof BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots().stream().map(snapshot -> snapshot.getPos().immutable()).toList() : List.of(event.getPos().immutable());
        for (BlockPos pos : positions) {
            BlockState expected = level.getBlockState(pos);
            defer(() -> { if (!event.isCanceled() && player.isAlive() && level.hasChunkAt(pos) && level.getBlockState(pos).equals(expected)) committedPlacement(player, pos, expected); });
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.getServer().isSingleplayer()) return;
        var resolver = event.getStructureHelper(); if (resolver == null || !resolver.resolve()) return;
        for (BlockPos pos : resolver.getToPush()) {
            // Moving terrain is no longer natural at its destination, regardless of previous provenance.
            CoreDiscoveryLedger.placed(level, pos.relative(resolver.getPushDirection()));
        }
    }
    static void committedPlacement(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!eligible(player) || state.isAir()) return;
        CoreDiscoveryLedger.placed(player.serverLevel(), pos);
        var record = SandboxProfessionSupport.record(player); if (record == null) return;
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (CoreDiscoveryLedger.firstPlacement(player, pos)) {
            credit(player, ProfessionId.ARCHITECT, SourceKind.BLOCK_PLACED, block, 1, "architect:construction", 600, "place:" + position(player, pos));
        }
        if (record.claimDiscovery("architect:material:" + block)) {
            ProfessionSavedData.get(player.getServer()).changed();
            credit(player, ProfessionId.ARCHITECT, SourceKind.BLOCK_PLACED, block, 5, null, 0, "material:" + block);
        }
        if (player.getMainHandItem().getItem() instanceof BlockItem || player.getMainHandItem().isEmpty()) CoreBuilding.rememberSupply(player, state);
    }

    /** HEAD/RETURN seam of native destroyBlock; a cancellable pre-break event cannot prove a successful break. */
    public static BreakCapture beforeBreak(ServerPlayer player, BlockPos pos) {
        if (!eligible(player) || !player.serverLevel().hasChunkAt(pos)) return null;
        BlockState state = player.serverLevel().getBlockState(pos);
        BreakCapture capture = new BreakCapture(player, pos.immutable(), state, player.hasCorrectToolForDrops(state), BREAK.get());
        BREAK.set(capture); return capture;
    }
    public static void afterBreak(BreakCapture capture, boolean success) {
        if (capture == null) return;
        BREAK.set(capture.parent);
        if (!success || !eligible(capture.player) || capture.player.serverLevel().getBlockState(capture.pos).equals(capture.state)) return;
        ServerPlayer player = capture.player;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(capture.state.getBlock());
        if (capture.state.getBlock() instanceof CropBlock crop && crop.isMaxAge(capture.state)) {
            credit(player, ProfessionId.FARMER, SourceKind.CROP_HARVEST, id, 2, "farmer:crops", 500, UUID.randomUUID().toString());
        }
        if (!capture.state.is(Tags.Blocks.ORES) || !capture.correctTool || !natural(player, capture.pos)) return;
        credit(player, ProfessionId.MINER, SourceKind.BLOCK_BREAK, id, 3, "miner:ores", 750, "ore:" + position(player, capture.pos));
        if (has(player, ProfessionId.MINER, "bedrock_precision")) for (ItemEntity item : capture.items) {
            if (!item.isAlive() || !item.isAddedToWorld() || item.level() != player.level()) continue;
            player.getInventory().add(item.getItem()); if (item.getItem().isEmpty()) item.discard();
        }
        if (player.isShiftKeyDown() && !VEINS.contains(player.getUUID()) && veinEnabled(player)) mineVein(player, capture.pos, capture.state);
    }
    static boolean natural(ServerPlayer player, BlockPos pos) {
        return CoreDiscoveryLedger.natural(player.serverLevel(), pos)
                && !ProfessionSavedData.get(player.getServer()).wasPlayerPlaced(position(player, pos));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        BreakCapture capture = BREAK.get();
        if (capture != null && event.getEntity() instanceof ItemEntity item && !event.isCanceled()
                && item.level() == capture.player.level() && item.distanceToSqr(Vec3.atCenterOf(capture.pos)) < 9 && capture.items.size() < 128) capture.items.add(item);
    }
    private static boolean veinEnabled(ServerPlayer player) { return has(player, ProfessionId.MINER, "vein_miner") || has(player, ProfessionId.MINER, "vein_master") || has(player, ProfessionId.MINER, "bedrock_precision"); }
    private static void mineVein(ServerPlayer player, BlockPos origin, BlockState ore) {
        int limit = has(player, ProfessionId.MINER, "vein_master") || has(player, ProfessionId.MINER, "bedrock_precision") ? 32 : 8;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(); Set<BlockPos> seen = new HashSet<>(); queue.add(origin); seen.add(origin);
        VEINS.add(player.getUUID()); int broken = 1;
        try {
            while (!queue.isEmpty() && broken < limit && seen.size() < 1024 && veinEnabled(player)) {
                BlockPos pos = queue.removeFirst();
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction); if (!seen.add(next) || !player.serverLevel().hasChunkAt(next) || next.distSqr(origin) > 144) continue;
                    BlockState state = player.serverLevel().getBlockState(next);
                    if (!state.is(ore.getBlock()) || !natural(player, next) || !player.hasCorrectToolForDrops(state) || !player.getMainHandItem().is(ItemTags.PICKAXES)) continue;
                    if (!CoreActions.permitted(player, next, player.getMainHandItem())) continue;
                    if (player.gameMode.destroyBlock(next)) { broken++; queue.addLast(next); }
                    if (broken >= limit) break;
                }
            }
        } finally { VEINS.remove(player.getUUID()); }
    }
    @SubscribeEvent
    public static void gesture(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        if (player.isShiftKeyDown() && event.getItemStack().is(ItemTags.PICKAXES) && senseEnabled(player)) {
            oreSense(player); event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event.getItemStack().is(ItemTags.HOES) && has(player, ProfessionId.FARMER, "crop_sense")) {
            CoreFarming.sense(player); event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    private static boolean senseEnabled(ServerPlayer player) { return has(player, ProfessionId.MINER, "ore_sense") || has(player, ProfessionId.MINER, "ore_memory") || has(player, ProfessionId.MINER, "deep_resonance"); }
    private static void oreSense(ServerPlayer player) {
        if (!senseEnabled(player) || !player.getMainHandItem().is(ItemTags.PICKAXES)) { tell(player, "Возьми кирку в основную руку.", "Hold a pickaxe in your main hand."); return; }
        if (!SandboxProfessionSupport.cooldown(player, "miner:ore_sense", 900)) { tell(player, "Чувство руды ещё восстанавливается.", "Ore sense is cooling down."); return; }
        int radius = has(player, ProfessionId.MINER, "deep_resonance") ? 12 : 6;
        BlockPos nearest = null; double distance = Double.MAX_VALUE; BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            double sq = pos.distSqr(origin); if (sq > radius * radius || sq >= distance || !player.serverLevel().hasChunkAt(pos)) continue;
            if (player.serverLevel().getBlockState(pos).is(Tags.Blocks.ORES) && natural(player, pos)) { nearest = pos.immutable(); distance = sq; }
        }
        if (nearest == null) tell(player, "Поблизости природная руда не найдена.", "No natural ore nearby.");
        else if (has(player, ProfessionId.MINER, "ore_memory")) {
            BlockPos delta = nearest.subtract(origin);
            boolean ru = player.getLanguage().startsWith("ru");
            String direction = Math.abs(delta.getY()) > Math.max(Math.abs(delta.getX()), Math.abs(delta.getZ()))
                    ? delta.getY() > 0 ? ru ? "выше" : "above" : ru ? "ниже" : "below"
                    : Math.abs(delta.getX()) >= Math.abs(delta.getZ()) ? delta.getX() >= 0 ? ru ? "восток" : "east" : ru ? "запад" : "west"
                    : delta.getZ() >= 0 ? ru ? "юг" : "south" : ru ? "север" : "north";
            player.displayClientMessage(player.serverLevel().getBlockState(nearest).getBlock().getName().copy()
                    .append(": " + direction + ", " + (int) Math.ceil(Math.sqrt(distance)) + (ru ? " бл." : " blocks")), true);
        } else tell(player, "Поблизости есть природная руда.", "Natural ore is nearby.");
    }
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        if (event.getAmount() > 0) CoreExploration.hurt(player);
        if (has(player, ProfessionId.MINER, "stone_step") && event.getSource().is(DamageTypeTags.IS_FALL)
                && player.level().getBlockState(player.blockPosition().below()).is(Tags.Blocks.STONE)) event.setAmount(event.getAmount() * .9F);
        if (event.getAmount() > 0 && has(player, ProfessionId.MINER, "lava_shield") && event.getSource().is(DamageTypeTags.IS_FIRE)
                && SandboxProfessionSupport.cooldown(player, "miner:lava_shield", 12000)) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, false, true));
    }
    @SubscribeEvent
    public static void breathe(LivingBreatheEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && eligible(player) && !event.canBreathe() && player.isUnderWater()
                && player.getY() < player.level().getSeaLevel() - 16 && has(player, ProfessionId.MINER, "cave_breath") && player.getRandom().nextFloat() < .15F) event.setConsumeAirAmount(0);
    }
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.getServer().isDedicatedServer()) return;
        reconcile(player); CoreFarming.tick(player); CoreBuilding.tick(player); CoreExploration.tick(player);
        if (!SandboxProfessionSupport.allowed(player)) { MOVEMENT.remove(player.getUUID()); return; }
        Movement movement = MOVEMENT.computeIfAbsent(player.getUUID(), ignored -> new Movement(player));
        Vec3 current = player.position(); String dimension = dimension(player);
        boolean walking = !player.isPassenger() && !player.getAbilities().flying && !player.isFallFlying();
        boolean boating = player.getVehicle() != null && (player.getVehicle().getClass() == Boat.class || player.getVehicle().getClass() == net.minecraft.world.entity.vehicle.ChestBoat.class);
        boolean normal = eligible(player) && dimension.equals(movement.dimension) && current.distanceToSqr(movement.pos) < 256
                && (walking || boating) && player.tickCount > movement.suppressedUntil;
        if (normal && (player.chunkPosition().x != movement.chunkX || player.chunkPosition().z != movement.chunkZ)) {
            if (CoreDiscoveryLedger.firstVisit(player, player.chunkPosition().x, player.chunkPosition().z)) {
                credit(player, ProfessionId.EXPLORER, SourceKind.AREA_DISCOVERED, player.level().dimension().location(), 2, "explorer:chunks", 200,
                        "chunk:" + dimension + ":" + player.chunkPosition().x + ":" + player.chunkPosition().z);
                movement.travelUntil = player.tickCount + 600;
            }
        }
        movement.pos = current; movement.dimension = dimension; movement.chunkX = player.chunkPosition().x; movement.chunkZ = player.chunkPosition().z;
    }
    public static void teleported(ServerPlayer player) {
        Movement movement = MOVEMENT.computeIfAbsent(player.getUUID(), ignored -> new Movement(player)); movement.suppressedUntil = player.tickCount + 40;
    }
    public static float sprintExhaustion(Player player, float amount) {
        return player instanceof ServerPlayer server && eligible(server) && has(server, ProfessionId.EXPLORER, "ration_saver") ? amount * .9F : amount;
    }
    public static void greenhouseGrowth(ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) { CoreFarming.growth(level, pos, random); }
    public static void sheared(ServerPlayer player, net.minecraft.world.entity.animal.Sheep sheep) { CoreFarming.sheared(player, sheep); }
    public static void placedOre(ServerLevel level, BlockPos pos) { CoreDiscoveryLedger.placed(level, pos); }
    private static void reconcile(ServerPlayer player) {
        Attribute hotbar = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("tinyinv", "hotbar_slots"));
        if (hotbar != null) modifier(player, hotbar, HOTBAR, has(player, ProfessionId.ARCHITECT, "architect_hotbar"), 4, AttributeModifier.Operation.ADDITION);
        Movement movement = MOVEMENT.get(player.getUUID());
        modifier(player, Attributes.MOVEMENT_SPEED, TRAVEL, eligible(player) && !player.isPassenger() && movement != null
                && player.tickCount < movement.travelUntil && has(player, ProfessionId.EXPLORER, "fast_traveler"), .08, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, boolean enabled, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute); if (instance == null) return;
        if (enabled && instance.getModifier(id) == null) instance.addTransientModifier(new AttributeModifier(id, "JEM profession", amount, operation));
        else if (!enabled && instance.getModifier(id) != null) instance.removeModifier(id);
    }
    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int count = DEFERRED.size(); while (count-- > 0) DEFERRED.removeFirst().run();
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        MOVEMENT.remove(event.getEntity().getUUID()); CoreFarming.logout(event.getEntity().getUUID()); CoreExploration.logout(event.getEntity().getUUID()); CoreBuilding.logout(event.getEntity().getUUID());
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        MOVEMENT.clear(); VEINS.clear(); DEFERRED.clear(); BREAK.remove(); CoreDiscoveryLedger.clear(); CoreFarming.clear(); CoreExploration.clear(); CoreBuilding.clear();
    }
    public static final class BreakCapture {
        final ServerPlayer player; final BlockPos pos; final BlockState state; final boolean correctTool; final BreakCapture parent; final List<ItemEntity> items = new ArrayList<>();
        BreakCapture(ServerPlayer player, BlockPos pos, BlockState state, boolean correctTool, BreakCapture parent) { this.player = player; this.pos = pos; this.state = state; this.correctTool = correctTool; this.parent = parent; }
    }
    private static final class Movement {
        Vec3 pos; String dimension; int chunkX, chunkZ, suppressedUntil, travelUntil;
        Movement(ServerPlayer player) { pos = player.position(); dimension = dimension(player); chunkX = player.chunkPosition().x; chunkZ = player.chunkPosition().z; suppressedUntil = player.tickCount + 40; }
    }
}
