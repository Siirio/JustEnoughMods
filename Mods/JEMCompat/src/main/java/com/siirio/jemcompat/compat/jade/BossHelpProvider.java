package com.siirio.jemcompat.compat.jade;

import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Small owner-authored encounter snapshot for the entity actually being observed. */
final class BossHelpProvider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
    private static final ResourceLocation UID = new ResourceLocation("jemcompat", "boss_help");
    private static final String STATE = "JemBossHelp";

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        if (!ModList.get().isLoaded("jem_world_boss_tiers") || !(accessor.getEntity() instanceof LivingEntity boss)
                || WorldTierApi.profile(boss).isEmpty()) return;
        CompoundTag state = new CompoundTag();
        state.putBoolean("waiting", HostedEncounterApi.held(boss));
        if (!state.getBoolean("waiting") && !HostedEncounterApi.status(boss).isEmpty()) {
            state.putBoolean("spectator", !HostedEncounterApi.eligibleParticipants(boss).contains(accessor.getPlayer().getUUID()));
        }
        if (!state.getBoolean("waiting") && !state.getBoolean("spectator") && ModList.get().isLoaded("cataclysm")) {
            CataclysmBossHelp.describe(boss, state);
        }
        tag.put(STATE, state);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag root = accessor.getServerData();
        if (!root.contains(STATE)) return;
        CompoundTag state = root.getCompound(STATE);
        if (state.getBoolean("waiting")) {
            tooltip.add(Component.translatable("jade.jemcompat.boss_waiting"));
            tooltip.add(Component.translatable("jade.jemcompat.boss_start"));
        } else if (state.getBoolean("spectator")) {
            tooltip.add(Component.translatable("jade.jemcompat.boss_spectator"));
        } else if (state.contains("defense")) {
            tooltip.add(Component.translatable("jade.jemcompat.boss_defense." + state.getString("defense")));
        }
        if (ModList.get().isLoaded("jem_advancements") && ModList.get().isLoaded("patchouli")) {
            BossHelpClient.addRoute(tooltip, accessor);
        }
    }

    @Override
    public ResourceLocation getUid() { return UID; }
}
