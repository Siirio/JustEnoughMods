package com.justenoughmods.achievementguide.integration.home;

import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChainBlock;

/** The chain endpoint is supplied by the installed native neighbor operation. */
public final class HomeSignalProof {
    private static final ThreadLocal<Deque<HomeInteractionProof.Call>> CALLS = ThreadLocal.withInitial(HomeInteractionProof::calls);

    public static void begin(Level level, BlockPos pos, Player player, InteractionHand hand) {
        HomeInteractionProof.begin(CALLS.get(), level, pos, player, hand);
    }
    public static void chains(Level level, BlockPos pos, int distance) {
        HomeInteractionProof.Call call = CALLS.get().peek();
        if (call != null && call.level == level && call.pos.equals(pos) && distance > 0) call.chainDistance = distance;
    }
    public static void finish(Level level, BlockPos pos, Player player, InteractionResult result, boolean lantern) {
        HomeInteractionProof.Call call = HomeInteractionProof.finish(CALLS.get(), level, pos, player, result);
        if (call == null || !HomeInteractionProof.property(call.before, "pressed").equals("false")) return;
        var after = level.getBlockState(pos);
        if (!HomeInteractionProof.changed(call, after, "pressed") || !HomeInteractionProof.property(after, "pressed").equals("true")) return;
        if (!lantern && after.getSignal(level, pos, Direction.NORTH) > 0)
            HomeInteractionProof.emit(player, "v3_home_secret_signal");
        if (lantern && call.chainDistance > 0) {
            BlockPos endpoint = pos.above(call.chainDistance);
            var chain = level.getBlockState(endpoint);
            if (chain.getBlock() instanceof ChainBlock && chain.getSignal(level, endpoint, Direction.DOWN) > 0)
                HomeInteractionProof.emit(player, "v3_home_secret_signal_chain");
        }
    }
    private HomeSignalProof() {}
}
