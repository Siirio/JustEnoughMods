package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.petutilities.PetUtilityQuestHay;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the actual native query result only inside the registered Hay protection callback. */
@Mixin(Level.class)
public abstract class PetUtilityQuestGolemEventsAccess {
    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"))
    private <T extends Entity> void jem$nativeHayRange(EntityTypeTest<Entity, T> type, AABB area, Predicate<? super T> predicate,
                                                    CallbackInfoReturnable<List<T>> callback) {
        PetUtilityQuestHay.found(callback.getReturnValue());
    }
}
