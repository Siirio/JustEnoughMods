package com.siirio.jemserver.smp.shops;

import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;
import net.spudacious5705.shops.block.entity.AbstractShopEntity;
import net.spudacious5705.shops.screen.ShopScreenHandlerCustomer;

public final class RemoteShop {
    private static final int MAX_OPEN_BYTES = 24000;

    public static void open(ServerPlayer player, CompoundTag row) {
        var level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(row.getString("dimension"))));
        var pos = BlockPos.of(row.getLong("position"));
        SmpRecords.require(level != null && level.hasChunkAt(pos), "shop_unloaded");
        SmpRecords.require(level.getBlockEntity(pos) instanceof AbstractShopEntity, "unavailable");
        var shop = (AbstractShopEntity) level.getBlockEntity(pos);
        SmpRecords.require(shop.isShopFunctional(), "unavailable");
        var snapshot = shop.saveWithFullMetadata();
        snapshot.remove("Items");
        snapshot.remove("contracts");
        SmpRecords.require(snapshot.sizeInBytes() < MAX_OPEN_BYTES, "item_details_too_large");
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) -> new CustomerMenu(id, inventory, shop), shop.getBlockState().getBlock().getName()), buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeBoolean(false);
            buffer.writeNbt(NbtUtils.writeBlockState(shop.getBlockState()));
            buffer.writeNbt(snapshot);
        });
    }

    private static final class CustomerMenu extends ShopScreenHandlerCustomer {
        private final AbstractShopEntity shop;
        private final java.util.UUID buyer;

        private CustomerMenu(int id, Inventory inventory, AbstractShopEntity shop) {
            super(id, inventory, shop, shop.getInventoryDelegate(inventory.player));
            this.shop = shop;
            this.buyer = inventory.player.getUUID();
        }

        @Override
        public boolean stillValid(Player player) {
            var level = shop.getLevel();
            return !shop.isRemoved() && level != null && level.hasChunkAt(shop.getBlockPos())
                    && level.getBlockEntity(shop.getBlockPos()) == shop
                    && player.getUUID().equals(buyer);
        }
    }

    private RemoteShop() {}
}
