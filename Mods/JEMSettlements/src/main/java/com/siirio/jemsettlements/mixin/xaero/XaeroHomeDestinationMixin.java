package com.siirio.jemsettlements.mixin.xaero;

import com.siirio.jemsettlements.compat.xaero.XaeroHomeMap;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="xaero.map.gui.GuiMap",remap=false)
public abstract class XaeroHomeDestinationMixin {
    @Shadow private double cameraX; @Shadow private double cameraZ; @Shadow private boolean shouldResetCameraPos;
    @Inject(method="m_7856_",at=@At("TAIL"))
    private void jem$centerHome(CallbackInfo ci) { var target = XaeroHomeMap.consume(); var level = Minecraft.getInstance().level; if (target == null || level == null || !level.dimension().location().equals(target.dimension())) return; cameraX = target.pos().getX()+.5; cameraZ = target.pos().getZ()+.5; shouldResetCameraPos = false; }
}
