package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.ModList;

/** FishingReal works without Tide, and its own active mappings remain authoritative. */
public final class NativeFishingConversionProof {
    private static final ThreadLocal<ItemFishedEvent> CURRENT = new ThreadLocal<>();
    private NativeFishingConversionProof() {}
    public static void begin(ItemFishedEvent event) {
        CURRENT.remove();
        var actor = RemainingFoodActors.real(event.getEntity());
        if (actor != null && !event.isCanceled() && actor.fishing == event.getHookEntity()
                && event.getHookEntity().getPlayerOwner() == actor) CURRENT.set(event);
    }
    public static void end() { CURRENT.remove(); }
    public static void accepted(Entity entity, FishingHook hook, ItemStack source, Player player, boolean accepted) {
        ItemFishedEvent event = CURRENT.get();
        var actor = RemainingFoodActors.real(player);
        if (!accepted || actor == null || event == null || event.getHookEntity() != hook || event.getEntity() != actor
                || source.isEmpty() || entity instanceof ItemEntity || !entity.isAlive()
                || event.getDrops().stream().noneMatch(stack -> stack == source)) return;
        QuestActionTrigger.INSTANCE.emit(actor, "v3_food_living_catch");
        if (ModList.get().isLoaded("tide")) TideCatchProof.converted(hook, actor);
    }
}
