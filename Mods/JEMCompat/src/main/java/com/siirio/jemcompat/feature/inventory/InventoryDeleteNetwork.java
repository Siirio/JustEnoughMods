package com.siirio.jemcompat.feature.inventory;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** A Delete press is a request to remove items from one real inventory slot. */
public final class InventoryDeleteNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JEMCompat.MOD_ID, "inventory_delete"), () -> PROTOCOL,
            PROTOCOL::equals, PROTOCOL::equals);

    private InventoryDeleteNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(DeleteRequest.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteRequest::encode).decoder(DeleteRequest::decode)
                .consumerMainThread((request, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) remove(player, request);
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void request(AbstractContainerMenu menu, int slotIndex, boolean all, ItemStack expected) {
        CHANNEL.sendToServer(new DeleteRequest(menu.containerId, menu.getStateId(), slotIndex, all, expected.copy()));
    }

    private static void remove(ServerPlayer player, DeleteRequest request) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != request.containerId() || menu.getStateId() != request.stateId()
                || !menu.stillValid(player) || !menu.getCarried().isEmpty()
                || request.slotIndex() < 0 || request.slotIndex() >= menu.slots.size()) return;

        Slot slot = menu.getSlot(request.slotIndex());
        ItemStack actual = slot.getItem();
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player) || !slot.allowModification(player)
                || !storageSlot(player, menu, slot, request.slotIndex()) || actual.getCount() != request.expected().getCount()
                || !ItemStack.isSameItemSameTags(actual, request.expected())) return;

        int amount = request.all() ? actual.getCount() : 1;
        slot.safeTake(amount, amount, player);
        menu.broadcastChanges();
    }

    private static boolean storageSlot(ServerPlayer player, AbstractContainerMenu menu, Slot slot, int menuSlot) {
        if (slot.container == player.getInventory()) return true;
        // These vanilla menus contain storage slots only; crafting and trade results do not.
        if (menu.getClass() == ChestMenu.class || menu.getClass() == HopperMenu.class
                || menu.getClass() == DispenserMenu.class || menu.getClass() == ShulkerBoxMenu.class
                || menu.getClass() == HorseInventoryMenu.class) return true;
        String menuClass = menu.getClass().getName();
        if (ModList.get().isLoaded("sophisticatedcore")
                && (menuClass.equals("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer")
                || menuClass.equals("net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu")))
            return SophisticatedStorageDelete.allowed(menu, slot, menuSlot);
        // Third-party virtual, result and unreviewed menus keep their native take behavior.
        return false;
    }

    private record DeleteRequest(int containerId, int stateId, int slotIndex, boolean all, ItemStack expected) {
        private static void encode(DeleteRequest request, FriendlyByteBuf out) {
            out.writeVarInt(request.containerId());
            out.writeVarInt(request.stateId());
            out.writeVarInt(request.slotIndex());
            out.writeBoolean(request.all());
            out.writeItem(request.expected());
        }

        private static DeleteRequest decode(FriendlyByteBuf in) {
            return new DeleteRequest(in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readBoolean(), in.readItem());
        }
    }
}
