package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.BattleQuestPhoenix;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LivingEntity.class,priority=500)
public abstract class BattleQuestPhoenixDeathMixin {
    @Shadow protected boolean dead;
    @Unique private boolean jem$wasDead;
    @Inject(method="die",at=@At("HEAD"))private void jem$before(DamageSource source,CallbackInfo ci){jem$wasDead=dead;}
    @Inject(method="die",at=@At("RETURN"))private void jem$after(DamageSource source,CallbackInfo ci){BattleQuestPhoenix.death((LivingEntity)(Object)this,source,!jem$wasDead&&dead);}
}
