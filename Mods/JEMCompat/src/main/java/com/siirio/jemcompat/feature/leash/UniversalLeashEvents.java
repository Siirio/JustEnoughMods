package com.siirio.jemcompat.feature.leash;

import com.siirio.jemcompat.advancement.AdvancementAwards;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class UniversalLeashEvents {
    private static final double KNOT_SEARCH_RADIUS = 7.0;
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("jem_guide", "qol/universal_leash");

    private UniversalLeashEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Mob mob)
                || !event.getItemStack().is(Items.LEAD)
                || mob.isLeashed()
                || mob.canBeLeashed(player)) {
            return;
        }
        mob.setLeashedTo(player, true);
        event.getItemStack().shrink(1);
        AdvancementAwards.award(player, ADVANCEMENT);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(BlockTags.WALLS)) {
            return;
        }
        BlockPos position = event.getPos();
        AABB area = new AABB(position).inflate(KNOT_SEARCH_RADIUS);
        LeashFenceKnotEntity knot = null;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, candidate -> candidate.getLeashHolder() == player)) {
            if (knot == null) {
                knot = LeashFenceKnotEntity.getOrCreateKnot(level, position);
                knot.playPlacementSound();
            }
            mob.setLeashedTo(knot, true);
        }
        if (knot != null) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }
}
