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
@Mixin(targets = "net.sweenus.simplybows.world.CosmicBountyManager", remap = false)
public abstract class CombatRuneCosmicBountyQuestMixin {
    @Inject(method = "explode", at = @At("HEAD"))
    private static void jem$explodeBegin(ServerLevel world, @Coerce Object star, CallbackInfo callback) { CombatRuneProof.object(star, "bounty"); }
    @Inject(method = "explode", at = @At("RETURN"))
    private static void jem$explodeEnd(ServerLevel world, @Coerce Object star, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "damageStardustTargets", at = @At("HEAD"))
    private static void jem$damageStardustTargetsBegin(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.object(field, "bounty"); }
    @Inject(method = "damageStardustTargets", at = @At("RETURN"))
    private static void jem$damageStardustTargetsEnd(ServerLevel world, @Coerce Object field, CallbackInfo callback) { CombatRuneProof.leave(); }
}
