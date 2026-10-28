package com.justenoughmods.achievementguide.mixin;

import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntitySeagull", remap = false)
public abstract class WildlifeQuestSeagullMapMixin {
    @Unique private WildlifeQuestFinders.Offering jem$offer;
    @Inject(method = "onGetItem", at = @At("HEAD"))
    private void jem$before(ItemEntity drop, CallbackInfo callback) {
        jem$offer = null;
        if (drop.getOwner() instanceof Player player && drop.getItem().is(AMTagRegistry.SEAGULL_OFFERINGS)) {
            var actor = WildlifeQuestProof.real(player); if (actor != null) jem$offer = new WildlifeQuestFinders.Offering(drop, drop.getItem().copy(), actor);
        }
    }
    @Inject(method = "onGetItem", at = @At("RETURN"))
    private void jem$after(ItemEntity drop, CallbackInfo callback) { WildlifeQuestFinders.guided((EntitySeagull)(Object)this, jem$offer); jem$offer = null; }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$arrival(CallbackInfo callback) { EntitySeagull self = (EntitySeagull)(Object)this; if (!self.level().isClientSide) WildlifeQuestFinders.arrived(self); }
}
