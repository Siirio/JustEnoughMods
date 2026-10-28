package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.*;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.EntityElephant", "com.github.alexthe666.alexsmobs.entity.EntityKangaroo"}, remap = false)
public abstract class WildlifeQuestCargoOpenMixin {
    @Inject(method = "openGUI", at = @At("RETURN"))
    private void jem$open(Player player, CallbackInfo callback) {
        if (!(player instanceof ServerPlayer actor)) return;
        if ((Object)this instanceof EntityElephant elephant && elephant.isChested()) WildlifeQuestCargo.opened(elephant, actor, elephant.elephantInventory);
        if ((Object)this instanceof EntityKangaroo kangaroo) WildlifeQuestCargo.opened(kangaroo, actor, kangaroo.kangarooInventory);
    }
}
