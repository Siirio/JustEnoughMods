package com.siirio.jemsettlements.mixin;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge's BabyEntitySpawnEvent is animal-only; native villager birth must be observed here. */
@Mixin(VillagerMakeLove.class)
public abstract class VillagerBirthMixin {
    @Inject(method = "breed", at = @At("RETURN"))
    private void jem$registeredChild(ServerLevel level, Villager first, Villager second, CallbackInfoReturnable<Optional<Villager>> cir) {
        cir.getReturnValue().ifPresent(child -> com.siirio.jemsettlements.runtime.SettlementRuntime.registerBornChild(level, first, second, child));
    }
}
