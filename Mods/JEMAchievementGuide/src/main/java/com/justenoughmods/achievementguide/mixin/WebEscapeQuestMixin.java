package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import dev.chybx.spideroverhaul.util.WebbedBreakProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WebEscapeQuestMixin {
    @Unique private boolean jem$nativeEscape;

    @Inject(method = "removeEffect", at = @At("HEAD"))
    private void jem$beforeWebRemoval(MobEffect effect, CallbackInfoReturnable<Boolean> callback) {
        LivingEntity target = (LivingEntity) (Object) this;
        MobEffectInstance active = target.getEffect(effect);
        jem$nativeEscape = target instanceof ServerPlayer && active != null && active.getDuration() > 0
                && new ResourceLocation("spider_overhaul", "webbed").equals(ForgeRegistries.MOB_EFFECTS.getKey(effect))
                && WebbedBreakProgress.get(target.getUUID()) >= 16;
    }

    @Inject(method = "removeEffect", at = @At("RETURN"))
    private void jem$afterWebRemoval(MobEffect effect, CallbackInfoReturnable<Boolean> callback) {
        if (jem$nativeEscape && Boolean.TRUE.equals(callback.getReturnValue())
                && (Object) this instanceof ServerPlayer actor && !actor.hasEffect(effect)) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_web_escape");
        }
        jem$nativeEscape = false;
    }
}
