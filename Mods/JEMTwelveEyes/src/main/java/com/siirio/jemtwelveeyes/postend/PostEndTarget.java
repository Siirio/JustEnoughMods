package com.siirio.jemtwelveeyes.postend;

import net.minecraft.resources.ResourceLocation;

public enum PostEndTarget {
    OBLITERATOR("the_obliterator", "legendary_monsters:the_obliterator"),
    ENDER_GUARDIAN("ender_guardian", "cataclysm:ender_guardian"),
    ENDERSENT("endersent", "legendary_monsters:endersent"),
    SHULKER_MIMIC("shulker_mimic", "legendary_monsters:shulker_mimic");

    private final String key;
    private final ResourceLocation entity;

    PostEndTarget(String key, String entity) {
        this.key = key;
        this.entity = new ResourceLocation(entity);
    }

    public String key() { return key; }
    public ResourceLocation entity() { return entity; }

    public boolean registered() {
        return net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(entity);
    }

    /** Optional post-End content has its own native availability, independent of MAIN. */
    public boolean available(net.minecraft.server.MinecraftServer server) {
        if (!registered()) return false;
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return false;
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                new ResourceLocation("jem_twelve_eyes", "post_end/" + key));
        return end.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getTag(tag).filter(values -> values.size() > 0).isPresent();
    }

    public static boolean chainAvailable(net.minecraft.server.MinecraftServer server) {
        return java.util.Arrays.stream(values()).allMatch(target -> target.available(server));
    }
}
