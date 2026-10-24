package com.siirio.jemfood.mixin;

import com.siirio.jemfood.FoodEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingMixin {
    @Unique private ItemFishedEvent jemfood$catch;
    @Inject(method = "retrieve", at = @At("HEAD"))
    private void start(ItemStack rod, CallbackInfoReturnable<Integer> callback) { jemfood$catch = null; }
    @Redirect(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false))
    private boolean afterListeners(IEventBus bus, Event event) {
        boolean canceled = bus.post(event);
        if (event instanceof ItemFishedEvent caught) jemfood$catch = caught;
        return canceled;
    }
    @Inject(method = "retrieve", at = @At("RETURN"))
    private void complete(ItemStack rod, CallbackInfoReturnable<Integer> callback) {
        if (jemfood$catch != null && !jemfood$catch.isCanceled() && !jemfood$catch.getDrops().isEmpty()
                && jemfood$catch.getEntity() instanceof ServerPlayer player) FoodEvents.fished(player);
        jemfood$catch = null;
    }
}
