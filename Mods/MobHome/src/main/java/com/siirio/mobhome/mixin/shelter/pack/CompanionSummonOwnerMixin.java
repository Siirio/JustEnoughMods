package com.siirio.mobhome.mixin.shelter.pack;

import com.siirio.mobhome.feature.shelter.compat.PackCompanionAdapter;
import java.util.UUID;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "dev.xylonity.companions.common.entity.CompanionSummonEntity", remap = false)
public abstract class CompanionSummonOwnerMixin implements PackCompanionAdapter.NativeOwner {
    @Shadow public abstract UUID getSecondOwnerUUID();
    @Override public UUID jemShelterNativeOwner() {
        UUID second = getSecondOwnerUUID();
        return second != null ? second : ((TamableAnimal) (Object) this).getOwnerUUID();
    }
}
