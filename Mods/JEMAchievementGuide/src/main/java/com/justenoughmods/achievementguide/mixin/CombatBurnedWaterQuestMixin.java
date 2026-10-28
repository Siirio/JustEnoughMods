package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatWaterProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatCorpseProof;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.teamfusion.rottencreatures.common.level.entities.living.burned.Burned", remap = false)
public abstract class CombatBurnedWaterQuestMixin {
    @Shadow public abstract boolean isObsidian();
    @Unique private boolean jem$wasObsidian;
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { jem$wasObsidian = isObsidian(); }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$end(CallbackInfo callback) { if (!jem$wasObsidian && isObsidian()) CombatWaterProof.converted((Entity)(Object)this); }
    @Inject(method = "setObsidian", at = @At("RETURN"))
    private void jem$state(boolean state, CallbackInfo callback) { if (!isObsidian()) ((Entity)(Object)this).getPersistentData().remove(CombatCorpseProof.WATER); }
}
