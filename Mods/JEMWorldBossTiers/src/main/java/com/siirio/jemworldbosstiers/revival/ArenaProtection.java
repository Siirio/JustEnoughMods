package com.siirio.jemworldbosstiers.revival;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.encounter.EncounterData;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

public final class ArenaProtection {
    public static final TagKey<Block> PROTECTED_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(JemWorldBossTiers.MOD_ID, "protected_from_boss_destruction"));

    private ArenaProtection() {
    }

    public static boolean canDestroy(Entity source, ServerLevel level, BlockPos position) {
        if (!(source instanceof LivingEntity boss) || WorldTierApi.profile(boss).isEmpty()) {
            return true;
        }
        EncounterData encounter = WorldTierApi.encounter(boss).orElse(null);
        ArenaRecord arena = encounter == null || encounter.arenaId() == null ? null : WorldTierData.get(level.getServer()).arena(encounter.arenaId()).orElse(null);
        boolean inside = arena != null && arena.contains(level.dimension().location().toString(), position.getX(), position.getY(), position.getZ());
        return ArenaDestructionPolicy.canDestroy(inside, level.getBlockEntity(position) != null, level.getBlockState(position).is(PROTECTED_BLOCKS));
    }
}
