package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class HostedLobby {
    private static final String ROOT = "JEMHostedLobby";
    private static final long TIMEOUT_TICKS = 18000;

    private HostedLobby() {}

    public static boolean hold(LivingEntity boss) {
        if (boss.level().isClientSide || !boss.isAlive() || HostedEncounters.isHosted(boss)
                || WorldTierApi.profile(boss).isEmpty()) return false;
        if (held(boss)) {
            boss.getPersistentData().getCompound(ROOT).putLong("Until", boss.level().getGameTime() + TIMEOUT_TICKS);
            return true;
        }
        CompoundTag state = new CompoundTag();
        state.putLong("Until", boss.level().getGameTime() + TIMEOUT_TICKS);
        boss.getPersistentData().put(ROOT, state);
        return true;
    }

    public static boolean held(LivingEntity boss) { return boss.getPersistentData().contains(ROOT); }

    public static void release(LivingEntity boss) {
        if (boss.level().isClientSide || !held(boss)) return;
        if (boss instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(false);
        boss.getPersistentData().remove(ROOT);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loaded(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity boss) release(boss);
    }
}
