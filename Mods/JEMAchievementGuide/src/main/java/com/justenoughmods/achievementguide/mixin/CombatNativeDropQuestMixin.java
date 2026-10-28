package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatDropProof;
import com.justenoughmods.achievementguide.integration.combattools.CombatCampfireProof;
import com.justenoughmods.achievementguide.integration.food.NativeCampfireGameProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Containers.class)
public abstract class CombatNativeDropQuestMixin {
    // Observe the final public drop input without competing with another campfire caller's redirect.
    @Inject(method = "dropItemStack", at = @At("HEAD"))
    private static void jem$campfireOutput(Level level, double x, double y, double z, ItemStack source, CallbackInfo callback) {
        if (level instanceof ServerLevel) NativeCampfireGameProof.output(source);
    }
    @Redirect(method = "dropItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$accepted(Level level, Entity item, Level sourceLevel, double x, double y, double z, ItemStack source) {
        boolean accepted = level.addFreshEntity(item); CombatDropProof.spawned(item, accepted); CombatCampfireProof.spawned(source, item, accepted); return accepted;
    }
}
