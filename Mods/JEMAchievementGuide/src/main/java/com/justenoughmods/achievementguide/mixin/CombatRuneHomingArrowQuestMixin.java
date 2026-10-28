package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.HomingArrowEntity", remap = false)
public abstract class CombatRuneHomingArrowQuestMixin {
    @Inject(method = "m_5790_", at = @At("HEAD"))
    private void jem$m_5790_Begin(net.minecraft.world.phys.EntityHitResult hit, CallbackInfo callback) { CombatRuneProof.arrow((Entity)(Object)this); }
    @Inject(method = "m_5790_", at = @At("RETURN"))
    private void jem$m_5790_End(net.minecraft.world.phys.EntityHitResult hit, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "m_8060_", at = @At("HEAD"))
    private void jem$m_8060_Begin(net.minecraft.world.phys.BlockHitResult hit, CallbackInfo callback) { CombatRuneProof.arrow((Entity)(Object)this); }
    @Inject(method = "m_8060_", at = @At("RETURN"))
    private void jem$m_8060_End(net.minecraft.world.phys.BlockHitResult hit, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$m_8119_Begin(CallbackInfo callback) { CombatRuneProof.arrow((Entity)(Object)this); }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$m_8119_End(CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "m_7761_", at = @At("HEAD"))
    private void jem$m_7761_Begin(LivingEntity target, CallbackInfo callback) { CombatRuneProof.arrow((Entity)(Object)this); }
    @Inject(method = "m_7761_", at = @At("RETURN"))
    private void jem$m_7761_End(LivingEntity target, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyStackingSlow", at = @At("HEAD"))
    private void jem$applyStackingSlowBegin(LivingEntity target, CallbackInfo callback) { CombatRuneProof.branch("grace"); }
    @Inject(method = "applyStackingSlow", at = @At("RETURN"))
    private void jem$applyStackingSlowEnd(LivingEntity target, CallbackInfo callback) { CombatRuneProof.leave(); }
}
