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
@Mixin(targets = "net.sweenus.simplybows.world.BubbleColumnFieldManager", remap = false)
public abstract class CombatRuneBubbleColumnFieldQuestMixin {
    @Inject(method = "applyBountySwarmDamage", at = @At("HEAD"))
    private static void jem$applyBountySwarmDamageBegin(ServerLevel world, @Coerce Object column, CallbackInfo callback) { CombatRuneProof.object(column, "bounty"); }
    @Inject(method = "applyBountySwarmDamage", at = @At("RETURN"))
    private static void jem$applyBountySwarmDamageEnd(ServerLevel world, @Coerce Object column, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "applyGraceAuras", at = @At("HEAD"))
    private static void jem$applyGraceAurasBegin(ServerLevel world, @Coerce Object column, CallbackInfo callback) { CombatRuneProof.object(column, "grace"); }
    @Inject(method = "applyGraceAuras", at = @At("RETURN"))
    private static void jem$applyGraceAurasEnd(ServerLevel world, @Coerce Object column, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "blockHostileProjectiles", at = @At("HEAD"))
    private static void jem$blockHostileProjectilesBegin(ServerLevel world, @Coerce Object column, CallbackInfo callback) { CombatRuneProof.object(column, "grace"); }
    @Inject(method = "blockHostileProjectiles", at = @At("RETURN"))
    private static void jem$blockHostileProjectilesEnd(ServerLevel world, @Coerce Object column, CallbackInfo callback) { CombatRuneProof.leave(); }
}
