package com.siirio.jemserver.client.smp;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

record SmpRoute(String tab, String filter, UUID selected, int page, int scroll, String search,
                String seller, String sort, CompoundTag model) {
    boolean sameDestination(SmpRoute other) {
        return tab.equals(other.tab()) && filter.equals(other.filter()) && java.util.Objects.equals(selected, other.selected())
                && page == other.page() && search.equals(other.search()) && seller.equals(other.seller()) && sort.equals(other.sort());
    }
}
