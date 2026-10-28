package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatEffectProof;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ThrownPotion.class)
public abstract class CombatPotionApplicationQuestMixin {
    @Inject(method = "applySplash", at = @At("HEAD"))
    private void jem$splashBegin(List<MobEffectInstance> effects, Entity hit, CallbackInfo callback) { CombatEffectProof.applyBegin((ThrownPotion)(Object)this); }
    @Inject(method = "applySplash", at = @At("RETURN"))
    private void jem$splashEnd(List<MobEffectInstance> effects, Entity hit, CallbackInfo callback) { CombatEffectProof.applyEnd(); }
    @Inject(method = "makeAreaOfEffectCloud", at = @At("HEAD"))
    private void jem$cloudBegin(ItemStack item, Potion potion, CallbackInfo callback) { CombatEffectProof.applyBegin((ThrownPotion)(Object)this); }
    @Redirect(method = "makeAreaOfEffectCloud", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$cloud(Level level, Entity cloud) { boolean accepted = level.addFreshEntity(cloud); CombatEffectProof.cloud(cloud, accepted); return accepted; }
    @Inject(method = "makeAreaOfEffectCloud", at = @At("RETURN"))
    private void jem$cloudEnd(ItemStack item, Potion potion, CallbackInfo callback) { CombatEffectProof.applyEnd(); }
}
