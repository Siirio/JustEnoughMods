package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe", remap = false)
public abstract class CreateSequenceAdvanceQuestMixin {
    @Shadow protected ResourceLocation id;
    @Shadow protected List<SequencedRecipe<?>> sequence;
    @Shadow protected int loops;
    @Shadow private int getStep(ItemStack item) { throw new AssertionError(); }

    @Inject(method = "advance", at = @At("RETURN"), remap = false)
    private void jem$nativeSequenceStep(ItemStack input, CallbackInfoReturnable<ItemStack> callback) {
        int step = getStep(input);
        if (!sequence.isEmpty()) CreateTransportInputs.advanced(input, callback.getReturnValue(), id.toString(), step,
                (step + 1) / sequence.size() >= loops);
    }
}
