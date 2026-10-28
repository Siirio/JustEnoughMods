package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class SpawnCareQuestMenuClickMixin {
 @Unique private SpawnCareQuestMenus.Run jem$run;@Unique private SpawnCareQuestMenus.Equipment jem$equipment;
 @Inject(method="clicked",at=@At("HEAD")) private void jem$before(int slot,int button,ClickType click,Player player,CallbackInfo ci){jem$equipment=player instanceof ServerPlayer actor?SpawnCareQuestMenus.equipment(actor):null;jem$run=player instanceof ServerPlayer actor?SpawnCareQuestMenus.begin((AbstractContainerMenu)(Object)this,actor):null;}
 @Inject(method="clicked",at=@At("RETURN")) private void jem$after(int slot,int button,ClickType click,Player player,CallbackInfo ci){var run=jem$run;jem$run=null;SpawnCareQuestMenus.end(run);SpawnCareQuestMenus.equipped(jem$equipment);jem$equipment=null;}
}
