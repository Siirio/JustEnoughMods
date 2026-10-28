package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFishingConversionProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "koala.fishingreal.FishingReal", remap = false)
public abstract class RemainingFishingConversionQuestMixin {
    @Redirect(method = "fishUpEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$acceptedConversion(Level level, Entity spawned, Entity mapped, FishingHook hook, ItemStack source, Player actor) {
        boolean accepted = level.addFreshEntity(spawned);
        if (spawned == mapped) NativeFishingConversionProof.accepted(mapped, hook, source, actor, accepted);
        return accepted;
    }
}
