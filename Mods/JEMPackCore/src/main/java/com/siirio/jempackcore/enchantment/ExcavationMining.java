package com.siirio.jempackcore.enchantment;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.ForgeEventFactory;

/** One native mining operation; only DiggerItem's normal mining wear is budgeted. */
public final class ExcavationMining {
    private static final ThreadLocal<Batch> ACTIVE = new ThreadLocal<>();

    public static boolean destroyBlock(ServerPlayer player, BlockPos origin) {
        ItemStack tool = player.getMainHandItem();
        if (ACTIVE.get() != null || !ExcavationEvents.isAreaActive(player)) {
            return player.gameMode.destroyBlock(origin);
        }
        var normal = ExcavationEvents.normalAxis(player, origin);
        Batch batch = new Batch(player, tool);
        ACTIVE.set(batch);
        try {
            boolean result = batch.breakOne(origin);
            if (!batch.removed) return result;
            for (BlockPos target : ExcavationPattern.positions(origin, normal)) {
                if (!batch.hasTool()) break;
                if (target.equals(origin) || !player.serverLevel().hasChunkAt(target)) continue;
                var state = player.serverLevel().getBlockState(target);
                if (state.isAir() || state.getDestroySpeed(player.serverLevel(), target) < 0
                        || !tool.isCorrectToolForDrops(state)) continue;
                batch.breakOne(target);
            }
            return result;
        } finally {
            ACTIVE.remove();
        }
    }

    public static void miningWear(ItemStack tool, int amount, LivingEntity entity,
                                  Consumer<LivingEntity> onBreak, BlockPos pos) {
        Batch batch = ACTIVE.get();
        if (batch != null && entity == batch.player && tool == batch.tool
                && pos.equals(batch.target) && amount == 1 && batch.wear == null) {
            batch.wear = onBreak;
        } else {
            tool.hurtAndBreak(amount, entity, onBreak);
        }
    }

    public static void blockRemoved(ServerPlayer player, BlockPos pos, boolean removed) {
        Batch batch = ACTIVE.get();
        if (batch == null || batch.player != player || !pos.equals(batch.target)) return;
        batch.removed = removed;
        if (!removed || batch.wear == null || !batch.hasTool()) return;
        // Distribute a four-point budget over successful native wear requests only.
        // I/II retain vanilla Unbreaking rolls; III instead has one exact 0/1 batch result.
        int total = batch.unbreaking >= 3
                ? (++batch.blocks + batch.offset) / 18
                : (4 * ++batch.blocks + batch.offset) / 9;
        int amount = total - batch.charged;
        batch.charged = total;
        if (amount <= 0) return;
        ItemStack before = batch.tool.copy();
        batch.charging = true;
        try {
            batch.tool.hurtAndBreak(amount, player, batch.wear);
        } finally {
            batch.charging = false;
        }
        if (batch.tool.isEmpty()) {
            ForgeEventFactory.onPlayerDestroyItem(player, before, InteractionHand.MAIN_HAND);
        }
    }

    public static boolean bypassUnbreaking(ItemStack tool) {
        Batch batch = ACTIVE.get();
        return batch != null && batch.charging && tool == batch.tool && batch.unbreaking >= 3;
    }

    private static final class Batch {
        final ServerPlayer player;
        final ItemStack tool;
        final int unbreaking;
        final int offset;
        BlockPos target;
        Consumer<LivingEntity> wear;
        boolean removed;
        boolean charging;
        int blocks;
        int charged;

        Batch(ServerPlayer player, ItemStack tool) {
            this.player = player;
            this.tool = tool;
            unbreaking = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, tool);
            offset = player.getRandom().nextInt(unbreaking >= 3 ? 18 : 9);
        }

        boolean hasTool() {
            return !tool.isEmpty() && player.getMainHandItem() == tool;
        }

        boolean breakOne(BlockPos pos) {
            target = pos;
            wear = null;
            removed = false;
            return player.gameMode.destroyBlock(pos);
        }
    }

    private ExcavationMining() { }
}
