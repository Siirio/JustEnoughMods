package com.siirio.jemserver.smp.events;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemserver.DeathHistory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HomeTravel {
    private static final String LAST_DAMAGE = "jem_server_home_last_damage";
    private static final String LAST_USE = "jem_server_home_last_use";
    private static final long COMBAT_TICKS = 20L * 15L;
    private static final long COOLDOWN_TICKS = 20L * 60L;

    private HomeTravel() { }

    private static boolean rejected(ServerPlayer player, String key, Object... arguments) { fail(player,key,arguments); return false; }
    static boolean ready(ServerPlayer player) {
        if (IdentityPolicy.resolve(player).isEmpty()) return rejected(player, "jem.home.identity_required");
        if (!player.isAlive() || player.isSpectator()) return rejected(player, "jem.home.unavailable");
        long now = player.server.overworld().getGameTime();
        long damaged = player.getPersistentData().getLong(LAST_DAMAGE);
        if (damaged > 0 && now - damaged < COMBAT_TICKS) return rejected(player, "jem.home.combat", com.siirio.jemidentity.api.MinecraftDays.format(COMBAT_TICKS - now + damaged));
        long used = player.getPersistentData().getLong(LAST_USE);
        if (used > 0 && now - used < COOLDOWN_TICKS) return rejected(player, "jem.home.cooldown", com.siirio.jemidentity.api.MinecraftDays.format(COOLDOWN_TICKS - now + used));
        var event = EventSession.forPlayer(player);
        if (event != null && event.getBoolean("combatStarted")) return rejected(player, "jem.home.arena");
        return true;
    }
    static void used(ServerPlayer player) { player.getPersistentData().putLong(LAST_USE, player.server.overworld().getGameTime()); }
    public static int execute(ServerPlayer player) { return execute(player, false); }
    public static int execute(ServerPlayer player, boolean all) {
        if (!ready(player)) return 0;
        BlockPos respawn = player.getRespawnPosition();
        ServerLevel level = player.server.getLevel(player.getRespawnDimension());
        if (respawn == null || level == null) return fail(player, "jem.home.missing");
        level.getChunkAt(respawn);
        var state = level.getBlockState(respawn);
        boolean valid = state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof RespawnAnchorBlock && state.getValue(RespawnAnchorBlock.CHARGE) > 0
                || player.isRespawnForced();
        if (!valid) return fail(player, "jem.home.invalid");
        if (all) return HomeTravelGroup.execute(player, level, respawn);
        Vec3 target = DeathHistory.nearestSafe(player, level, respawn, 3);
        if (target == null) return fail(player, "jem.home.unsafe");
        BossTeleportSafety.Decision decision = BossTeleportSafety.validate(player, level, target);
        if (decision.matched() && (decision.destination() == null || !target.equals(decision.destination()))) return fail(player, "jem.home.arena");
        player.closeContainer();
        player.stopRiding();
        BossTeleportSafety.apply(player, level, target, player.getRespawnAngle(), player.getXRot());
        used(player);
        player.sendSystemMessage(Component.translatable("jem.home.arrived"), true);
        return 1;
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player)
            player.getPersistentData().putLong(LAST_DAMAGE, player.server.overworld().getGameTime());
    }

    private static int fail(ServerPlayer player, String key, Object... arguments) {
        player.sendSystemMessage(Component.translatable(key, arguments), true);
        return 0;
    }
}
