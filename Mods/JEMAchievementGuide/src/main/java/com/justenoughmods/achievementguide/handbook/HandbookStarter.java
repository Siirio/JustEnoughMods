package com.justenoughmods.achievementguide.handbook;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Gives every real player the native JEM handbook once, including existing players after this update. */
public final class HandbookStarter {
    private static final ResourceLocation BOOK_ITEM = new ResourceLocation("patchouli", "guide_book");
    private static final String BOOK_ID = "jem_advancements:just_enough_guide";
    private static final String RECEIVED = "JemHandbookReceivedV1";

    private HandbookStarter() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(HandbookStarter.class);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED)) return;

        if (!carriesHandbook(player)) {
            Item item = ForgeRegistries.ITEMS.getValue(BOOK_ITEM);
            if (item == null || item == Items.AIR) return;
            ItemStack handbook = new ItemStack(item);
            handbook.getOrCreateTag().putString("patchouli:book", BOOK_ID);
            player.getInventory().placeItemBackInInventory(handbook);
        }

        persisted.putBoolean(RECEIVED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static boolean carriesHandbook(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (BOOK_ITEM.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())) && stack.hasTag()
                    && BOOK_ID.equals(stack.getTag().getString("patchouli:book"))) return true;
        }
        return false;
    }
}
