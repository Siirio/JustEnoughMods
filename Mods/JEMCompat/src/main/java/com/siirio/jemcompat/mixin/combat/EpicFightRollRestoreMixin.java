package com.siirio.jemcompat.mixin.combat;

import com.siirio.jemcompat.compat.combat.RollClearance;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Pseudo
@Mixin(targets = "yesman.epicfight.gameasset.Animations$ReusableSources", remap = false)
public abstract class EpicFightRollRestoreMixin {
    @Unique
    private static final Set<ResourceLocation> JEMCOMPAT_ROLLS = Set.of(
            new ResourceLocation("epicfight", "biped/skill/roll_forward"),
            new ResourceLocation("epicfight", "biped/skill/roll_backward"),
            new ResourceLocation("epicfightx", "biped/skill/roll_forward"),
            new ResourceLocation("epicfightx", "biped/skill/roll_backward"),
            new ResourceLocation("efn", "biped/dodge/dodge_f"),
            new ResourceLocation("efn", "biped/dodge/dodge_b")
    );

    @Redirect(
            method = "lambda$static$1(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/asset/AssetAccessor;Lyesman/epicfight/api/animation/property/AnimationParameters;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6210_()V"),
            require = 1)
    private static void jemcompat$restoreRoll(LivingEntity entity, LivingEntityPatch<?> patch,
            AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> parameters) {
        if (entity instanceof Player player && JEMCOMPAT_ROLLS.contains(animation.registryName())) {
            RollClearance.restore(player);
        } else {
            entity.refreshDimensions();
        }
    }
}
