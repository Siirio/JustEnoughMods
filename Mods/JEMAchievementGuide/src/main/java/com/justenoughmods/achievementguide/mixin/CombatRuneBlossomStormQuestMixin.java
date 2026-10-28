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
@Mixin(targets = "net.sweenus.simplybows.world.BlossomStormManager", remap = false)
public abstract class CombatRuneBlossomStormQuestMixin {
    @Inject(method = "processBountyTrapTriggers", at = @At("HEAD"))
    private static void jem$processBountyTrapTriggersBegin(ServerLevel world, @Coerce Object storm, LivingEntity owner, CallbackInfoReturnable<Boolean> callback) { CombatRuneProof.object(storm, "bounty"); }
    @Inject(method = "processBountyTrapTriggers", at = @At("RETURN"))
    private static void jem$processBountyTrapTriggersEnd(ServerLevel world, @Coerce Object storm, LivingEntity owner, CallbackInfoReturnable<Boolean> callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyPainAreaDamage", at = @At("HEAD"))
    private static void jem$applyPainAreaDamageBegin(ServerLevel world, @Coerce Object storm, LivingEntity owner, CallbackInfo callback) { CombatRuneProof.object(storm, "pain"); }
    @Inject(method = "applyPainAreaDamage", at = @At("RETURN"))
    private static void jem$applyPainAreaDamageEnd(ServerLevel world, @Coerce Object storm, LivingEntity owner, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyGraceSupportPulse", at = @At("HEAD"))
    private static void jem$applyGraceSupportPulseBegin(ServerLevel world, @Coerce Object storm, LivingEntity owner, LivingEntity currentTarget, CallbackInfo callback) { CombatRuneProof.object(storm, "grace"); }
    @Inject(method = "applyGraceSupportPulse", at = @At("RETURN"))
    private static void jem$applyGraceSupportPulseEnd(ServerLevel world, @Coerce Object storm, LivingEntity owner, LivingEntity currentTarget, CallbackInfo callback) { CombatRuneProof.leave(); }
}
