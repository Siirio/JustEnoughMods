package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateSpoutProofs;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.equipment.wrench.WrenchItem", remap = false)
public abstract class CreatePumpConfigurationQuestMixin {
    @Redirect(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/equipment/wrench/IWrenchable;onWrenched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"), remap = false)
    private InteractionResult jem$actualPumpConfiguration(IWrenchable block, BlockState before, UseOnContext context) {
        InteractionResult result = block.onWrenched(before, context);
        CreateSpoutProofs.configured(before, context, result);
        return result;
    }
}
