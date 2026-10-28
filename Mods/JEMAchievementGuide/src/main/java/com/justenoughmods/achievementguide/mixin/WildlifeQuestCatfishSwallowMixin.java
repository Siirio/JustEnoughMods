package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCatfish", remap = false)
public abstract class WildlifeQuestCatfishSwallowMixin {
    @Inject(method = "swallowEntity", at = @At("RETURN"))
    private void jem$stored(Entity swallowed, CallbackInfoReturnable<Boolean> callback) {
        var self = (EntityCatfish)(Object)this;
        if (callback.getReturnValue() && self.hasSwallowedEntity() && self.getSwallowedEntityType().equals(WildlifeQuestProof.type(swallowed))) {
            var proof = WildlifeQuestProof.data(self); proof.putUUID("catfish_swallowed_source", swallowed.getUUID());
            proof.putString("catfish_swallowed_type", self.getSwallowedEntityType()); proof.put("catfish_swallowed_data", self.getSwallowedData().copy());
        }
    }
}
