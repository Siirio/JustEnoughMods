package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.mixin.ExperienceOrbInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One logical-server baseline, before Mending and downstream XP modifiers. */
@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class ExperienceRewardApi {
    private static final ThreadLocal<Integer> TRANSFER = ThreadLocal.withInitial(() -> 0);

    public static int boosted(int amount) {
        return amount > 0 ? (int) Math.min(Integer.MAX_VALUE, (long) amount * 2) : amount;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void gained(PlayerXpEvent.XpChange event) {
        if (event.getEntity() instanceof ServerPlayer && TRANSFER.get() == 0)
            event.setAmount(boosted(event.getAmount()));
    }

    public static int repair(Player player, int amount) {
        int experience = boosted(amount);
        var orb = new ExperienceOrb(player.level(), player.getX(), player.getY(), player.getZ(), experience);
        return ((ExperienceOrbInvoker) orb).jem$repairPlayerItems(player, experience);
    }

    /** The baseline has already been applied to the XP entering Mending. */
    public static void transfer(Player player, int remaining) {
        if (remaining <= 0) return;
        int depth = TRANSFER.get();
        TRANSFER.set(depth + 1);
        try { player.giveExperiencePoints(remaining); }
        finally { if (depth == 0) TRANSFER.remove(); else TRANSFER.set(depth); }
    }

    public static void give(ServerPlayer player, int experience) {
        if (experience > 0) transfer(player, repair(player, experience));
    }

    private ExperienceRewardApi() {}
}
