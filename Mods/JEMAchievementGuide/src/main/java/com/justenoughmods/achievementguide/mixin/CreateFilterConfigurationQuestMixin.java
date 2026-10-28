package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateFunnelRun;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour", remap = false)
public abstract class CreateFilterConfigurationQuestMixin {
    @Unique private CompoundTag jem$prior;
    @Unique private CompoundTag jem$snapshot() {
        CompoundTag tag = new CompoundTag();
        ((FilteringBehaviour)(Object)this).write(tag, false);
        return tag;
    }
    @Unique private void jem$changed(Player player) {
        CompoundTag now = jem$snapshot();
        if (jem$prior != null && !jem$prior.equals(now) && player instanceof ServerPlayer actor
                && ((BlockEntityBehaviour)(Object)this).blockEntity instanceof CreateFunnelRun funnel) {
            funnel.jem$filterConfiguration(actor, now);
        }
        jem$prior = null;
    }
    @Inject(method = "onShortInteract", at = @At("HEAD"), remap = false)
    private void jem$beforeShort(Player actor, InteractionHand hand, Direction side, BlockHitResult hit, CallbackInfo callback) { jem$prior = jem$snapshot(); }
    @Inject(method = "onShortInteract", at = @At("RETURN"), remap = false)
    private void jem$afterShort(Player actor, InteractionHand hand, Direction side, BlockHitResult hit, CallbackInfo callback) { jem$changed(actor); }
    @Inject(method = "setValueSettings", at = @At("HEAD"), remap = false)
    private void jem$beforeValue(Player actor, ValueSettingsBehaviour.ValueSettings settings, boolean control, CallbackInfo callback) { jem$prior = jem$snapshot(); }
    @Inject(method = "setValueSettings", at = @At("RETURN"), remap = false)
    private void jem$afterValue(Player actor, ValueSettingsBehaviour.ValueSettings settings, boolean control, CallbackInfo callback) { jem$changed(actor); }
    @Inject(method = "readFromClipboard", at = @At("HEAD"), remap = false)
    private void jem$beforeClipboard(CompoundTag data, Player actor, Direction side, boolean simulate, CallbackInfoReturnable<Boolean> callback) { jem$prior = jem$snapshot(); }
    @Inject(method = "readFromClipboard", at = @At("RETURN"), remap = false)
    private void jem$afterClipboard(CompoundTag data, Player actor, Direction side, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        if (!simulate && callback.getReturnValueZ()) jem$changed(actor); else jem$prior = null;
    }
}
