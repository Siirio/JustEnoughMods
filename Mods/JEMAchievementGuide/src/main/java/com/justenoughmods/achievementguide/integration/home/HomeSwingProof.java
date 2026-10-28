package com.justenoughmods.achievementguide.integration.home;

import euphy.upo.casualswing.content.entity.EntitySeat;
import euphy.upo.casualswing.content.rope.BaseSwingRopeBlockEntity;
import euphy.upo.casualswing.content.seat.BaseSwingSeatBlockEntity;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class HomeSwingProof {
    private record Ride(EntitySeat seat, ServerPlayer player, BaseSwingRopeBlockEntity master, float amplitude) {}
    private static final ThreadLocal<Deque<Ride>> RIDES = ThreadLocal.withInitial(ArrayDeque::new);

    public static void assembled(BaseSwingRopeBlockEntity master, List<BlockPos> nativeParts) {
        HomeSwingLineage lineage = (HomeSwingLineage)master;
        lineage.jem$homeBuilder(null);
        Level level = master.getLevel();
        ServerPlayer actor = HomeItemUseProof.actor();
        if (actor == null || level != actor.level() || !master.isFinalMaster() || nativeParts.size() > 128) return;
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        for (BlockPos pos : nativeParts) states.put(pos.immutable(), level.getBlockState(pos));
        if (states.isEmpty()) return;
        HomeItemUseProof.defer(actor, () -> !master.isRemoved() && master.isFinalMaster()
                        && level.getBlockEntity(master.getBlockPos()) == master
                        && states.entrySet().stream().allMatch(part -> level.hasChunkAt(part.getKey()) && level.getBlockState(part.getKey()).equals(part.getValue())),
                () -> lineage.jem$homeBuilder(actor.getUUID()));
    }

    public static void before(EntitySeat seat, BlockPos origin) {
        if (RIDES.get().size() >= 16) RIDES.get().clear();
        ServerPlayer player = seat.getFirstPassenger() instanceof ServerPlayer actor ? HomeInteractionProof.actor(actor) : null;
        BaseSwingRopeBlockEntity master = master(seat, origin);
        RIDES.get().push(new Ride(seat, player, master, master == null ? 0 : master.targetAmplitude));
    }
    public static void after(EntitySeat seat, BlockPos origin, boolean isSwing) {
        Ride before = RIDES.get().poll();
        if (before == null || before.seat != seat || before.player == null || !isSwing
                || before.player.getVehicle() != seat || seat.getFirstPassenger() != before.player
                || HomeInteractionProof.actor(before.player) == null || before.master == null
                || master(seat, origin) != before.master || !before.master.isFinalMaster()
                || before.master.targetAmplitude == before.amplitude || before.player.zza == 0
                || !before.player.getUUID().equals(((HomeSwingLineage)before.master).jem$homeBuilder())) return;
        HomeInteractionProof.emit(before.player, "v3_home_swing");
    }
    private static BaseSwingRopeBlockEntity master(EntitySeat seat, BlockPos origin) {
        Level level = seat.level();
        if (origin == null || !level.hasChunkAt(origin) || !(level.getBlockEntity(origin) instanceof BaseSwingSeatBlockEntity nativeSeat)) return null;
        BlockPos pos = nativeSeat.getMasterPos();
        return pos != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof BaseSwingRopeBlockEntity master ? master : null;
    }
    private HomeSwingProof() {}
}
