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
@Mixin(targets = "net.sweenus.simplybows.world.EarthSpikeFieldManager", remap = false)
public abstract class CombatRuneEarthSpikeFieldQuestMixin {
    @Inject(method = "tickPainWaves", at = @At("HEAD"))
    private static void jem$tickPainWavesBegin(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.object(field, "pain"); }
    @Inject(method = "tickPainWaves", at = @At("RETURN"))
    private static void jem$tickPainWavesEnd(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyGraceAllySupport", at = @At("HEAD"))
    private static void jem$applyGraceAllySupportBegin(ServerLevel world, LivingEntity owner, Vec3 center, @Coerce Object tuning, CallbackInfo callback) { CombatRuneProof.branch("grace"); }
    @Inject(method = "applyGraceAllySupport", at = @At("RETURN"))
    private static void jem$applyGraceAllySupportEnd(ServerLevel world, LivingEntity owner, Vec3 center, @Coerce Object tuning, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyBountyCenterImpact", at = @At("HEAD"))
    private static void jem$applyBountyCenterImpactBegin(ServerLevel world, LivingEntity owner, Vec3 center, @Coerce Object tuning, CallbackInfo callback) { CombatRuneProof.branch("bounty"); }
    @Inject(method = "applyBountyCenterImpact", at = @At("RETURN"))
    private static void jem$applyBountyCenterImpactEnd(ServerLevel world, LivingEntity owner, Vec3 center, @Coerce Object tuning, CallbackInfo callback) { CombatRuneProof.leave(); }
}
