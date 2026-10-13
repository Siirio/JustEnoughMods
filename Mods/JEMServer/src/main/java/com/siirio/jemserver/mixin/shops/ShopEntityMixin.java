package com.siirio.jemserver.mixin.shops;

import com.siirio.jemserver.smp.shops.ShopIndex;
import com.siirio.jemserver.smp.shops.ShopSnapshot;
import com.siirio.jemserver.smp.shops.ShopStock;
import com.siirio.jemserver.smp.shops.ShopCoinAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.spudacious5705.shops.block.entity.ShopInventory;
import net.spudacious5705.shops.permission.PermissionLevel;
import net.spudacious5705.shops.screen.ToggleButtonID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.spudacious5705.shops.block.entity.AbstractShopEntity", remap = false)
public abstract class ShopEntityMixin implements ShopSnapshot, ShopCoinAccess {
    @Shadow protected ShopInventory shopInventory;
    @Shadow protected java.util.EnumMap<ToggleButtonID, Boolean> toggleSettings;

    @Inject(method = {"setChanged", "m_6596_"}, at = @At("TAIL"), require = 0, remap = false)
    private void jemShopChanged(CallbackInfo callback) {
        ShopIndex.changed((BlockEntity) (Object) this);
    }

    @Override
    public CompoundTag jemShopSnapshot() {
        var block = (BlockEntity) (Object) this;
        var result = new CompoundTag();
        var offer = shopInventory.getVendingStack();
        var payment = shopInventory.getPaymentStack();
        result.put("offer", offer.save(new CompoundTag()));
        result.put("payment", payment.save(new CompoundTag()));
        result.putString("title", offer.getHoverName().getString());
        var stock = (ShopStock) shopInventory;
        boolean creative = toggleSettings.getOrDefault(ToggleButtonID.CreativeToggle, false);
        result.putString("state", !shopInventory.tradeFunctional() ? "UNCONFIGURED" : !creative && stock.jemOutOfStock() ? "OUT_OF_STOCK" : !creative && stock.jemPaymentFull() ? "PAYMENT_FULL" : "ACTIVE");
        boolean selectable = toggleSettings.getOrDefault(ToggleButtonID.SelectableTradeToggle, false);
        result.putBoolean("selectable", selectable);
        var offers = new ListTag();
        if (selectable) {
            var choices = new ListTag();
            stock.jemChoices().forEach(item -> {
                item.setCount(shopInventory.getVendingQuantity());
                choices.add(item.save(new CompoundTag()));
                offers.add(offer(payment, item, creative ? -1 : stock.jemAvailableTrades(item, item.getCount())));
            });
            result.put("choices", choices);
        } else if (!offer.isEmpty() && !payment.isEmpty()) {
            var output = offer.copyWithCount(shopInventory.getVendingQuantity());
            offers.add(offer(payment, output, creative ? -1 : stock.jemAvailableTrades(output, output.getCount())));
        }
        result.put("offers", offers);
        result.putInt("quantity", shopInventory.getVendingQuantity());
        result.putInt("price", shopInventory.getPrice());
        var nativeData = block.saveWithoutMetadata();
        for (Tag value : nativeData.getList("contracts", Tag.TAG_COMPOUND)) {
            var contract = (CompoundTag) value;
            if (contract.getInt("contract_lvl") == PermissionLevel.OWNER.asInt() && contract.hasUUID("contract_uuid")) {
                result.putUUID("owner", contract.getUUID("contract_uuid"));
                result.putString("name", contract.getString("contract_name"));
                break;
            }
        }
        return result;
    }

    private CompoundTag offer(ItemStack payment, ItemStack output, int availableTrades) {
        var result = new CompoundTag();
        result.put("payment", payment.copyWithCount(shopInventory.getPrice()).save(new CompoundTag()));
        result.put("output", output.save(new CompoundTag()));
        result.putInt("availableTrades", availableTrades);
        return result;
    }

    @Override
    public boolean jemCreativeStock() {
        return toggleSettings.getOrDefault(ToggleButtonID.CreativeToggle, false);
    }

    @Override
    public ItemStack jemTakeCoinSale(ItemStack expected, int quantity) {
        if (expected.isEmpty() || quantity <= 0) return ItemStack.EMPTY;
        if (jemCreativeStock()) return expected.copyWithCount(quantity);
        int remaining = quantity;
        for (int slot = 0; slot <= 53 && remaining > 0; slot++) {
            ItemStack current = shopInventory.get(slot);
            if (!ItemStack.isSameItemSameTags(current, expected)) continue;
            remaining -= Math.min(remaining, current.getCount());
        }
        if (remaining > 0) return ItemStack.EMPTY;
        remaining = quantity;
        for (int slot = 0; slot <= 53 && remaining > 0; slot++) {
            ItemStack current = shopInventory.get(slot);
            if (!ItemStack.isSameItemSameTags(current, expected)) continue;
            int take = Math.min(remaining, current.getCount());
            current.shrink(take);
            remaining -= take;
        }
        ((BlockEntity) (Object) this).setChanged();
        return expected.copyWithCount(quantity);
    }

    @Override
    public boolean jemRestoreCoinSale(ItemStack returned) {
        if (returned.isEmpty() || jemCreativeStock()) return true;
        int remaining = returned.getCount();
        for (int slot = 0; slot <= 53 && remaining > 0; slot++) {
            ItemStack current = shopInventory.get(slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, returned)) continue;
            int add = Math.min(remaining, current.getMaxStackSize() - current.getCount());
            current.grow(add);
            remaining -= add;
        }
        for (int slot = 0; slot <= 53 && remaining > 0; slot++) {
            if (!shopInventory.get(slot).isEmpty()) continue;
            int add = Math.min(remaining, returned.getMaxStackSize());
            shopInventory.set(slot, returned.copyWithCount(add));
            remaining -= add;
        }
        ((BlockEntity) (Object) this).setChanged();
        return remaining == 0;
    }
}
