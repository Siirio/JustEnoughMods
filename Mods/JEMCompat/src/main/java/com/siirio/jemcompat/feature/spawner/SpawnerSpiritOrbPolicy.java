package com.siirio.jemcompat.feature.spawner;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Preserves Paraglider's exploration reward without making movable spawners a renewable source. */
public final class SpawnerSpiritOrbPolicy {
    private static final String PLAYER_PLACED = "JemPlayerPlacedSpawner";

    private SpawnerSpiritOrbPolicy() {
    }

    @SubscribeEvent
    public static void markPlayerPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide()
                || !event.getPlacedBlock().is(Blocks.SPAWNER)) {
            return;
        }
        var blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity != null) {
            blockEntity.getPersistentData().putBoolean(PLAYER_PLACED, true);
            blockEntity.setChanged();
        }
    }

    public static boolean isPlayerPlaced(LootContext context) {
        var blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        return blockEntity != null && blockEntity.getPersistentData().getBoolean(PLAYER_PLACED);
    }
}
