package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.compat.PackCompanionAdapter;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.capabilities.magic.SummonManager$ExpirationInstance", remap = false)
public interface IronSummonExpirationAccessor extends PackCompanionAdapter.NativeExpiration {
    @Override @Accessor("uuid") UUID jemShelterId();
    @Override @Accessor("expirationServerTick") int jemShelterExpiry();
}
