package com.siirio.jemcompat.mixin.trading;

import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge owns missing-type handling. Preserve FFAPI's additional empty/AIR
 * rejection without relying on Forge's differing local-variable layout. */
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EmeraldsForVillagerTypeItem")
public abstract class FabricTypeAwareTradeMixin {
    @Shadow @Final private Map<VillagerType, Item> trades;
    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void jemcompat$rejectAir(Entity entity, RandomSource random, CallbackInfoReturnable<MerchantOffer> callback) {
        if (entity instanceof VillagerDataHolder holder && trades.get(holder.getVillagerData().getType()) == Items.AIR)
            callback.setReturnValue(null);
    }
}
