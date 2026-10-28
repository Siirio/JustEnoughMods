package com.justenoughmods.achievementguide.integration.create;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** MC-only subscriber, safe without Create. Observes one actual native drop call, never later pickups. */
@Mod.EventBusSubscriber(modid = "jem_advancements")
public final class CreateHarvestDropCommit {
    private record Drop(ServerLevel level, ItemStack output, List<ItemEntity> candidates) {}
    private static final ThreadLocal<Drop> CURRENT = new ThreadLocal<>();
    private CreateHarvestDropCommit() {}

    public static boolean observe(Level level, ItemStack output, Runnable nativeCall) {
        if (!(level instanceof ServerLevel serverLevel)) { nativeCall.run(); return false; }
        Drop previous = CURRENT.get();
        Drop drop = new Drop(serverLevel, output.copy(), new ArrayList<>());
        CURRENT.set(drop);
        try {
            nativeCall.run();
            // Join events are pre-commit. Resolve the exact entity only after native addFreshEntity returned.
            return drop.candidates().stream().anyMatch(entity -> !entity.isRemoved()
                    && serverLevel.getEntity(entity.getUUID()) == entity && !entity.getItem().isEmpty()
                    && entity.getItem().getCount() <= output.getCount()
                    && ItemStack.isSameItemSameTags(entity.getItem(), output));
        } finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joining(EntityJoinLevelEvent event) {
        Drop drop = CURRENT.get();
        if (drop != null && event.getLevel() == drop.level() && drop.candidates().size() < 4
                && event.getEntity() instanceof ItemEntity item && ItemStack.isSameItemSameTags(item.getItem(), drop.output())) {
            drop.candidates().add(item);
        }
    }
}
