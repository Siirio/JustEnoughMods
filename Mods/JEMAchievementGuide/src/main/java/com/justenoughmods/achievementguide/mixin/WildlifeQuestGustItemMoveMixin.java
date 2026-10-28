package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestGust;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class WildlifeQuestGustItemMoveMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$moved(CallbackInfo callback) { WildlifeQuestGust.moved((ItemEntity)(Object)this); }
}
