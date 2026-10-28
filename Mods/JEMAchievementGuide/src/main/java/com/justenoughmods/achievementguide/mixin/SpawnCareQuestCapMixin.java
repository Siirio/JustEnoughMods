package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.*;import net.minecraft.world.level.Level;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ArmorItem.class) public abstract class SpawnCareQuestCapMixin {
 @Unique private SpawnCareQuestMenus.Equipment jem$equipment;
 @Inject(method="use",at=@At("HEAD")) private void jem$before(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){var actor=SpawnCareQuestProof.real(player);jem$equipment=actor==null?null:SpawnCareQuestMenus.equipment(actor);}
 @Inject(method="use",at=@At("RETURN")) private void jem$after(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){if(ci.getReturnValue().getResult().consumesAction())SpawnCareQuestMenus.equipped(jem$equipment);jem$equipment=null;}
}