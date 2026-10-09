package com.siirio.jempackcore.enchantment;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ExcavationEvents {
    private static final String DISABLED_LEVEL_TAG = "JEMExcavationDisabledLevel";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || ACTIVE.contains(player.getUUID())) return;
        var tool = player.getMainHandItem();
        if (EnchantmentHelper.getItemEnchantmentLevel(CoreEnchantments.EXCAVATION.get(), tool) == 0) return;
        ACTIVE.add(player.getUUID());
        try {
            for (BlockPos target : ExcavationPattern.positions(event.getPos(), normalAxis(player, event.getPos()))) {
                if (target.equals(event.getPos()) || !level.hasChunkAt(target)) continue;
                var state = level.getBlockState(target);
                if (state.isAir() || state.getDestroySpeed(level, target) < 0 || !tool.isCorrectToolForDrops(state)) continue;
                player.gameMode.destroyBlock(target);
            }
        } finally {
            ACTIVE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void toggle(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || !toggle(event.getItemStack())) return;
        finishToggle(event, player);
    }

    @SubscribeEvent
    public static void toggle(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || !toggle(event.getItemStack())) return;
        finishToggle(event, player);
    }

    private static void finishToggle(PlayerInteractEvent event, ServerPlayer player) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.displayClientMessage(Component.translatable("enchantment.jem_pack_core.excavation")
                .append(": ")
                .append(Component.translatable(isEnabled(player.getMainHandItem()) ? "options.on" : "options.off")), true);
    }

    private static boolean toggle(ItemStack stack) {
        var enchantment = CoreEnchantments.EXCAVATION.get();
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        int activeLevel = enchantments.getOrDefault(enchantment, 0);
        if (activeLevel > 0) {
            stack.getOrCreateTag().putInt(DISABLED_LEVEL_TAG, activeLevel);
            enchantments.remove(enchantment);
            EnchantmentHelper.setEnchantments(enchantments, stack);
            return true;
        }
        var tag = stack.getTag();
        int disabledLevel = tag == null ? 0 : tag.getInt(DISABLED_LEVEL_TAG);
        if (disabledLevel <= 0) return false;
        enchantments.put(enchantment, disabledLevel);
        EnchantmentHelper.setEnchantments(enchantments, stack);
        stack.removeTagKey(DISABLED_LEVEL_TAG);
        return true;
    }

    private static boolean isEnabled(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(CoreEnchantments.EXCAVATION.get(), stack) > 0;
    }

    private static net.minecraft.core.Direction.Axis normalAxis(ServerPlayer player, BlockPos origin) {
        var hit = player.pick(player.getBlockReach(), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(origin)) return blockHit.getDirection().getAxis();
        return player.getDirection().getAxis();
    }

    private ExcavationEvents() {
    }
}
