package com.justenoughmods.achievementguide.integration.creaturecare;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Public observation DTOs remain accessible from native classes after Mixin handler relocation. */
public final class CreatureCareMouse {
    private CreatureCareMouse() {}
    public static final class Capture {
        public final ServerPlayer actor;
        public final CompoundTag state;
        public final ItemStack before;
        public final List<UUID> batches = new ArrayList<>();
        public Capture(ServerPlayer actor, Entity mouse, ItemStack source) {
            this.actor = actor; state = CreatureCareLineage.nativeState(mouse); before = source.copy();
        }
    }
    public static final class Release {
        public final ServerPlayer actor;
        public final ItemStack before;
        public Entity released;
        public Release(ServerPlayer actor, ItemStack stack) { this.actor = actor; before = stack.copy(); }
    }
}
