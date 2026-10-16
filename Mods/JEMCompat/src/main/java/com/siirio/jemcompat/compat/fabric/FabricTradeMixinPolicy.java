package com.siirio.jemcompat.compat.fabric;

import java.util.List;

/** Installed FFAPI build whose local-capture handler predates Forge's Item local. */
public final class FabricTradeMixinPolicy {
    public static final String VERSION = "11.1.3+4bd998fa77";
    public static final String MIXIN = "net.fabricmc.fabric.mixin.object.builder.TradeOffersTypeAwareBuyForOneEmeraldFactoryMixin";
    public static final String TARGET = "net.minecraft.world.entity.npc.VillagerTrades$EmeraldsForVillagerTypeItem";
    private FabricTradeMixinPolicy() { }
    public static boolean shouldCancel(List<String> targets, String mixin, String moduleVersion) {
        return VERSION.equals(moduleVersion) && MIXIN.equals(mixin) && targets.size() == 1 && targets.contains(TARGET);
    }
}
