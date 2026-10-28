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
@Mixin(targets = "net.sweenus.simplybows.world.VineFlowerFieldManager", remap = false)
public abstract class CombatRuneVineFlowerFieldQuestMixin {
    @Inject(method = "tickChaosField", at = @At("HEAD"))
    private static void jem$tickChaosFieldBegin(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.object(field, "chaos"); }
    @Inject(method = "tickChaosField", at = @At("RETURN"))
    private static void jem$tickChaosFieldEnd(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyChaosRootMaintenance", at = @At("HEAD"))
    private static void jem$applyChaosRootMaintenanceBegin(ServerLevel world, @Coerce Object field, long now, CallbackInfo callback) { CombatRuneProof.object(field, "chaos"); }
    @Inject(method = "applyChaosRootMaintenance", at = @At("RETURN"))
    private static void jem$applyChaosRootMaintenanceEnd(ServerLevel world, @Coerce Object field, long now, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyAuraEffects", at = @At("HEAD"))
    private static void jem$applyAuraEffectsBegin(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.object(field, "pain,grace"); }
    @Inject(method = "applyAuraEffects", at = @At("RETURN"))
    private static void jem$applyAuraEffectsEnd(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "trySpawnBountyLoot", at = @At("HEAD"))
    private static void jem$trySpawnBountyLootBegin(ServerLevel world, LivingEntity entity, LivingEntity owner, double chance, CallbackInfo callback) { CombatRuneProof.branch("bounty"); }
    @Inject(method = "trySpawnBountyLoot", at = @At("RETURN"))
    private static void jem$trySpawnBountyLootEnd(ServerLevel world, LivingEntity entity, LivingEntity owner, double chance, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "triggerChaosExpiryBurst", at = @At("HEAD"))
    private static void jem$triggerChaosExpiryBurstBegin(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.object(field, "chaos"); }
    @Inject(method = "triggerChaosExpiryBurst", at = @At("RETURN"))
    private static void jem$triggerChaosExpiryBurstEnd(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.leave(); }
}
