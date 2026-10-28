package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.primal.entity.animal.WalrusEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.WalrusEntity", remap = false)
public abstract class CreatureCareWalrusInstrumentMixin {
    @Unique private ItemStack jem$instrument = ItemStack.EMPTY;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$instrument = player.getItemInHand(hand).copy(); }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$accepted(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        WalrusEntity self = (WalrusEntity)(Object)this; var actor = CreatureCareProof.real(player);
        if (actor != null && self.isTamed() && actor.getUUID().equals(self.getOwnerUUID()) && self.isInstrument(jem$instrument)
                && callback.getReturnValue().consumesAction() && CreatureCareProof.debited(jem$instrument, player.getItemInHand(hand))
                && ItemStack.isSameItemSameTags(self.getMainHandItem(), jem$instrument)) {
            CreatureCareProof.mark(self, "walrus_instrument_actor", actor.getUUID());
            CreatureCareProof.data(self).put("walrus_instrument_stack", self.getMainHandItem().save(new net.minecraft.nbt.CompoundTag()));
            CreatureCareProof.data(self).putInt("walrus_instrument_song", self.getSongId());
        }
        jem$instrument = ItemStack.EMPTY;
    }
}
