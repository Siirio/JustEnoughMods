package com.siirio.jemprofessions.sandbox.nativecompat;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import team.creative.playerrevive.PlayerRevive;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.event.PlayerRevivedEvent;
import team.creative.playerrevive.server.PlayerReviveServer;

/** Adds progress only to an existing native rescue; never manufactures a downed state or resurrection. */
public final class SandboxReviveEffects {
    private static final Map<ServerPlayer, Rescue> COMPLETING = new WeakHashMap<>();
    private SandboxReviveEffects() {}
    static void register() { MinecraftForge.EVENT_BUS.register(SandboxReviveEffects.class); }
    static void logout(ServerPlayer player) {
        COMPLETING.remove(player);
        COMPLETING.entrySet().removeIf(entry -> entry.getValue().helpers.contains(player));
    }
    private static boolean liveSandbox(Player player) {
        return player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null
                && !serverPlayer.getServer().isDedicatedServer() && player.isAlive() && !player.isRemoved();
    }

    public static float extraProgress(Player recipient, IBleeding bleeding) {
        if (!liveSandbox(recipient) || !bleeding.isBleeding() || bleeding.bledOut()) return 0;
        float bonus = 0;
        for (Player helper : bleeding.revivingPlayers()) {
            if (!(helper instanceof ServerPlayer player) || !SandboxProfessionSupport.allowed(player) || !player.isAlive()
                    || player.distanceTo(recipient) > PlayerRevive.CONFIG.revive.maxDistance) continue;
            double rank = SandboxProfessionSupport.enabled(player, ProfessionId.WARRIOR, "revival_master") ? .35
                    : SandboxProfessionSupport.enabled(player, ProfessionId.WARRIOR, "field_medic") ? .20 : 0;
            bonus += (float) (rank * PlayerRevive.CONFIG.revive.progressPerPlayer);
        }
        return bonus;
    }

    /** Snapshot before native resetPlayer clears helpers. The public completion event remains the success signal. */
    public static void beginning(Player recipient) {
        if (!(recipient instanceof ServerPlayer player) || !liveSandbox(player)) return;
        IBleeding bleeding = PlayerReviveServer.getBleeding(player);
        if (bleeding == null || !bleeding.isBleeding() || bleeding.bledOut() || !bleeding.revived()) return;
        List<ServerPlayer> helpers = bleeding.revivingPlayers().stream().filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast).filter(SandboxProfessionSupport::allowed).filter(Player::isAlive).limit(16).toList();
        if (!helpers.isEmpty()) COMPLETING.put(player, new Rescue(helpers, player.serverLevel().getGameTime()));
    }

    @SubscribeEvent
    public static void completed(PlayerRevivedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer recipient)) return;
        Rescue rescue = COMPLETING.remove(recipient);
        if (rescue == null || !liveSandbox(recipient) || rescue.tick != recipient.serverLevel().getGameTime()) return;
        for (ServerPlayer helper : rescue.helpers) {
            if (!SandboxProfessionSupport.enabled(helper, ProfessionId.WARRIOR, "battle_aid")) continue;
            long now = recipient.serverLevel().getGameTime();
            // Recipient-owned cooldown prevents several rescuers from stacking the same recovery.
            long until = SandboxNativeEffects.state(recipient).getLong("BattleAidUntil");
            if (until > now) return;
            SandboxNativeEffects.state(recipient).putLong("BattleAidUntil", now + 1200);
            recipient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            SandboxProfessionSupport.message(helper, "Боевая помощь: союзник получает Регенерацию I на 5 секунд.", "Battle Aid: your ally receives Regeneration I for 5 seconds.");
            return;
        }
    }

    private record Rescue(List<ServerPlayer> helpers, long tick) {}
}
