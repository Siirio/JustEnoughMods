package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.EntityBison", "com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle"}, remap = false)
public abstract class WildlifeQuestShearingMixin {
    @Unique private boolean jem$eligible;
    @Inject(method = "onSheared", at = @At("HEAD"))
    private void jem$before(Player player, ItemStack tool, Level world, BlockPos pos, int fortune, CallbackInfoReturnable<List<ItemStack>> callback) {
        Entity self = (Entity)(Object)this;
        jem$eligible = self instanceof com.github.alexthe666.alexsmobs.entity.EntityBison b ? !b.isSheared() && !b.isBaby()
                : self instanceof com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle t && t.getMoss() > 0;
    }
    @Inject(method = "onSheared", at = @At("RETURN"))
    private void jem$after(Player player, ItemStack tool, Level world, BlockPos pos, int fortune, CallbackInfoReturnable<List<ItemStack>> callback) {
        var actor = WildlifeQuestProof.real(player); if (!jem$eligible || actor == null || !(world instanceof ServerLevel level)) return;
        boolean bison = (Object)this instanceof com.github.alexthe666.alexsmobs.entity.EntityBison;
        String expected = bison ? "alexsmobs:bison_fur" : "alexsmobs:spiked_scute";
        for (var output : callback.getReturnValue()) if (WildlifeQuestProof.item(output).equals(expected))
            NativeFoodSources.get(level).certify(level, actor.getUUID(), bison ? "wildlife_fur" : "wildlife_scute", output, output.getCount());
    }
}
