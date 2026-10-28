package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsFilterProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.AbstractFilteredMenu", remap = false)
public abstract class TomsFilterConfigurationQuestMixin {
    @Shadow @Final protected Inventory pinv;
    @Unique private CompoundTag jem$filterClick;
    @Unique private CompoundTag jem$filterMessage;
    @Inject(method = "m_150399_", at = @At("HEAD"))
    private void jem$beforeClick(int slot, int button, ClickType type, Player actor, CallbackInfo callback) {
        jem$filterClick = TomsFilterProof.beforeConfiguration((AbstractContainerMenu)(Object)this, actor);
    }
    @Inject(method = "m_150399_", at = @At("RETURN"))
    private void jem$afterClick(int slot, int button, ClickType type, Player actor, CallbackInfo callback) {
        var before = jem$filterClick; jem$filterClick = null;
        TomsFilterProof.configured((AbstractContainerMenu)(Object)this, actor, before);
    }
    @Inject(method = "receive", at = @At("HEAD"))
    private void jem$beforeMessage(CompoundTag data, CallbackInfo callback) {
        jem$filterMessage = TomsFilterProof.beforeConfiguration((AbstractContainerMenu)(Object)this, pinv.player);
    }
    @Inject(method = "receive", at = @At("RETURN"))
    private void jem$afterMessage(CompoundTag data, CallbackInfo callback) {
        var before = jem$filterMessage; jem$filterMessage = null;
        TomsFilterProof.configured((AbstractContainerMenu)(Object)this, pinv.player, before);
    }
}
