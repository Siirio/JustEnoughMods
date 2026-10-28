package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_advancements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WildlifeQuestNativeEvents {
    private WildlifeQuestNativeEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void nativeShield(ShieldBlockEvent event) { WildlifeQuestHurt.shield(event); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void nativeDrops(LivingDropsEvent event) {
        if (!event.isCanceled() && WildlifeQuestProof.type(event.getEntity()).equals("alexsmobs:void_worm")) { var participants = WildlifeQuestWorm.participants(event.getEntity()); for (var drop : event.getDrops()) WildlifeQuestWorm.dropped(drop, participants); }
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level && event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            var actor = WildlifeQuestProof.real(player);
            if (actor != null && WildlifeQuestProof.type(event.getEntity()).equals("alexsmobs:tarantula_hawk") && WildlifeQuestProof.data(actor).hasUUID("native_hawk_offspring")) {
                for (var drop : event.getDrops()) if (WildlifeQuestProof.item(drop.getItem()).equals("alexsmobs:tarantula_hawk_wing_fragment")) NativeFoodSources.get(level).certify(level, actor.getUUID(), "wildlife_hawk_fragment", drop.getItem(), drop.getItem().getCount());
            }
        }

        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level && WildlifeQuestProof.type(event.getEntity()).equals("alexsmobs:blobfish")) {
            var actor = WildlifeQuestProof.actor(event.getEntity(), "captured_blobfish_owner");
            if (actor != null) for (var drop : event.getDrops()) if (WildlifeQuestProof.item(drop.getItem()).equals("alexsmobs:blobfish")) NativeFoodSources.get(level).certify(level, actor.getUUID(), "wildlife_captured_blobfish", drop.getItem(), drop.getItem().getCount());
        }
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level
                && WildlifeQuestProof.type(event.getEntity()).equals("alexsmobs:flying_fish")) {
            for (var drop : event.getDrops()) if (WildlifeQuestProof.item(drop.getItem()).equals("alexsmobs:flying_fish")) {
                // Only actual later inventory receipt can consume this native output budget.
                NativeFoodSources.get(level).certify(level, WildlifeQuestSources.NATURAL, "wildlife_flying_fish", drop.getItem(), drop.getItem().getCount());
            }
        }
    }
}
