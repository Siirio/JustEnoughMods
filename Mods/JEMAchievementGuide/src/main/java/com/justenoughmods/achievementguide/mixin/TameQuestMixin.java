package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TameQuestMixin {
    @Unique private static final Set<String> JEM_EXOTIC = Set.of(
            "alexscaves:vallumraptor", "alexscaves:raycat",
            "unearthed_journey:leptictidium", "unearthed_journey:gnatusuchus");
    @Unique private boolean jem$unownedBefore;
    @Unique private boolean jem$babyBefore;

    @Inject(method = "tame", at = @At("HEAD"))
    private void jem$beforeTaming(Player player, CallbackInfo callback) {
        TamableAnimal animal = (TamableAnimal) (Object) this;
        jem$unownedBefore = !animal.isTame() && animal.getOwnerUUID() == null;
        jem$babyBefore = animal.isBaby();
    }

    @Inject(method = "tame", at = @At("TAIL"))
    private void jem$afterTaming(Player player, CallbackInfo callback) {
        TamableAnimal animal = (TamableAnimal) (Object) this;
        if (!(player instanceof ServerPlayer actor) || !jem$unownedBefore
                || !animal.isTame() || !animal.isOwnedBy(actor)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
        if (JEM_EXOTIC.contains(id)) QuestActionTrigger.INSTANCE.emit(actor, "exotic_bond");
        if (id.equals("cataclysm:modern_remnant") || id.equals("cataclysm:netherite_ministrosity")) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_small_titans");
        }
        if (jem$babyBefore && id.equals("blastfromthepast:speartooth")) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_era_winter_companion");
        }
        jem$unownedBefore = false;
    }
}
