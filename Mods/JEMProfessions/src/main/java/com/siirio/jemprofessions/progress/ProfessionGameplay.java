package com.siirio.jemprofessions.progress;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.api.ProfessionProgressRequest;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ProfessionGameplay {
    private ProfessionGameplay() {
    }

    @SubscribeEvent
    public static void blockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (event.getState().getBlock() instanceof CropBlock crop && crop.isMaxAge(event.getState())) {
            credit(player, ProfessionId.FARMER, SourceKind.CROP_HARVEST, block, 2);
        } else if (event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE) && player.hasCorrectToolForDrops(event.getState())) {
            credit(player, ProfessionId.MINER, SourceKind.BLOCK_BREAK, block, 1);
        }
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getDrops().isEmpty()) {
            credit(player, ProfessionId.FISHER, SourceKind.FISH_CAUGHT, new ResourceLocation("minecraft", "fishing"), 3);
        }
    }

    @SubscribeEvent
    public static void defeated(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() != player) {
            ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            credit(player, ProfessionId.WARRIOR, SourceKind.MOB_DEFEATED, type, Math.max(1, (int) Math.ceil(event.getEntity().getMaxHealth() / 10.0F)));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            credit(player, ProfessionId.EXPLORER, SourceKind.DIMENSION_DISCOVERED, event.getTo().location(), 10);
        }
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        if (item.getNamespace().equals("create")) credit(player, ProfessionId.ENGINEER, SourceKind.ITEM_CRAFTED, item, 2);
    }

    private static void credit(ServerPlayer player, ProfessionId profession, SourceKind kind, ResourceLocation source, int amount) {
        ProfessionApi.credit(player, new ProfessionProgressRequest(profession, kind, source, amount, UUID.randomUUID()));
    }
}
