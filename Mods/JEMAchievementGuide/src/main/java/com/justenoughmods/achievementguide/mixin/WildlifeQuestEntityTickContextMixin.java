package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestContext;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey", "com.github.alexthe666.alexsmobs.entity.EntityCrow", "com.github.alexthe666.alexsmobs.entity.EntityHummingbird"}, remap = false)
public abstract class WildlifeQuestEntityTickContextMixin {
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) { WildlifeQuestContext.begin((Entity)(Object)this); }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$after(CallbackInfo callback) { WildlifeQuestContext.end((Entity)(Object)this); }
}
