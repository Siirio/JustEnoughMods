package com.siirio.jemprofessions.mixin.sandbox.core;

import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public abstract class SheepShearMixin {
    @Unique private boolean jem$ready;
    @Inject(method = "onSheared", at = @At("HEAD"), remap = false)
    private void jem$before(Player player, ItemStack item, Level level, BlockPos pos, int fortune, CallbackInfoReturnable<List<ItemStack>> callback) { jem$ready = ((Sheep) (Object) this).readyForShearing(); }
    @Inject(method = "onSheared", at = @At("RETURN"), remap = false)
    private void jem$after(Player player, ItemStack item, Level level, BlockPos pos, int fortune, CallbackInfoReturnable<List<ItemStack>> callback) {
        if (jem$ready && player instanceof ServerPlayer server && ((Sheep) (Object) this).isSheared() && !callback.getReturnValue().isEmpty()) SandboxCoreEffects.sheared(server, (Sheep) (Object) this);
    }
}
