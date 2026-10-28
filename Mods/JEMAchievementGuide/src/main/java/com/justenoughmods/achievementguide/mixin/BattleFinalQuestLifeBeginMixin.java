package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.BattleFinalQuestLife;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=LivingEntity.class,priority=2000)
public abstract class BattleFinalQuestLifeBeginMixin {
    @Shadow protected boolean dead;
    @Inject(method="hurt",at=@At("HEAD"))private void jem$beforeHurt(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){BattleFinalQuestLife.hurtBegin((LivingEntity)(Object)this,source);
        if(net.minecraftforge.fml.ModList.get().isLoaded("efn")&&net.minecraftforge.fml.ModList.get().isLoaded("epicfight"))com.justenoughmods.achievementguide.integration.battle.BattleQuestDamage.begin((LivingEntity)(Object)this,source);
        if(net.minecraftforge.fml.ModList.get().isLoaded("companions")&&net.minecraftforge.fml.ModList.get().isLoaded("knightlib"))com.justenoughmods.achievementguide.integration.companions.CompanionQuestSpell.damageBegin((LivingEntity)(Object)this,source);
        if(net.minecraftforge.fml.ModList.get().isLoaded("unearthed_journey"))com.justenoughmods.achievementguide.integration.prehistoric.WorldPrehistoricQuestDart.damageScopeBegin((LivingEntity)(Object)this,source,amount);
        if(net.minecraftforge.fml.ModList.get().isLoaded("cataclysm")&&net.minecraftforge.fml.ModList.get().isLoaded("curios")&&net.minecraftforge.fml.ModList.get().isLoaded("lionfishapi"))com.justenoughmods.achievementguide.integration.battlearmor.WorldBattleArmorQuestDamage.scopeBegin((LivingEntity)(Object)this,source,amount);}
    @Inject(method="die",at=@At("HEAD"))private void jem$beforeDeath(DamageSource source,CallbackInfo ci){BattleFinalQuestLife.deathBegin((LivingEntity)(Object)this,source,dead);}
}
