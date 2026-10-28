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

@Mixin(value=dev.xylonity.companions.common.tesla.behaviour.dinamo.DinamoPulseBehaviour.class,remap=false)
public abstract class CompanionQuestGeneratorPulseMixin {
 @Redirect(method="tick",at=@At(value="INVOKE",target="Ldev/xylonity/companions/common/blockentity/AbstractTeslaBlockEntity;startCycle()V"))private void jem$actualCurrent(dev.xylonity.companions.common.blockentity.AbstractTeslaBlockEntity target,dev.xylonity.companions.common.entity.companion.DinamoEntity source){CompanionQuestElectricity.generator(source,target);}
}
