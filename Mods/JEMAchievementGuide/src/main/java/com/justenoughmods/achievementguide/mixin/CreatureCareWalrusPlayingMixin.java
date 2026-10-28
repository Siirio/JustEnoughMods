package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.primal.entity.animal.WalrusEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.entity.ai.behavior.walrus.WalrusPlayInstrument", remap = false)
public abstract class CreatureCareWalrusPlayingMixin {
    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lorg/primal/entity/animal/WalrusEntity;J)V", at = @At("RETURN"))
    private void jem$played(ServerLevel level, WalrusEntity walrus, long time, CallbackInfo callback) {
        var actor = CreatureCareProof.actor(walrus, "walrus_instrument_actor");
        if (actor != null && walrus.isTamed() && actor.getUUID().equals(walrus.getOwnerUUID())
                && walrus.getPose() == Pose.ROARING && walrus.isInstrument(walrus.getMainHandItem())
                && CreatureCareProof.data(walrus).getInt("walrus_instrument_song") == walrus.getSongId()
                && ItemStack.isSameItemSameTags(walrus.getMainHandItem(), ItemStack.of(CreatureCareProof.data(walrus).getCompound("walrus_instrument_stack")))) {
            CreatureCareProof.emit(actor, "v3_fauna_walrus_signal_played");
            CreatureCareProof.data(walrus).remove("walrus_instrument_actor");
            CreatureCareProof.data(walrus).remove("walrus_instrument_stack");
        }
    }
}
