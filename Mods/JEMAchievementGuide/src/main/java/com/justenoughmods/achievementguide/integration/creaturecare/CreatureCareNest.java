package com.justenoughmods.achievementguide.integration.creaturecare;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CreatureCareNest {
    private CreatureCareNest() {}
    public static final class Place {
        public final ServerPlayer actor;
        public final ItemStack before;
        public final int count;
        public final List<CreatureCareLineage.Egg> siblings;
        public Place(ServerPlayer actor, ItemStack before, int count, List<CreatureCareLineage.Egg> siblings) {
            this.actor = actor; this.before = before.copy(); this.count = count; this.siblings = List.copyOf(siblings);
        }
    }
}
