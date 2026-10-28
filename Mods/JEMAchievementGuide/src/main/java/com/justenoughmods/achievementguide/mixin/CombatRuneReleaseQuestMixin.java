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
import net.minecraft.world.InteractionHand;
import java.util.List;
@Pseudo
@Mixin(targets = {"net.sweenus.simplybows.item.unique.BeeBowItem", "net.sweenus.simplybows.item.unique.BlossomBowItem", "net.sweenus.simplybows.item.unique.BubbleBowItem", "net.sweenus.simplybows.item.unique.CosmicBowItem", "net.sweenus.simplybows.item.unique.EarthBowItem", "net.sweenus.simplybows.item.unique.EchoBowItem", "net.sweenus.simplybows.item.unique.IceBowItem", "net.sweenus.simplybows.item.unique.VineBowItem"}, remap = false)
public abstract class CombatRuneReleaseQuestMixin {
    @Inject(method = "performStoppedUsing", at = @At("HEAD"))
    private void jem$begin(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack bow, List<ItemStack> ammunition, float charge, float speed, boolean critical, LivingEntity target, CallbackInfo callback) { CombatRuneProof.beginBow(shooter, bow); }
    @Inject(method = "performStoppedUsing", at = @At("RETURN"))
    private void jem$end(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack bow, List<ItemStack> ammunition, float charge, float speed, boolean critical, LivingEntity target, CallbackInfo callback) { CombatRuneProof.leave(); }
}
