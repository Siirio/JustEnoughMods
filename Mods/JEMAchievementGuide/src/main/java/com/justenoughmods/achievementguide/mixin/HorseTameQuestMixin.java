package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class HorseTameQuestMixin {
    @Unique private boolean jem$eligibleBefore;

    @Inject(method = "tameWithName", at = @At("HEAD"))
    private void jem$beforeTaming(Player player, CallbackInfoReturnable<Boolean> callback) {
        AbstractHorse animal = (AbstractHorse) (Object) this;
        jem$eligibleBefore = animal.isBaby() && !animal.isTamed() && animal.getOwnerUUID() == null
                && BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString().equals("blastfromthepast:frostomper");
    }

    @Inject(method = "tameWithName", at = @At("RETURN"))
    private void jem$afterTaming(Player player, CallbackInfoReturnable<Boolean> callback) {
        AbstractHorse animal = (AbstractHorse) (Object) this;
        if (player instanceof ServerPlayer actor && jem$eligibleBefore && Boolean.TRUE.equals(callback.getReturnValue())
                && animal.isTamed() && actor.getUUID().equals(animal.getOwnerUUID())) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_era_winter_companion");
        }
        jem$eligibleBefore = false;
    }
}
