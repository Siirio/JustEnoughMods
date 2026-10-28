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
@Mixin(targets = "net.sweenus.simplybows.world.BlossomChaosKoiManager", remap = false)
public abstract class CombatRuneBlossomChaosKoiQuestMixin {
    @Inject(method = "handleEntityContact", at = @At("HEAD"))
    private static void jem$handleEntityContactBegin(ServerLevel world, @Coerce Object effect, @Coerce Object koi, long now, CallbackInfo callback) { CombatRuneProof.object(effect, "chaos"); }
    @Inject(method = "handleEntityContact", at = @At("RETURN"))
    private static void jem$handleEntityContactEnd(ServerLevel world, @Coerce Object effect, @Coerce Object koi, long now, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "handleProjectileReflection", at = @At("HEAD"))
    private static void jem$handleProjectileReflectionBegin(ServerLevel world, @Coerce Object effect, @Coerce Object koi, long now, CallbackInfo callback) { CombatRuneProof.object(effect, "chaos"); }
    @Inject(method = "handleProjectileReflection", at = @At("RETURN"))
    private static void jem$handleProjectileReflectionEnd(ServerLevel world, @Coerce Object effect, @Coerce Object koi, long now, CallbackInfo callback) { CombatRuneProof.leave(); }
}
