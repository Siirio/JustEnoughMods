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
@Mixin(targets = "net.sweenus.simplybows.world.IceChaosWallManager", remap = false)
public abstract class CombatRuneIceChaosWallQuestMixin {
    @Inject(method = "blockProjectiles", at = @At("HEAD"))
    private static void jem$blockProjectilesBegin(ServerLevel world, @Coerce Object wall, CallbackInfo callback) { CombatRuneProof.object(wall, "chaos"); }
    @Inject(method = "blockProjectiles", at = @At("RETURN"))
    private static void jem$blockProjectilesEnd(ServerLevel world, @Coerce Object wall, CallbackInfo callback) { CombatRuneProof.leave(); }
    @Inject(method = "blockEntities", at = @At("HEAD"))
    private static void jem$blockEntitiesBegin(ServerLevel world, @Coerce Object wall, CallbackInfo callback) { CombatRuneProof.object(wall, "chaos"); }
    @Inject(method = "blockEntities", at = @At("RETURN"))
    private static void jem$blockEntitiesEnd(ServerLevel world, @Coerce Object wall, CallbackInfo callback) { CombatRuneProof.leave(); }
}
