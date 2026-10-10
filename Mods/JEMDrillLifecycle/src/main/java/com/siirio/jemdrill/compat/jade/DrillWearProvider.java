package com.siirio.jemdrill.compat.jade;

import com.siirio.jemdrill.drill.DrillWearAccess;
import com.siirio.jemdrill.drill.DrillWearPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

final class DrillWearProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    private static final ResourceLocation UID = new ResourceLocation("jemcompat", "drill_wear");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof DrillWearAccess wear) {
            tag.putInt(DrillWearPolicy.WEAR_KEY, wear.jemcompat$getWear());
            tag.putInt(DrillWearPolicy.REPAIRS_KEY, wear.jemcompat$getRepairs());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        int wear = data.getInt(DrillWearPolicy.WEAR_KEY);
        int repairs = data.getInt(DrillWearPolicy.REPAIRS_KEY);
        float remaining = 1.0F - (float) wear / DrillWearPolicy.MAX_WEAR;
        IElementHelper elements = tooltip.getElementHelper();
        tooltip.add(elements.progress(remaining,
                Component.translatable("jemcompat.drill.wear", wear, DrillWearPolicy.MAX_WEAR),
                elements.progressStyle().color(0xFFCC3333, 0xFF3A3A3A), BoxStyle.DEFAULT, false));
        tooltip.add(Component.translatable("jemcompat.drill.repairs", repairs, DrillWearPolicy.MAX_REPAIRS));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
