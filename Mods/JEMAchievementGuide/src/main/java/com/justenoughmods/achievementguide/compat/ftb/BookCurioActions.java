package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/** Curios 5.14.1 reports equipment changes after its native inventory has accepted the stack. */
public final class BookCurioActions {
    private BookCurioActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookCurioActions.class); }

    @SubscribeEvent
    public static void changed(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getTo().isEmpty()) return;
        var id = BuiltInRegistries.ITEM.getKey(event.getTo().getItem());
        boolean pendant = id.toString().equals("kaleidoscope_nether:ghast_pendant");
        if (!pendant) return;
        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getStacksHandler(event.getIdentifier()).ifPresent(slots -> {
                    int index = event.getSlotIndex();
                    if (index < 0 || index >= slots.getStacks().getSlots()) return;
                    ItemStack current = slots.getStacks().getStackInSlot(index);
                    SlotContext context = new SlotContext(event.getIdentifier(), player, index, false, slots.isVisible());
                    if (!ItemStack.isSameItemSameTags(current, event.getTo())
                            || !CuriosApi.isStackValid(context, current)) return;
                    QuestActionTrigger.INSTANCE.emit(player, "v3_food_ghast_friend");
                }));
    }
}
