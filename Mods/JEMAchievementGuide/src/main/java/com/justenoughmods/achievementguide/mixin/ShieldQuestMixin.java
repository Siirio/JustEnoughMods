package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ShieldQuestMixin {
    @Unique private final ArrayDeque<Boolean> jem$shieldProof = new ArrayDeque<>();

    @Inject(method = "hurt", at = @At("HEAD"))
    private void jem$beforeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        jem$shieldProof.push(false);
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeHooks;onShieldBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)Lnet/minecraftforge/event/entity/living/ShieldBlockEvent;", remap = false))
    private ShieldBlockEvent jem$observeNativeBlock(LivingEntity target, DamageSource source, float amount) {
        ShieldBlockEvent result = ForgeHooks.onShieldBlock(target, source, amount);
        boolean committed = !result.isCanceled() && Float.isFinite(result.getBlockedDamage()) && result.getBlockedDamage() > 0;
        if (!jem$shieldProof.isEmpty()) {
            jem$shieldProof.pop();
            jem$shieldProof.push(committed);
        }
        return result;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void jem$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        boolean blocked = !jem$shieldProof.isEmpty() && jem$shieldProof.pop();
        if (blocked && (Object) this instanceof ServerPlayer actor) {
            QuestActionTrigger.INSTANCE.emit(actor, "shield_block");
        }
    }
}
