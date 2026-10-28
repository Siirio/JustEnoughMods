package com.justenoughmods.achievementguide.mixin;
import com.github.alexmodguy.alexscaves.server.entity.living.RaycatEntity;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.RaycatEntity", remap = false)
public abstract class WildlifeQuestRaycatTransferMixin {
    @Unique private ServerPlayer jem$owner;
    @Unique private int jem$ownerRad;
    @Unique private int jem$catRad;
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        RaycatEntity self = (RaycatEntity)(Object)this; jem$owner = WildlifeQuestProof.owner(self); jem$ownerRad = -1; jem$catRad = self.hasEffect(ACEffectRegistry.IRRADIATED.get()) ? self.getEffect(ACEffectRegistry.IRRADIATED.get()).getAmplifier() : -1;
        if (jem$owner != null && self.getAbsorbTarget() == jem$owner && jem$owner.hasEffect(ACEffectRegistry.IRRADIATED.get())) jem$ownerRad = jem$owner.getEffect(ACEffectRegistry.IRRADIATED.get()).getAmplifier();
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$after(CallbackInfo callback) {
        RaycatEntity self = (RaycatEntity)(Object)this;
        if (jem$owner != null && jem$ownerRad >= 0 && self.getAbsorbTarget() == null && self.hasEffect(ACEffectRegistry.IRRADIATED.get()) && self.getEffect(ACEffectRegistry.IRRADIATED.get()).getAmplifier() == jem$catRad + 1
                && (!jem$owner.hasEffect(ACEffectRegistry.IRRADIATED.get()) || jem$owner.getEffect(ACEffectRegistry.IRRADIATED.get()).getAmplifier() == jem$ownerRad - 1)) WildlifeQuestProof.emit(jem$owner, "v3_fauna_companion_powers");
    }
}
