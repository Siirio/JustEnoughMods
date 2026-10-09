package com.siirio.jemcompat.mixin.compat.tradecycling;

import com.siirio.jemcompat.discovery.CampaignDiscoveryEvents;
import de.maxhenkel.tradecycling.TradeCyclingMod;
import de.maxhenkel.tradecycling.mixin.MerchantMenuAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TradeCyclingMod.class, remap = false)
public abstract class TradeCyclingModMixin {
    @Inject(method = "onCycleTrades", at = @At("HEAD"), cancellable = true)
    private static void jemTwelveEyes$protectLockedCartographerTrades(ServerPlayer player, CallbackInfo callback) {
        if (player.containerMenu instanceof MerchantMenuAccessor accessor
                && accessor.getTrader() instanceof Villager villager
                && villager.getVillagerData().getProfession() == VillagerProfession.CARTOGRAPHER
                && villager.getVillagerXp() > 0) {
            callback.cancel();
        }
    }

    @Inject(method = "onCycleTrades", at = @At(value = "INVOKE",
            target = "Lde/maxhenkel/tradecycling/mixin/VillagerAccessor;invokeUpdateSpecialPrices(Lnet/minecraft/world/entity/player/Player;)V",
            remap = false))
    private static void jemTwelveEyes$rerollLocatorTrades(ServerPlayer player, CallbackInfo callback) {
        if (!(player.containerMenu instanceof MerchantMenu menu)
                || !(menu instanceof MerchantMenuAccessor accessor)
                || !(accessor.getTrader() instanceof Villager villager)
                || villager.getVillagerData().getProfession() != VillagerProfession.CARTOGRAPHER) {
            return;
        }
        CampaignDiscoveryEvents.rerollCartographerTrades(villager);
    }
}
