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
@Mixin(targets = "net.sweenus.simplybows.world.CosmicChaosSunManager", remap = false)
public abstract class CombatRuneCosmicChaosSunQuestMixin {
    @Inject(method = "tryCaptureProjectile", at = @At("HEAD"))
    private static void jem$tryCaptureProjectileBegin(ServerLevel world, Projectile projectile, CallbackInfo callback) { CombatRuneProof.branch("chaos"); }
    @Inject(method = "tryCaptureProjectile", at = @At("RETURN"))
    private static void jem$tryCaptureProjectileEnd(ServerLevel world, Projectile projectile, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "grantCelestialSwiftness", at = @At("HEAD"))
    private static void jem$grantCelestialSwiftnessBegin(ServerLevel world, @Coerce Object sun, CallbackInfo callback) { CombatRuneProof.object(sun, "chaos"); }
    @Inject(method = "grantCelestialSwiftness", at = @At("RETURN"))
    private static void jem$grantCelestialSwiftnessEnd(ServerLevel world, @Coerce Object sun, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "fireOneProjectile", at = @At("HEAD"))
    private static void jem$fireOneProjectileBegin(ServerLevel world, @Coerce Object sun, CallbackInfo callback) { CombatRuneProof.object(sun, "chaos"); }
    @Inject(method = "fireOneProjectile", at = @At("RETURN"))
    private static void jem$fireOneProjectileEnd(ServerLevel world, @Coerce Object sun, CallbackInfo callback) { CombatRuneProof.leave(); }
}
