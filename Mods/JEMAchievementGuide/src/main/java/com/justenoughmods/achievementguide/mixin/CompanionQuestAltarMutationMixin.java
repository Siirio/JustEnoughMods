package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value=dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity.class,remap=false)
public abstract class CompanionQuestAltarMutationMixin {
 @Inject(method={"setCharges","clearCharges"},at=@At("HEAD"))private void jem$invalidate(CallbackInfo ci){((dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity)(Object)this).getPersistentData().remove(CompanionQuestShade.CHARGE);}
 @Inject(method="consumeCharge",at=@At("HEAD"))private void jem$spent(CallbackInfoReturnable<Boolean> ci){((dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity)(Object)this).getPersistentData().remove(CompanionQuestShade.CHARGE);}
}
