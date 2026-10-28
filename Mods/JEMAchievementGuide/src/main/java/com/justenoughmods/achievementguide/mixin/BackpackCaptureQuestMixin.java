package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCaptureProof;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler", remap = false)
public abstract class BackpackCaptureQuestMixin {
    @Inject(method = "capture", at = @At("HEAD"))
    private static void jem$beginCapture(ServerPlayer actor, IBackpackWrapper wrapper, LivingEntity target, boolean advanced,
                                         CallbackInfoReturnable<?> callback) { BackpackCaptureProof.beginCapture(actor, wrapper, target); }
    @Redirect(method = "capture", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedbackpacks/upgrades/mobcatcher/MobCatcherStorage;addCapturedMob(Lnet/p3pp3rf1y/sophisticatedbackpacks/backpack/wrapper/IBackpackWrapper;Lnet/p3pp3rf1y/sophisticatedbackpacks/upgrades/mobcatcher/CapturedMob;)V"))
    private static void jem$stored(IBackpackWrapper wrapper, CapturedMob record) {
        MobCatcherStorage.addCapturedMob(wrapper, record);
        BackpackCaptureProof.stored(wrapper, record);
    }
    @Inject(method = "capture", at = @At("RETURN"))
    private static void jem$captured(ServerPlayer actor, IBackpackWrapper wrapper, LivingEntity target, boolean advanced,
                                    CallbackInfoReturnable<?> callback) { BackpackCaptureProof.finishCapture(); }

    @Inject(method = "release", at = @At("HEAD"))
    private static void jem$beginRelease(ServerPlayer actor, UUID id, CallbackInfo callback) { BackpackCaptureProof.beginRelease(actor, id); }
    @Redirect(method = "release", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedbackpacks/backpack/wrapper/BackpackLinkedStorageResolver;resolveForGlobalUpgradeProcessing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/p3pp3rf1y/sophisticatedbackpacks/backpack/wrapper/IBackpackWrapper;"))
    private static IBackpackWrapper jem$nativeReleaseWrapper(Level level, ItemStack backpack) {
        IBackpackWrapper wrapper = BackpackLinkedStorageResolver.resolveForGlobalUpgradeProcessing(level, backpack);
        BackpackCaptureProof.releaseWrapper(wrapper);
        return wrapper;
    }
    @Redirect(method = "release", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$released(ServerLevel level, Entity entity) {
        boolean accepted = level.addFreshEntity(entity);
        BackpackCaptureProof.spawned(entity, accepted);
        return accepted;
    }
    @Inject(method = "release", at = @At("RETURN"))
    private static void jem$finishedRelease(ServerPlayer actor, UUID id, CallbackInfo callback) { BackpackCaptureProof.finishRelease(); }
}
