package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeDollProof;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopedoll.block.DollGiftBoxBlock", remap = false)
public abstract class HomeDollLootQuestMixin {
    @Inject(method = "m_49635_", at = @At("RETURN"))
    private void jem$nativeDoll(BlockState state, LootParams.Builder parameters, CallbackInfoReturnable<List<ItemStack>> callback) {
        HomeDollProof.drops(parameters, callback.getReturnValue());
    }
}
