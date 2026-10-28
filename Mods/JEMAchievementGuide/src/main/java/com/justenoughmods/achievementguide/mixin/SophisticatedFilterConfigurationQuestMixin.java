package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SophisticatedFilterProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class SophisticatedFilterConfigurationQuestMixin {
    @Shadow @Final protected Player player;
    @Unique private SophisticatedFilterProof.Settings jem$filterClick;
    @Unique private SophisticatedFilterProof.Settings jem$filterMessage;
    @Unique private SophisticatedFilterProof.Settings jem$filterGhost;
    @Inject(method = "m_150399_", at = @At("HEAD"))
    private void jem$beforeClick(int slot, int button, ClickType type, Player actor, CallbackInfo callback) {
        jem$filterClick = SophisticatedFilterProof.before((StorageContainerMenuBase<?>)(Object)this, actor);
    }
    @Inject(method = "m_150399_", at = @At("RETURN"))
    private void jem$afterClick(int slot, int button, ClickType type, Player actor, CallbackInfo callback) {
        var before = jem$filterClick; jem$filterClick = null;
        SophisticatedFilterProof.configured((StorageContainerMenuBase<?>)(Object)this, actor, before);
    }
    @Inject(method = "handleMessage", at = @At("HEAD"))
    private void jem$beforeMessage(CompoundTag data, CallbackInfo callback) {
        jem$filterMessage = SophisticatedFilterProof.before((StorageContainerMenuBase<?>)(Object)this, player);
    }
    @Inject(method = "handleMessage", at = @At("RETURN"))
    private void jem$afterMessage(CompoundTag data, CallbackInfo callback) {
        var before = jem$filterMessage; jem$filterMessage = null;
        SophisticatedFilterProof.configured((StorageContainerMenuBase<?>)(Object)this, player, before);
    }
    @Inject(method = "setGhostSlot", at = @At("HEAD"))
    private void jem$beforeGhost(int slot, ItemStack stack, CallbackInfo callback) {
        jem$filterGhost = SophisticatedFilterProof.before((StorageContainerMenuBase<?>)(Object)this, player);
    }
    @Inject(method = "setGhostSlot", at = @At("RETURN"))
    private void jem$afterGhost(int slot, ItemStack stack, CallbackInfo callback) {
        var before = jem$filterGhost; jem$filterGhost = null;
        SophisticatedFilterProof.configured((StorageContainerMenuBase<?>)(Object)this, player, before);
    }
}
