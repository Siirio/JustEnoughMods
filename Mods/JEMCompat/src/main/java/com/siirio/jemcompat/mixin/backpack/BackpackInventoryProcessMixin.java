package com.siirio.jemcompat.mixin.backpack;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import com.siirio.jemcompat.feature.backpack.*;
import com.siirio.jemidentity.api.IdentityPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native tick is already equipped-only. stillValid also checks exact stack identity and living owner. */
@Mixin(value = BackpackInventory.class, remap = false)
public abstract class BackpackInventoryProcessMixin implements BackpackFurnaceAccess {
    @Shadow @Final private Player player;
    @Unique private BackpackFurnace jemcompat$furnace;
    @Override public BackpackFurnace jemcompat$furnace() {
        if (jemcompat$furnace == null) jemcompat$furnace = new BackpackFurnace(((BackpackInventory)(Object)this).getBackpackStack());
        return jemcompat$furnace;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void jemcompat$tickProcesses(CallbackInfo callback) {
        if (!(player instanceof ServerPlayer owner) || owner.getServer() == null || !owner.getServer().isDedicatedServer()
                || !owner.isAlive() || owner.isSpectator() || !IdentityPolicy.permitsAccountMutation(owner)) return;
        var inventory = (BackpackInventory)(Object)this;
        if (!inventory.stillValid(owner)) return;
        var stack = inventory.getBackpackStack();
        if (BackpackHelper.findAugment(stack, FurnaceAugment.TYPE) != null) jemcompat$furnace().tick(owner);
        if (BackpackHelper.findAugment(stack, AutoFeedAugment.TYPE) != null) BackpackAutoFeed.tick(owner, inventory);
    }
}
