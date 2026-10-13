package com.siirio.jemsettlements.collection;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Bytecode-audited tame/ownership-capable rideables in the installed 2026-10-13 pack. */
public final class MountRoster {
    private static final List<ResourceLocation> ELIGIBLE = List.of(
            id("minecraft:horse"), id("minecraft:donkey"), id("minecraft:mule"), id("minecraft:llama"), id("minecraft:trader_llama"),
            id("minecraft:camel"), id("minecraft:skeleton_horse"), id("barnyardbuddies:clydesdale_horse"), id("barnyardbuddies:trader_clydesdale"),
            id("alexsmobs:elephant"), id("primal:walrus"), id("companions:antlion"));
    private MountRoster() { }
    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    public static List<ResourceLocation> eligible() { return ELIGIBLE; }
    public static boolean contains(ResourceLocation id) { return ELIGIBLE.contains(id); }
    public static int milestone(int percent) { return (int) Math.ceil(ELIGIBLE.size() * percent / 100.0D); }
}
