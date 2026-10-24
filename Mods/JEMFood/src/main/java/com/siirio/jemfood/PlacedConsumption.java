package com.siirio.jemfood;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

/** Native block use scope with actual hunger insertion and portion mutation as the receipt. */
public final class PlacedConsumption {
    private static final ThreadLocal<Receipt> CURRENT = new ThreadLocal<>();
    private static final class Receipt {
        final ServerPlayer player;
        boolean ate;
        Receipt(ServerPlayer player) { this.player = player; }
    }
    private PlacedConsumption() {}
    public static boolean active(Player player) { return CURRENT.get() != null && CURRENT.get().player == player; }
    public static void hunger(FoodData food, int nutrition) {
        Receipt receipt = CURRENT.get();
        if (receipt != null && receipt.player.getFoodData() == food && nutrition > 0) receipt.ate = true;
    }
    public static InteractionResult use(BlockState state, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (!(player instanceof ServerPlayer server) || server.isCreative() || server.isSpectator() || FoodCatalog.find(id) == null)
            return state.use(level, player, hand, hit);
        Receipt previous = CURRENT.get();
        Receipt receipt = new Receipt(server);
        CURRENT.set(receipt);
        try {
            InteractionResult result = state.use(level, player, hand, hit);
            BlockPos pos = hit.getBlockPos();
            if (receipt.ate && result.consumesAction() && !level.getBlockState(pos).equals(state))
                FoodState.get(server).consume(server, id);
            return result;
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
