package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.compat.PackCompanionAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native age is normally not serialized; recovery must not restart a summon clock. */
@Mixin(Entity.class)
public abstract class PackSummonAgeMixin {
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void jemSaveAge(CompoundTag tag, CallbackInfoReturnable<CompoundTag> ci) {
        Entity self = (Entity) (Object) this;
        if (PackCompanionAdapter.preserveNativeAge(self) && self.getServer() != null
                && com.siirio.jemcompat.feature.shelter.CompanionAdapters.bound(self)) {
            if (self instanceof PackCompanionAdapter.NativeTransient state) state.jemShelterWriteTransient(tag);
            tag.putInt("jemShelterNativeAge", self.tickCount);
            tag.putLong("jemShelterNativeSavedAt", self.getServer().overworld().getGameTime());
        }
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void jemReadAge(CompoundTag tag, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (PackCompanionAdapter.preserveNativeAge(self) && tag.contains("jemShelterNativeAge") && self.getServer() != null) {
            if (self instanceof PackCompanionAdapter.NativeTransient state) state.jemShelterReadTransient(tag);
            self.tickCount = (int) Math.min(Integer.MAX_VALUE, tag.getInt("jemShelterNativeAge"));
        }
    }
}
