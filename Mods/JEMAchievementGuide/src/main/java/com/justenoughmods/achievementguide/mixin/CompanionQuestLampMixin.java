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

@Mixin(value=dev.xylonity.companions.common.tesla.behaviour.lamp.LampPulseBehaviour.class,remap=false)
public abstract class CompanionQuestLampMixin {
 @Inject(method="process",at=@At("RETURN"))private void jem$powered(dev.xylonity.companions.common.blockentity.AbstractTeslaBlockEntity lamp,Level world,BlockPos pos,BlockState state,CallbackInfo ci){if(!world.isClientSide)CompanionQuestElectricity.lamp(lamp);}
}
