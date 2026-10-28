package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="slexom.animal_feeding_trough.platform.common.screen.FeedingTroughScreenHandler",remap=false)
public abstract class SpawnCareQuestTroughMenuMixin {
 @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V",at=@At("RETURN"),remap=false) private void jem$bind(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.Container container,CallbackInfo ci){SpawnCareQuestMenus.bind((net.minecraft.world.inventory.AbstractContainerMenu)(Object)this,container,null,"trough");}
}
