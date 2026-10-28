package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatCorpseProof;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.github.teamfusion.rottencreatures.common.level.blockentities.TreasureChestBlockEntity", remap = false)
public abstract class CombatTreasureReplacementQuestMixin {
    @Inject(method = "addItemsWithReplacement", at = @At("HEAD"))
    private void jem$invalidate(ItemStack item, int count, CallbackInfoReturnable<NonNullList<ItemStack>> callback) {
        ((BlockEntity)(Object)this).getPersistentData().remove(CombatCorpseProof.CHEST);
    }
}
