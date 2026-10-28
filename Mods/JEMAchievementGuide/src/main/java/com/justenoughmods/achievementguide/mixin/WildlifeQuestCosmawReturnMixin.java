package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityCosmaw;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCosmaw", remap = false)
public abstract class WildlifeQuestCosmawReturnMixin implements WildlifeQuestMotion.NativeRescuer {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityCosmaw;m_20153_()V"))
    private void jem$returned(EntityCosmaw cosmaw) {
        var actor = WildlifeQuestProof.actor(cosmaw, "cosmaw_native_pickup"); boolean aboard = actor != null && actor.getVehicle() == cosmaw;
        cosmaw.ejectPassengers();
        if (aboard && actor.getVehicle() != cosmaw) WildlifeQuestMotion.rescued(actor, cosmaw);
        WildlifeQuestProof.data(cosmaw).remove("cosmaw_native_pickup");
    }
    @Override public boolean jem$wildlifeRescuerOwns(ServerPlayer actor) {
        TamableAnimal self = (TamableAnimal)(Object)this; return self.isAlive() && self.isTame() && actor.getUUID().equals(self.getOwnerUUID());
    }
}
