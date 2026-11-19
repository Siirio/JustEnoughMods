package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.resident.VillagerBreedingHome;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge's BabyEntitySpawnEvent is animal-only; native villager birth must be observed here. */
@Mixin(VillagerMakeLove.class)
public abstract class VillagerBirthMixin {
    @Inject(method = "isBreedingPossible", at = @At("RETURN"), cancellable = true)
    private void jem$requireCompleteHomes(Villager villager, CallbackInfoReturnable<Boolean> cir) {
        // JEM owns one bounded family engine for villagers and Recruits. Keeping the vanilla
        // behavior active would create a second courtship loop and retain its spare-bed rule.
        cir.setReturnValue(false);
    }

    @Inject(method = "breed", at = @At("RETURN"))
    private void jem$registeredChild(ServerLevel level, Villager first, Villager second, CallbackInfoReturnable<Optional<Villager>> cir) {
        // Birth registration is performed by FamilyRuntime at the same authoritative spawn.
    }
}
