package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.ninni.spawn.client.inventory.PigmentShifterMenu",remap=false)
public abstract class SpawnCareQuestPigmentMenuMixin {
 @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("RETURN"),remap=false) private void jem$bind(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.inventory.ContainerLevelAccess access,CallbackInfo ci){SpawnCareQuestMenus.bind((net.minecraft.world.inventory.AbstractContainerMenu)(Object)this,((com.ninni.spawn.client.inventory.PigmentShifterMenu)(Object)this).inputContainer,null,"pigment");}
}
