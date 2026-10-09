package com.siirio.jemcompat.gate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CampaignEyeProtection {
    private static final int EXTRA_LIFETIME = 6000;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void protectEntity(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && CampaignEyeService.isCampaignEye(item.getItem())) {
            CampaignEyeService.protect(item);
        }
    }

    @SubscribeEvent
    public void preventExpiry(ItemExpireEvent event) {
        if (CampaignEyeService.isCampaignEye(event.getEntity().getItem())) {
            event.setExtraLife(EXTRA_LIFETIME);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void preserveInventoryEyes(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> protectedItems = new ArrayList<>();
        player.getInventory().items.forEach(stack -> preserve(stack, protectedItems));
        player.getInventory().armor.forEach(stack -> preserve(stack, protectedItems));
        player.getInventory().offhand.forEach(stack -> preserve(stack, protectedItems));
        if (!protectedItems.isEmpty()) {
            CampaignSavedData.get(player.server).protectDeathItems(player.getUUID(), protectedItems);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void preserveDeathItems(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<net.minecraft.world.item.ItemStack> protectedItems = new ArrayList<>();
        event.getDrops().removeIf(drop -> {
            if (!CampaignEyeService.isCampaignEye(drop.getItem())) {
                return false;
            }
            protectedItems.add(drop.getItem().copy());
            return true;
        });
        if (!protectedItems.isEmpty()) {
            CampaignSavedData.get(player.server).protectDeathItems(player.getUUID(), protectedItems);
        }
    }

    @SubscribeEvent
    public void restoreDeathItems(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CampaignSavedData.get(player.server).takeProtectedDeathItems(player.getUUID()).forEach(stack -> {
            if (!player.getInventory().add(stack)) {
                ItemEntity item = player.drop(stack, false);
                if (item != null) {
                    CampaignEyeService.protect(item);
                }
            }
        });
        player.containerMenu.broadcastChanges();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void portalFrame(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getLevel().getBlockState(event.getPos()).is(Blocks.END_PORTAL_FRAME)) {
            return;
        }
        if (event.getItemStack().is(Items.ENDER_EYE)) {
            player.displayClientMessage(Component.translatable("message.jemcompat.ender_eye_locator_only"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        ResourceLocation heldId = RegistryIds.item(event.getItemStack());
        if (heldId != null && "endrem".equals(heldId.getNamespace()) && heldId.getPath().endsWith("_eye")
                && CampaignBoss.byEye(heldId).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.jemcompat.non_campaign_eye"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (heldId != null && CampaignBoss.byEye(heldId).isPresent()) {
            ServerLevel level = player.serverLevel();
            net.minecraft.core.BlockPos position = event.getPos().immutable();
            level.getServer().execute(() -> {
                if (hasPortal(level, position)) {
                    GateAdvancements.awardCampaignEvent(level, player.position(),
                            new ResourceLocation("jemcompat", "campaign/open_portal"), player);
                }
            });
        }
        if (event.getItemStack().isEmpty() && player.isShiftKeyDown()) {
            int recovered = CampaignEyeService.recover(player);
            player.displayClientMessage(Component.translatable("message.jemcompat.eyes_recovered", recovered), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void preserve(ItemStack stack, List<ItemStack> protectedItems) {
        if (!CampaignEyeService.isCampaignEye(stack)) {
            return;
        }
        protectedItems.add(stack.copy());
        stack.setCount(0);
    }

    private static boolean hasPortal(ServerLevel level, net.minecraft.core.BlockPos position) {
        for (net.minecraft.core.BlockPos candidate : net.minecraft.core.BlockPos.betweenClosed(
                position.offset(-5, -2, -5), position.offset(5, 2, 5))) {
            if (level.getBlockState(candidate).is(Blocks.END_PORTAL)) {
                return true;
            }
        }
        return false;
    }
}
