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

@Mixin(value={dev.xylonity.companions.common.tesla.behaviour.coil.CoilPulseBehaviour.class,dev.xylonity.companions.common.tesla.behaviour.pillar.PillarPulseBehaviour.class},remap=false)
public abstract class CompanionQuestRelayPulseMixin {
 @Redirect(method="process",at=@At(value="INVOKE",target="Ldev/xylonity/companions/common/blockentity/AbstractTeslaBlockEntity;startCycle()V"))private void jem$actualCurrent(dev.xylonity.companions.common.blockentity.AbstractTeslaBlockEntity target,dev.xylonity.companions.common.blockentity.AbstractTeslaBlockEntity source,Level world,BlockPos pos,BlockState state){CompanionQuestElectricity.relay(source,target);}
}
