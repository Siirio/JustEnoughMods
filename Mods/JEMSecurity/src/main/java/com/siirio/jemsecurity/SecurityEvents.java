package com.siirio.jemsecurity;

import com.google.gson.JsonObject;
import com.siirio.jemsecurity.network.SecurityNetwork;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMSecurity.MOD_ID)
public final class SecurityEvents {
    private static final String LAST_GAME_MODE = "jem_security_last_game_mode";
    private static final Set<Reveal> REVEALS = new HashSet<>();
    private static final Map<UUID, Integer> SUSPICION = new HashMap<>();

    private SecurityEvents() { }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SecurityCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void command(CommandEvent event) {
        Entity entity = event.getParseResults().getContext().getSource().getEntity();
        if (!(entity instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        JsonObject details = new JsonObject();
        details.addProperty("command", sanitizedCommand(event.getParseResults().getReader().getString()));
        details.addProperty("permissionLevel", player.hasPermissions(4) ? 4 : player.hasPermissions(3) ? 3 : player.hasPermissions(2) ? 2 : 0);
        SecurityAuditLog.record(player, "command_used", details);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        player.getPersistentData().putString(LAST_GAME_MODE, player.gameMode.getGameModeForPlayer().getName());
        SecurityAuditLog.record(player, "player_joined", SecurityAuditLog.details("gameMode", player.gameMode.getGameModeForPlayer().getName()));
        SecurityNetwork.expect(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        SecurityAuditLog.record(player, "player_left", SecurityAuditLog.details("gameMode", player.gameMode.getGameModeForPlayer().getName()));
        SecurityNetwork.clear(player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gameModeChanged(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        JsonObject details = new JsonObject();
        details.addProperty("from", event.getCurrentGameMode().getName());
        details.addProperty("to", event.getNewGameMode().getName());
        SecurityAuditLog.record(player, "game_mode_changed", details);
        player.getPersistentData().putString(LAST_GAME_MODE, event.getNewGameMode().getName());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SecurityRuntime.active(event.getServer())) return;
        if (!REVEALS.isEmpty()) {
            Set<Reveal> pending = Set.copyOf(REVEALS);
            REVEALS.clear();
            for (Reveal reveal : pending) {
                ServerLevel level = event.getServer().getLevel(reveal.dimension());
                if (level != null) AntiXray.revealAdjacent(level, reveal.pos());
            }
        }
        SecurityNetwork.tick(event.getServer());
        if (event.getServer().getTickCount() % 20 != 0) return;
        ItemProvenance.scan(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) auditGameMode(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)
                || !SecurityRuntime.active(player.server)) return;
        if (!validBlockAction(player, event.getPos())) {
            event.setCanceled(true);
            suspicious(player, "invalid_block_break", event.getPos(), 4);
            return;
        }
        if (AntiXray.hiddenBlock(event.getState()) && !AntiXray.exposed(level, event.getPos())) {
            suspicious(player, "covered_ore_break", event.getPos(), 8);
        }
        REVEALS.add(new Reveal(level.dimension(), event.getPos().immutable()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        if (!validBlockAction(player, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            suspicious(player, "invalid_block_interaction", event.getPos(), 3);
            return;
        }
        auditTaggedUse(player, event.getItemStack(), "creative_item_block_use", event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        if (!validEntityAction(player, event.getTarget())) {
            event.setCanceled(true);
            suspicious(player, "invalid_entity_attack", event.getTarget().blockPosition(), 4);
        }
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && SecurityRuntime.active(player.server))
            auditTaggedUse(player, event.getItemStack(), "creative_item_use", player.blockPosition());
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        auditTaggedUse(player, player.getMainHandItem(), "creative_item_placed", event.getPos());
    }

    @SubscribeEvent
    public static void tossed(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        ItemStack stack = event.getEntity().getItem();
        if (ItemProvenance.provenance(stack) != null) SecurityAuditLog.record(player, "creative_item_tossed", ItemProvenance.stackDetails(stack));
    }

    @SubscribeEvent
    public static void pickedUp(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        ItemStack stack = event.getItem().getItem();
        if (ItemProvenance.provenance(stack) != null) SecurityAuditLog.record(player, "creative_item_picked_up", ItemProvenance.stackDetails(stack));
    }

    @SubscribeEvent
    public static void containerClosed(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecurityRuntime.active(player.server)) return;
        Inventory inventory = player.getInventory();
        for (var slot : event.getContainer().slots) {
            if (slot.container == inventory || !slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            if (ItemProvenance.provenance(stack) == null) continue;
            JsonObject details = ItemProvenance.stackDetails(stack);
            details.addProperty("containerType", BuiltInRegistries.MENU.getKey(event.getContainer().getType()).toString());
            details.addProperty("containerSlot", slot.getContainerSlot());
            SecurityAuditLog.record(player, "creative_item_stored", details);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        REVEALS.clear();
        SUSPICION.clear();
        ItemProvenance.clear();
        SecurityNetwork.reset();
        SecurityAuditLog.stop();
    }

    public static int suspicion(UUID playerId) {
        return SUSPICION.getOrDefault(playerId, 0);
    }

    private static void auditGameMode(ServerPlayer player) {
        String current = player.gameMode.getGameModeForPlayer().getName();
        String previous = player.getPersistentData().getString(LAST_GAME_MODE);
        if (previous.isEmpty()) {
            player.getPersistentData().putString(LAST_GAME_MODE, current);
            return;
        }
        if (previous.equals(current)) return;
        JsonObject details = new JsonObject();
        details.addProperty("from", previous);
        details.addProperty("to", current);
        SecurityAuditLog.record(player, "game_mode_changed", details);
        player.getPersistentData().putString(LAST_GAME_MODE, current);
    }

    private static boolean validBlockAction(ServerPlayer player, BlockPos pos) {
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 0.75D;
        Vec3 eye = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);
        if (eye.distanceToSqr(target) > reach * reach) return false;
        BlockHitResult hit = player.level().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private static boolean validEntityAction(ServerPlayer player, Entity targetEntity) {
        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get()) + 0.75D;
        Vec3 eye = player.getEyePosition();
        Vec3 target = targetEntity.getBoundingBox().getCenter();
        if (eye.distanceToSqr(target) > reach * reach) return false;
        HitResult hit = player.level().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void suspicious(ServerPlayer player, String type, BlockPos pos, int points) {
        int score = SUSPICION.merge(player.getUUID(), points, Integer::sum);
        JsonObject details = new JsonObject();
        details.addProperty("dimension", player.level().dimension().location().toString());
        details.addProperty("position", pos.toShortString());
        details.addProperty("points", points);
        details.addProperty("score", score);
        SecurityAuditLog.record(player, type, details);
    }

    private static void auditTaggedUse(ServerPlayer player, ItemStack stack, String event, BlockPos pos) {
        if (ItemProvenance.provenance(stack) == null) return;
        JsonObject details = ItemProvenance.stackDetails(stack);
        details.addProperty("dimension", player.level().dimension().location().toString());
        details.addProperty("position", pos.toShortString());
        SecurityAuditLog.record(player, event, details);
    }

    private static String bounded(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private static String sanitizedCommand(String command) {
        String bounded = bounded(command, 2048);
        String first = bounded.stripLeading().split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT);
        if (first.contains("login") || first.contains("register") || first.contains("password")
                || first.contains("passwd") || first.contains("token") || first.contains("secret")
                || first.contains("auth") || Set.of("msg", "tell", "w", "whisper", "me").contains(first))
            return first + " [REDACTED]";
        return bounded;
    }

    private record Reveal(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) { }
}
