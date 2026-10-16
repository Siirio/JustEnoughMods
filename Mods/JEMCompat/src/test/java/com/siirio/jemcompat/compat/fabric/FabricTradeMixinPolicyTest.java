package com.siirio.jemcompat.compat.fabric;

import java.util.List;

public final class FabricTradeMixinPolicyTest {
    public static void main(String[] args) {
        check(FabricTradeMixinPolicy.shouldCancel(List.of(FabricTradeMixinPolicy.TARGET), FabricTradeMixinPolicy.MIXIN, FabricTradeMixinPolicy.VERSION), "exact installed mismatch is cancelled");
        check(!FabricTradeMixinPolicy.shouldCancel(List.of(FabricTradeMixinPolicy.TARGET), FabricTradeMixinPolicy.MIXIN, "11.1.4"), "future module version remains untouched");
        check(!FabricTradeMixinPolicy.shouldCancel(List.of(FabricTradeMixinPolicy.TARGET), "another.Mixin", FabricTradeMixinPolicy.VERSION), "other mixins remain untouched");
        check(!FabricTradeMixinPolicy.shouldCancel(List.of("net.minecraft.world.entity.npc.VillagerTrades$Other"), FabricTradeMixinPolicy.MIXIN, FabricTradeMixinPolicy.VERSION), "other provider targets remain untouched");
        check(!FabricTradeMixinPolicy.shouldCancel(List.of(FabricTradeMixinPolicy.TARGET, "another.Target"), FabricTradeMixinPolicy.MIXIN, FabricTradeMixinPolicy.VERSION), "changed multi-target mixin requires review");
        check(!FabricTradeMixinPolicy.shouldCancel(List.of(), FabricTradeMixinPolicy.MIXIN, ""), "missing module cannot activate adapter");
        System.out.println("Fabric trade compatibility policy: 6 assertions passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
