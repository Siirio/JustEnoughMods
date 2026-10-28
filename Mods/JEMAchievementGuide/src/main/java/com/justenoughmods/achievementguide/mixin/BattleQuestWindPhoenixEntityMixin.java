package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestPhoenix;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=net.mcreator.luminousbeasts.entity.WindPhoenixEntity.class,remap=false)
public abstract class BattleQuestWindPhoenixEntityMixin {
    @Inject(method="m_6153_",at=@At("HEAD"))private void jem$before(CallbackInfo ci){BattleQuestPhoenix.begin((Entity)(Object)this);}
    @Inject(method="m_6153_",at=@At("RETURN"))private void jem$after(CallbackInfo ci){BattleQuestPhoenix.end();}
}
