package com.siirio.jemcompat.discovery;

import net.minecraft.core.registries.BuiltInRegistries;
import com.siirio.jemtwelveeyes.network.CampaignNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CampaignLocatorItem extends Item {
    private static final Set<UUID> ACTIVE_SEARCHES = ConcurrentHashMap.newKeySet();
    private final CampaignTarget target;

    public CampaignLocatorItem(CampaignTarget target) {
        super(new Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
        this.target = target;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack locator = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(locator, true);
        }
        if (target.dimension().equals(Level.NETHER.location()) && serverPlayer.level().dimension() != Level.NETHER) {
            serverPlayer.displayClientMessage(Component.translatable("message.jemcompat.map_requires_nether").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(locator);
        }
        UUID playerId = serverPlayer.getUUID();
        if (!ACTIVE_SEARCHES.add(playerId)) {
            serverPlayer.displayClientMessage(Component.translatable("message.jemcompat.map_search_active").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.success(locator);
        }
        serverPlayer.displayClientMessage(Component.translatable("message.jemcompat.map_searching").withStyle(ChatFormatting.AQUA), true);
        CampaignMapService.locateAsync(serverPlayer.server, target)
                .whenCompleteAsync((target, error) -> finishSearch(serverPlayer.server, playerId, target, error), serverPlayer.server);
        return InteractionResultHolder.success(locator);
    }

    private void finishSearch(MinecraftServer server, UUID playerId,
                              Optional<CampaignMapService.Target> result, Throwable error) {
        ACTIVE_SEARCHES.remove(playerId);
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        if (error != null) {
            player.displayClientMessage(Component.translatable("message.jemcompat.map_search_failed").withStyle(ChatFormatting.RED), true);
            return;
        }
        CampaignMapService.Target target = result.orElse(null);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.jemcompat.map_not_found").withStyle(ChatFormatting.RED), true);
            return;
        }
        CampaignNetwork.openBossMap(player, target.position(), target.dimension().location(), this.target.mapNameKey());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(target.mapNameKey());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jemcompat.campaign_map.lore", Component.translatable(
                BuiltInRegistries.ENTITY_TYPE.get(target.entity()).getDescriptionId()
        )).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.jemcompat.boss_locator.use").withStyle(ChatFormatting.AQUA));
    }
}
