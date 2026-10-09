package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeGameplayEvents {
    private static final Set<String> FURNITURE_NAMES = Set.of(
            "armchair", "bathtub", "bench", "bookcase", "bookshelf", "cabinet", "chair", "couch",
            "counter", "cupboard", "desk", "dresser", "drawer", "nightstand", "ottoman", "seat",
            "shelf", "side_table", "sink", "sofa", "stool", "table", "toilet", "vanity", "wardrobe"
    );
    private static final ResourceLocation DREAMCATCHER = new ResourceLocation("primal", "dreamcatcher");
    private static final ResourceLocation HOLIDAY_GEM = new ResourceLocation("moa_decor_holidays", "gema_festividades");
    private static final ResourceLocation AGED_PHOTOGRAPH = new ResourceLocation("exposure", "aged_photograph");
    private static final Set<ResourceLocation> CNC_ANTLERS = Set.of(
            new ResourceLocation("cnc", "caribou_antler"),
            new ResourceLocation("cnc", "elk_antler"),
            new ResourceLocation("cnc", "white_tailed_deer_antler")
    );
    private static final ResourceLocation DREAMCATCHER_ADVANCEMENT = new ResourceLocation("jem_guide", "mobs/8_4_1");
    private static final ResourceLocation HOLIDAY_GEM_ADVANCEMENT = new ResourceLocation("jem_guide", "building/14");
    private static final ResourceLocation AGED_PHOTOGRAPH_ADVANCEMENT = new ResourceLocation("jem_guide", "cool_stuff/4_4");
    private static final ResourceLocation FURNITURE_ADVANCEMENT = new ResourceLocation("jem_guide", "building/4");

    private ForgeGameplayEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation output = ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem());
        if (HOLIDAY_GEM.equals(output) && event.getCrafting().getCount() == 9) {
            JemCriteria.fire(player, HOLIDAY_GEM_ADVANCEMENT);
            return;
        }
        if (AGED_PHOTOGRAPH.equals(output) && containsIngredient(event, new ResourceLocation("minecraft", "brush"))) {
            JemCriteria.fire(player, AGED_PHOTOGRAPH_ADVANCEMENT);
            return;
        }
        if (DREAMCATCHER.equals(output)) {
            for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
                if (CNC_ANTLERS.contains(ForgeRegistries.ITEMS.getKey(event.getInventory().getItem(slot).getItem()))) {
                    JemCriteria.fire(player, DREAMCATCHER_ADVANCEMENT);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        if (blockId == null || "minecraft".equals(blockId.getNamespace())) {
            return;
        }
        String path = blockId.getPath();
        if (FURNITURE_NAMES.stream().anyMatch(path::contains)) {
            JemCriteria.fire(player, FURNITURE_ADVANCEMENT);
        }
    }

    private static boolean containsIngredient(PlayerEvent.ItemCraftedEvent event, ResourceLocation itemId) {
        for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
            if (itemId.equals(ForgeRegistries.ITEMS.getKey(event.getInventory().getItem(slot).getItem()))) {
                return true;
            }
        }
        return false;
    }
}
