package com.justenoughmods.achievementguide.integration.food;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "jem_advancements")
public final class NativeGameDropProof {
    private NativeGameDropProof() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void actualGame(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player player)) return;
        var actor = RemainingFoodActors.real(player);
        var type = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (actor == null || type == null || !type.getNamespace().equals("cnc")) return;
        for (var drop : event.getDrops()) {
            if (Set.of("cnc:raw_turkey", "cnc:raw_venison").contains(RemainingFoodActors.item(drop.getItem()))) {
                NativeFoodSources.get(level).certify(level, actor.getUUID(), "native_game", drop.getItem(), drop.getItem().getCount());
            }
        }
    }
}
