package com.siirio.jemserver.mixin.shops;

import com.siirio.jemserver.smp.shops.ShopStock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.spudacious5705.shops.block.entity.ShopInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.spudacious5705.shops.block.entity.ShopInventory", remap = false)
public abstract class ShopInventoryMixin implements ShopStock {
    @Shadow @Final protected static int STOCK_END;

    @Override @Invoker("outOfStock") public abstract boolean jemOutOfStock();
    @Override @Invoker("paymentRegisterFull") public abstract boolean jemPaymentFull();

    @Override
    public List<ItemStack> jemChoices() {
        var choices = new ArrayList<ItemStack>();
        var inventory = (ShopInventory) (Object) this;
        for (int slot = 0; slot <= STOCK_END; slot++) {
            var stack = inventory.get(slot);
            if (!stack.isEmpty() && choices.stream().noneMatch(item -> ItemStack.isSameItemSameTags(item, stack))) choices.add(stack.copy());
        }
        return choices;
    }

    @Override
    public int jemAvailableTrades(ItemStack output, int quantity) {
        if (output.isEmpty() || quantity <= 0) return 0;
        var inventory = (ShopInventory) (Object) this;
        long available = 0;
        for (int slot = 0; slot <= STOCK_END; slot++) {
            var stack = inventory.get(slot);
            if (ItemStack.isSameItemSameTags(output, stack)) available += stack.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, available / quantity);
    }
}
