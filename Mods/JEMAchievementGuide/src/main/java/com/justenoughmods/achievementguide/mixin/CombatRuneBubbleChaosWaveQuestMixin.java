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
@Mixin(targets = "net.sweenus.simplybows.world.BubbleChaosWaveManager", remap = false)
public abstract class CombatRuneBubbleChaosWaveQuestMixin {
    @Inject(method = "applyWaveDamage", at = @At("HEAD"))
    private static void jem$applyWaveDamageBegin(ServerLevel world, Vec3 center, @Coerce Object wave, CallbackInfo callback) { CombatRuneProof.object(wave, "chaos"); }
    @Inject(method = "applyWaveDamage", at = @At("RETURN"))
    private static void jem$applyWaveDamageEnd(ServerLevel world, Vec3 center, @Coerce Object wave, CallbackInfo callback) { CombatRuneProof.leave(); }
}
