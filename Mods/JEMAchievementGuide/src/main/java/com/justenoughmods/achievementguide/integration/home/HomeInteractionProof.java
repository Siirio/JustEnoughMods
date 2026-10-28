package com.justenoughmods.achievementguide.integration.home;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

/** Call-local observations only. Advancement criteria own persistent multi-step progress. */
public final class HomeInteractionProof {
    public static final class Call {
        public final Level level;
        public final BlockPos pos;
        public final Player player;
        public final InteractionHand hand;
        public final BlockState before;
        public final ItemStack material;
        public BlockState committed;
        public boolean accepted;
        public int chainDistance;

        private Call(Level level, BlockPos pos, Player player, InteractionHand hand) {
            this.level = level;
            this.pos = pos.immutable();
            this.player = player;
            this.hand = hand;
            this.before = level.getBlockState(pos);
            this.material = player == null ? ItemStack.EMPTY : player.getItemInHand(hand).copy();
        }

        public boolean debitedOne() {
            if (material.isEmpty() || player == null) return false;
            ItemStack now = player.getItemInHand(hand);
            return now.isEmpty() ? material.getCount() == 1
                    : ItemStack.isSameItemSameTags(material, now) && material.getCount() == now.getCount() + 1;
        }
    }

    public static ServerPlayer actor(Player player) {
        return player instanceof ServerPlayer server && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(server) ? server : null;
    }

    public static Deque<Call> calls() { return new ArrayDeque<>(); }

    public static void begin(Deque<Call> calls, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (calls.size() >= 16) calls.clear();
        calls.push(new Call(level, pos, player, hand));
    }

    public static Call finish(Deque<Call> calls, Level level, BlockPos pos, Player player, InteractionResult result) {
        Call call = calls.poll();
        if (call == null || call.level != level || !call.pos.equals(pos) || call.player != player
                || actor(player) == null || player.level() != level || !result.consumesAction()) return null;
        return call;
    }

    public static String property(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? "" : value(state, property);
    }

    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public static boolean changed(Call call, BlockState after, String property) {
        return call.before.getBlock() == after.getBlock()
                && !property(call.before, property).isEmpty()
                && !property(call.before, property).equals(property(after, property));
    }

    public static String block(BlockState state) { return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())); }
    public static String item(ItemStack stack) { return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())); }
    public static void emit(Player player, String action) {
        ServerPlayer server = actor(player);
        if (server != null) QuestActionTrigger.INSTANCE.emit(server, action);
    }

    private HomeInteractionProof() {}
}
