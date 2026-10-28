package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestPickup;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class WorldRitualQuestPickupMixin {
    @Inject(method="playerTouch",at=@At("HEAD"))private void jem$before(Player actor,CallbackInfo ci){WorldRitualQuestPickup.begin((ItemEntity)(Object)this,actor);}
    @Inject(method="playerTouch",at=@At("RETURN"))private void jem$after(Player actor,CallbackInfo ci){WorldRitualQuestPickup.end((ItemEntity)(Object)this,actor);}
}
