package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.procedures.QuakeRightClickedOnEntityProcedure", remap = false)
public abstract class SkarrierQuakeMountQuestMixin {
    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$mounted(LevelAccessor world, Entity mount, Entity actor, ItemStack item, CallbackInfo callback) {
        if (mount != null && actor != null && actor.level() == world && actor.getVehicle() == mount) SkarrierWorkshopProof.quakeMounted(mount, actor);
    }
}
