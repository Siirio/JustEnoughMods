package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

public final class RemainingFoodActors {
    private RemainingFoodActors() {}
    public static ServerPlayer real(Player player) {
        return player instanceof ServerPlayer actor && com.justenoughmods.achievementguide.integration.travel.TravelProof.eligible(actor) ? actor : null;
    }
    public static String item(ItemStack stack) {
        return stack.isEmpty() ? "" : String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
