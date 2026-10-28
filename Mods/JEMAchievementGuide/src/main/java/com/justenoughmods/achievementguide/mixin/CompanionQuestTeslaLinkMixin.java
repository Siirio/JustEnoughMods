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

@Mixin(value=dev.xylonity.companions.common.item.WrenchItem.class,remap=false)
public abstract class CompanionQuestTeslaLinkMixin {
 @Inject(method="createConnection",at=@At("RETURN"))private void jem$connected(Player p,dev.xylonity.companions.common.tesla.ConnectionTarget source,dev.xylonity.companions.common.tesla.ConnectionTarget target,net.minecraft.world.item.context.UseOnContext context,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue())CompanionQuestElectricity.connected(p,source,target);}
}
