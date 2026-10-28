package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.HummingbirdAIPollinate", remap = false)
public abstract class WildlifeQuestHummingbirdPollinationMixin {
    @Shadow @Final private EntityHummingbird bird;
    @Redirect(method = "pollinate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BoneMealItem;m_40627_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean jem$grew(ItemStack stack, Level level, BlockPos pos) {
        var before = level.getBlockState(pos); boolean accepted = BoneMealItem.growCrop(stack, level, pos); var after = level.getBlockState(pos);
        var actor = WildlifeQuestProof.actor(bird, "pollinator_actor");
        if (actor != null && accepted && before.getBlock() instanceof CropBlock crop && after.is(crop) && crop.getAge(after) > crop.getAge(before)) WildlifeQuestProof.emit(actor, "v3_fauna_pollinators");
        return accepted;
    }
}
