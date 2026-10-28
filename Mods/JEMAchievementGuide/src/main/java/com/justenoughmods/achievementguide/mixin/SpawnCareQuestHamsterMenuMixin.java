package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.ninni.spawn.client.inventory.HamsterInventoryMenu",remap=false)
public abstract class SpawnCareQuestHamsterMenuMixin {
 @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lcom/ninni/spawn/server/entity/mob/Hamster;)V",at=@At("RETURN"),remap=false) private void jem$bind(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.Container container,com.ninni.spawn.server.entity.mob.Hamster hamster,CallbackInfo ci){SpawnCareQuestMenus.bind((net.minecraft.world.inventory.AbstractContainerMenu)(Object)this,container,hamster,"hamster");}
}
