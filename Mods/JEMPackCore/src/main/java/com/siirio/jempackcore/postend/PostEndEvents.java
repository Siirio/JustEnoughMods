package com.siirio.jempackcore.postend;

import com.siirio.jemtwelveeyes.api.CampaignApi;
import net.minecraft.server.MinecraftServer;
import com.siirio.jemtwelveeyes.AdvancementAwards;
import com.siirio.jemtwelveeyes.network.CampaignNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PostEndEvents {
    private static final Set<UUID> PENDING_REWARDS = new LinkedHashSet<>();
    private static final Map<UUID, Vec3> SAFE_POSITIONS = new LinkedHashMap<>();
    private static final ResourceLocation FINAL_ADVANCEMENT = new ResourceLocation("jemcompat", "post_end/the_end");

    private PostEndEvents() { }

    @SubscribeEvent
    public static void defeated(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        List<ServerPlayer> players = event.getSource().getEntity() instanceof ServerPlayer player
                ? List.of(player)
                : level.players();
        if (players.isEmpty()) return;
        MinecraftServer server = level.getServer();
        if (event.getEntity().getType() == EntityType.ENDER_DRAGON) {
            players.forEach(player -> AdvancementAwards.award(player, new ResourceLocation("jemcompat", "post_end/dragon")));
            return;
        }
        ResourceLocation entity = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        for (PostEndTarget target : PostEndTarget.values()) {
            if (!target.entity().equals(entity)) continue;
            if (target == PostEndTarget.OBLITERATOR) {
                if (ready(server) && CampaignApi.recordPostEndDefeat(server, entity)) players.forEach(player -> AdvancementAwards.award(player, FINAL_ADVANCEMENT));
            } else if (CampaignApi.recordPostEndDefeat(server, entity)) {
                players.forEach(player -> AdvancementAwards.award(player, new ResourceLocation("jemcompat", "post_end/" + target.key())));
                if (ready(server)) level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.jemcompat.post_end.open"), false);
            }
        }
    }

    public static boolean startSecretEnding(ServerPlayer player) {
        if (!dragonDefeated(player)) return false;
        MinecraftServer server = player.server;
        if (!CampaignApi.startSecretEnding(server, player.getUUID())) return false;
        PENDING_REWARDS.add(player.getUUID());
        CampaignNetwork.startSecretEnding(player);
        return true;
    }

    private static boolean dragonDefeated(ServerPlayer player) {
        ServerLevel end = player.server.getLevel(net.minecraft.world.level.Level.END);
        return end != null && end.getDragonFight() != null && end.getDragonFight().hasPreviouslyKilledDragon();
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() && event.getEntity() instanceof ServerPlayer player
                && PENDING_REWARDS.remove(player.getUUID())) {
            give(player, new ItemStack(PostEndItems.MYSTERY_LOCATOR.get()));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_REWARDS.remove(event.getEntity().getUUID());
        SAFE_POSITIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PENDING_REWARDS.clear();
        SAFE_POSITIONS.clear();
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.level().dimension() != net.minecraft.world.level.Level.END || player.tickCount % 5 != 0) return;
        MinecraftServer server = player.server;
        TagKey<Structure> station = TagKey.create(Registries.STRUCTURE, new ResourceLocation("jem_twelve_eyes", "post_end/the_obliterator"));
        var start = player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), station);
        if (!start.isValid()) {
            SAFE_POSITIONS.put(player.getUUID(), player.position());
            return;
        }
        if (CampaignApi.revealPostEnd(server)) player.server.getPlayerList().getPlayers().forEach(value -> AdvancementAwards.award(value, new ResourceLocation("jemcompat", "post_end/discovered")));
        if (ready(server)) return;
        Vec3 safe = SAFE_POSITIONS.get(player.getUUID());
        if (safe == null) safe = new Vec3(start.getBoundingBox().minX() - 3.0D, player.getY(), start.getBoundingBox().minZ() - 3.0D);
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(player.serverLevel(), safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
        Component remaining = Component.literal("Ender Guardian, Endersent, Shulker Mimic");
        CampaignNetwork.showGateWarning(player, Component.translatable("message.jemcompat.post_end.not_ready"), remaining, 200);
    }

    @SubscribeEvent
    public static void trader(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof WanderingTrader trader)
                || player.level().dimension() != net.minecraft.world.level.Level.END) return;
        MinecraftServer server = player.server;
        if (!CampaignApi.postEndRevealed(server)) return;
        addTrade(trader, server, PostEndTarget.ENDER_GUARDIAN, PostEndItems.ENDER_GUARDIAN_LOCATOR.get());
        addTrade(trader, server, PostEndTarget.ENDERSENT, PostEndItems.ENDERSENT_LOCATOR.get());
        addTrade(trader, server, PostEndTarget.SHULKER_MIMIC, PostEndItems.SHULKER_MIMIC_LOCATOR.get());
    }

    private static void addTrade(WanderingTrader trader, MinecraftServer server, PostEndTarget target, net.minecraft.world.item.Item item) {
        if (CampaignApi.postEndDefeated(server, target.entity()) || trader.getOffers().stream().anyMatch(offer -> offer.getResult().is(item))) return;
        trader.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, 12), new ItemStack(item), 12, 5, 0.05F));
    }

    private static boolean ready(MinecraftServer server) {
        return CampaignApi.postEndDefeated(server, PostEndTarget.ENDER_GUARDIAN.entity())
                && CampaignApi.postEndDefeated(server, PostEndTarget.ENDERSENT.entity())
                && CampaignApi.postEndDefeated(server, PostEndTarget.SHULKER_MIMIC.entity());
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
