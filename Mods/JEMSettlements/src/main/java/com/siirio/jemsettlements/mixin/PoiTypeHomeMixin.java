package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.compat.NativeBedCompatibility;
import java.util.Set;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep HOME's immutable record fields/hash identity intact while extending both public matching APIs. */
@Mixin(PoiType.class)
public abstract class PoiTypeHomeMixin {
    @Inject(method="is",at=@At("RETURN"),cancellable=true)
    private void jem$acceptHandcraftedHead(BlockState state,CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValueZ()&&NativeBedCompatibility.isHome((PoiType)(Object)this)&&NativeBedCompatibility.supportedHead(state))
            cir.setReturnValue(true);
    }
    @Inject(method="matchingStates",at=@At("RETURN"),cancellable=true)
    private void jem$completeHomeStates(CallbackInfoReturnable<Set<BlockState>> cir) {
        if(NativeBedCompatibility.isHome((PoiType)(Object)this))cir.setReturnValue(NativeBedCompatibility.homeStates());
    }
}
