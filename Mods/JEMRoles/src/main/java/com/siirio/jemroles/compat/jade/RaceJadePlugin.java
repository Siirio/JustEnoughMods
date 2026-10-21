package com.siirio.jemroles.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class RaceJadePlugin implements IWailaPlugin {
    private static final Provider PROVIDER = new Provider();
    @Override public void register(IWailaCommonRegistration registration) { registration.registerEntityDataProvider(PROVIDER, net.minecraft.world.entity.player.Player.class); }
    @Override public void registerClient(IWailaClientRegistration registration) { registration.registerEntityComponent(PROVIDER, net.minecraft.world.entity.player.Player.class); }
    private static final class Provider implements IServerDataProvider<EntityAccessor>, IEntityComponentProvider {
        private static final ResourceLocation ID = new ResourceLocation("jem_roles", "race");
        @Override public ResourceLocation getUid() { return ID; }
        @Override public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            if (accessor.getEntity() instanceof ServerPlayer player && player.server.isDedicatedServer())
                com.siirio.jemroles.api.RaceApi.get(player).ifPresent(value -> tag.putString("JemRace", value.race().name()));
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            String race = accessor.getServerData().getString("JemRace");
            try {
                var value = com.siirio.jemroles.race.RaceId.valueOf(race);
                tooltip.add(Component.translatable("jem.roles.jade.race", Component.translatable("jem.roles.race." + value.name().toLowerCase(java.util.Locale.ROOT) + ".name")));
            } catch (IllegalArgumentException absent) { }
        }
    }
}
