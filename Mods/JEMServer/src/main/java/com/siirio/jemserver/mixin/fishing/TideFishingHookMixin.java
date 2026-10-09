package com.siirio.jemserver.mixin.fishing;

import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import com.siirio.jemserver.smp.fishing.FishingProfiles;
import com.siirio.jemserver.smp.fishing.FishingRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TideFishingHook.class, remap = false)
public abstract class TideFishingHookMixin {
    @Inject(method = "retrieve(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", shift = At.Shift.AFTER))
    private void jemCatch(ItemStack rod, ServerLevel level, Player player, CallbackInfoReturnable<Integer> result) {
        FishingRuntime.caught((TideFishingHook) (Object) this);
    }
    @Redirect(method = "retrieve(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean jemCatchExperience(Level level, Entity entity) {
        if (entity instanceof ExperienceOrb && ((TideFishingHook) (Object) this).getPersistentData().getBoolean("jem_fishing_rewarded")) return false;
        return level.addFreshEntity(entity);
    }
    @Inject(method = "canFishInLava", at = @At("RETURN"), cancellable = true)
    private void jemLava(CallbackInfoReturnable<Boolean> result) {
        if (FishingProfiles.get(((TideFishingHook) (Object) this).rod()).intrinsicLava()) result.setReturnValue(true);
    }
    @Inject(method = "canFishInVoid", at = @At("RETURN"), cancellable = true)
    private void jemVoid(CallbackInfoReturnable<Boolean> result) {
        if (FishingProfiles.get(((TideFishingHook) (Object) this).rod()).intrinsicVoid()) result.setReturnValue(true);
    }
}
