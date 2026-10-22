package com.siirio.jemserver.smp.events.calendar;

import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Owns only the approved New Year consent-based gift escrow. Legacy activity records remain readable. */
public final class HolidayActivities {
    private static final String RETIRED_MOB_TAG = "jem_holiday_owner_";

    public static UUID offerGift(ServerPlayer sender, ServerPlayer target) {
        if (!com.siirio.jemserver.smp.events.EventRuntime.enabled(sender.getServer())) return null;
        var senderPrincipal = IdentityPolicy.resolve(sender);
        var targetPrincipal = IdentityPolicy.resolve(target);
        CalendarEventData data = CalendarEventData.get(sender.getServer());
        if (senderPrincipal.isEmpty() || targetPrincipal.isEmpty() || sender == target
                || data.activeEvent().orElse(null) != CalendarEvent.NEW_YEAR) return null;
        ItemStack held = sender.getMainHandItem();
        if (held.isEmpty() || held.getTag() != null && held.getTag().sizeInBytes() > 16_384L) return null;
        ItemStack escrowed = held.copy();
        sender.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        UUID id = data.escrowGift(senderPrincipal.get().accountId(), sender.getGameProfile().getName(),
                targetPrincipal.get().accountId(), target.getGameProfile().getName(), escrowed);
        sender.getInventory().setChanged(); sender.containerMenu.broadcastChanges();
        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.gift_offer",
                sender.getDisplayName(), escrowed.getHoverName(), escrowed.getCount(), id));
        return id;
    }

    public static boolean acceptGift(ServerPlayer target, UUID id) {
        var principal = IdentityPolicy.resolve(target);
        CalendarEventData data = CalendarEventData.get(target.getServer());
        var gift = data.gift(id);
        if (principal.isEmpty() || gift.isEmpty() || !gift.get().getUUID("Target").equals(principal.get().accountId())) return false;
        ItemStack stack = ItemStack.of(gift.get().getCompound("Item"));
        if (stack.isEmpty() || !canAccept(target, stack)) return false;
        ItemStack remaining = stack.copy();
        if (!target.getInventory().add(remaining) || !remaining.isEmpty()) return false;
        data.removeGift(id);
        target.getInventory().setChanged(); target.containerMenu.broadcastChanges();
        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.holiday.gift_accepted",
                gift.get().getString("SenderName"), stack.getHoverName(), stack.getCount()));
        return true;
    }

    public static boolean cancelGift(ServerPlayer sender, UUID id) {
        var principal = IdentityPolicy.resolve(sender);
        CalendarEventData data = CalendarEventData.get(sender.getServer());
        var gift = data.gift(id);
        if (principal.isEmpty() || gift.isEmpty() || !gift.get().getUUID("Sender").equals(principal.get().accountId())) return false;
        ItemStack stack = ItemStack.of(gift.get().getCompound("Item"));
        if (stack.isEmpty() || !canAccept(sender, stack)) return false;
        ItemStack remaining = stack.copy();
        if (!sender.getInventory().add(remaining) || !remaining.isEmpty()) return false;
        data.removeGift(id);
        sender.getInventory().setChanged(); sender.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean canAccept(ServerPlayer player, ItemStack offered) {
        int remaining = offered.getCount();
        for (ItemStack current : player.getInventory().items) {
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(current, offered)) {
                remaining -= Math.max(0, current.getMaxStackSize() - current.getCount());
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    static void cleanup(net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) for (var entity : level.getAllEntities())
            if (entity.getTags().stream().anyMatch(tag -> tag.startsWith(RETIRED_MOB_TAG))) entity.discard();
    }

    private HolidayActivities() { }
}
