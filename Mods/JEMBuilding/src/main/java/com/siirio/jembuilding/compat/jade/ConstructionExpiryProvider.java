package com.siirio.jembuilding.compat.jade;

import com.siirio.jembuilding.client.ConstructionExpiryClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

final class ConstructionExpiryProvider implements IBlockComponentProvider {
    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final long TICKS_PER_DAY = 24_000L;
    private static final ResourceLocation UID = new ResourceLocation("jem_building", "temporary_expiry");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        long remaining = ConstructionExpiryClient.remaining(accessor.getPosition());
        if (remaining < 0L) return;
        if (remaining >= TICKS_PER_DAY) {
            long days = remaining / TICKS_PER_DAY;
            String key = remaining % TICKS_PER_DAY == 0L
                    ? "jade.jem_building.expires.days" : "jade.jem_building.expires.days_plus";
            tooltip.add(Component.translatable(key, days));
        } else if (remaining >= TICKS_PER_MINUTE) {
            long minutes = (remaining + TICKS_PER_MINUTE - 1L) / TICKS_PER_MINUTE;
            tooltip.add(Component.translatable("jade.jem_building.expires.minutes", minutes));
        } else if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 19L) / 20L);
            tooltip.add(Component.translatable("jade.jem_building.expires.seconds", seconds));
        } else {
            tooltip.add(Component.translatable("jade.jem_building.expires.now"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
