package com.siirio.jemtwelveeyes.postend;

import com.siirio.jemtwelveeyes.api.CampaignApi;
import com.siirio.jemtwelveeyes.api.BossLocation;
import com.siirio.jemtwelveeyes.BossLocationDiscoveredEvent;
import net.minecraft.server.MinecraftServer;
import com.siirio.jemtwelveeyes.AdvancementAwards;
import com.siirio.jemtwelveeyes.network.CampaignNetwork;
import com.siirio.jemrewards.PendingRewardContainer;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class PostEndEvents {
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
            if (PostEndTarget.chainAvailable(server)) players.forEach(player ->
                    AdvancementAwards.award(player, new ResourceLocation("jemcompat", "post_end/dragon")));
            return;
        }
        ResourceLocation entity = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        for (PostEndTarget target : PostEndTarget.values()) {
            if (!target.entity().equals(entity) || !target.available(server)) continue;
            if (target == PostEndTarget.OBLITERATOR) {
                if (ready(server) && CampaignApi.recordPostEndDefeat(server, entity)) players.forEach(player -> AdvancementAwards.award(player, FINAL_ADVANCEMENT));
            } else if (CampaignApi.recordPostEndDefeat(server, entity)) {
                players.forEach(player -> AdvancementAwards.award(player, new ResourceLocation("jemcompat", "post_end/" + target.key())));
                if (ready(server)) level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.jemcompat.post_end.open"), false);
            }
        }
    }

    public static boolean startSecretEnding(ServerPlayer player) {
        if (!dragonDefeated(player) || !PostEndTarget.chainAvailable(player.server)) return false;
        MinecraftServer server = player.server;
        if (!CampaignApi.startSecretEnding(server, player.getUUID())) return false;
        CampaignNetwork.startSecretEnding(player);
        return true;
    }

    public static void holdAtEndPortal(ServerPlayer player) {
        player.setPortalCooldown();
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int radius = 0; radius <= 5; radius++) {
            for (int y = 0; y <= 4; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                        BlockPos feet = origin.offset(x, y, z);
                        BlockState floor = level.getBlockState(feet.below());
                        if (!floor.isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                                || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                        player.teleportTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                        return;
                    }
                }
            }
        }
        player.teleportTo(player.getX(), player.getY() + 2.0D, player.getZ());
    }

    public static void finishSecretEnding(ServerPlayer player) {
        if (!CampaignApi.secretEndingPending(player.server, player.getUUID())
                || player.level().dimension() != net.minecraft.world.level.Level.END
                || !PostEndTarget.chainAvailable(player.server)) return;
        if (player.getInventory().contains(new ItemStack(PostEndItems.MYSTERY_LOCATOR.get()))) {
            CampaignApi.completeSecretEnding(player.server, player.getUUID());
            return;
        }
        ItemStack clue = new ItemStack(PostEndItems.MYSTERY_LOCATOR.get());
        int selected = player.getInventory().selected;
        ItemStack displaced = player.getInventory().getItem(selected).copy();
        boolean storeInInventory = displaced.isEmpty() || canStoreOutsideSelected(player.getInventory(), displaced, selected);
        if (!storeInInventory) {
            String source = "jem_twelve_eyes:post_end_displaced";
            if (!PendingRewardContainer.hasIssued(player.server, player.getUUID(), source)) {
                PendingRewardContainer.enqueue(player.server, player.getUUID(), source,
                        "jem_twelve_eyes.reward.post_end_displaced", List.of(displaced.copy()));
            }
            if (!PendingRewardContainer.hasIssued(player.server, player.getUUID(), source)) return;
        }
        player.getInventory().setItem(selected, clue);
        if (storeInInventory && !displaced.isEmpty()) storeOutsideSelected(player.getInventory(), displaced, selected);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        CampaignApi.completeSecretEnding(player.server, player.getUUID());
    }

    private static boolean canStoreOutsideSelected(Inventory inventory, ItemStack stack, int selected) {
        int remaining = stack.getCount();
        for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++) {
            if (slot == selected) continue;
            ItemStack stored = inventory.items.get(slot);
            if (stored.isEmpty()) remaining -= stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(stored, stack)) remaining -= Math.max(0, stored.getMaxStackSize() - stored.getCount());
        }
        return remaining <= 0;
    }

    private static void storeOutsideSelected(Inventory inventory, ItemStack stack, int selected) {
        for (int slot = 0; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
            if (slot == selected) continue;
            ItemStack stored = inventory.items.get(slot);
            if (stored.isEmpty() || !ItemStack.isSameItemSameTags(stored, stack)) continue;
            int moved = Math.min(stack.getCount(), stored.getMaxStackSize() - stored.getCount());
            if (moved > 0) {
                stored.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int slot = 0; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
            if (slot == selected || !inventory.items.get(slot).isEmpty()) continue;
            int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack placed = stack.copy();
            placed.setCount(moved);
            inventory.items.set(slot, placed);
            stack.shrink(moved);
        }
    }

    private static boolean dragonDefeated(ServerPlayer player) {
        ServerLevel end = player.server.getLevel(net.minecraft.world.level.Level.END);
        return end != null && end.getDragonFight() != null && end.getDragonFight().hasPreviouslyKilledDragon();
    }

    @SubscribeEvent
    public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        var server = event.getServer();
        for (PostEndTarget target : PostEndTarget.values()) {
            if (!target.available(server)) continue;
            String key = "post_end/" + target.key();
            var position = CampaignApi.locatedStructure(server, key);
            if (position != null) MinecraftForge.EVENT_BUS.post(new BossLocationDiscoveredEvent(server,
                    new BossLocation(key, target.entity(), net.minecraft.world.level.Level.END.location(), position)));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && CampaignApi.secretEndingPending(player.server, player.getUUID())) finishSecretEnding(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.level().dimension() != net.minecraft.world.level.Level.END || player.tickCount % 5 != 0) return;
        MinecraftServer server = player.server;
        if (!PostEndTarget.OBLITERATOR.available(server)) return;
        TagKey<Structure> station = TagKey.create(Registries.STRUCTURE, new ResourceLocation("jem_twelve_eyes", "post_end/the_obliterator"));
        var start = player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), station);
        if (start.isValid() && CampaignApi.revealPostEnd(server)) {
            player.server.getPlayerList().getPlayers().forEach(value -> AdvancementAwards.award(value,
                    new ResourceLocation("jemcompat", "post_end/discovered")));
        }
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
        if (!target.available(server)) {
            trader.getOffers().removeIf(offer -> offer.getResult().is(item));
            return;
        }
        if (CampaignApi.postEndDefeated(server, target.entity())
                || trader.getOffers().stream().anyMatch(offer -> offer.getResult().is(item))) return;
        trader.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, 12), new ItemStack(item), 12, 5, 0.05F));
    }

    public static boolean ready(MinecraftServer server) {
        return PostEndTarget.chainAvailable(server)
                && CampaignApi.postEndDefeated(server, PostEndTarget.ENDER_GUARDIAN.entity())
                && CampaignApi.postEndDefeated(server, PostEndTarget.ENDERSENT.entity())
                && CampaignApi.postEndDefeated(server, PostEndTarget.SHULKER_MIMIC.entity());
    }

}
