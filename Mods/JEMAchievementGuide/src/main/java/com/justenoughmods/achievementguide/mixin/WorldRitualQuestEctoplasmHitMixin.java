package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=net.mcreator.luminousnether.entity.EctoplasmShotEntity.class,remap=false)
public abstract class WorldRitualQuestEctoplasmHitMixin {
    @Inject(method="m_8060_",at=@At("HEAD"))private void jem$before(net.minecraft.world.phys.BlockHitResult result,CallbackInfo ci){WorldRitualQuestEctoplasm.hit((net.mcreator.luminousnether.entity.EctoplasmShotEntity)(Object)this,result);}
    @Inject(method="m_8060_",at=@At("RETURN"))private void jem$after(net.minecraft.world.phys.BlockHitResult result,CallbackInfo ci){WorldRitualQuestEctoplasm.end();}
}
