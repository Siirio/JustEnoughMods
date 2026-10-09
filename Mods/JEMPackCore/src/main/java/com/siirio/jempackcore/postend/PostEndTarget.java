package com.siirio.jempackcore.postend;

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
}
