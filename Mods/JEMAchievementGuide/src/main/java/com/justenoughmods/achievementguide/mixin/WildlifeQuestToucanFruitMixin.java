package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityToucan", remap = false)
public abstract class WildlifeQuestToucanFruitMixin {
    @Inject(method = "setSaplingState", at = @At("RETURN"))
    private void jem$fruit(BlockState state, CallbackInfo callback) {
        Mob self = (Mob)(Object)this; var actor = WildlifeQuestProof.actor(self, "worker_actor");
        if (actor != null && state != null && ItemStack.isSameItemSameTags(self.getMainHandItem(), ItemStack.of(WildlifeQuestProof.data(self).getCompound("worker_food")))) {
            WildlifeQuestProof.mark(self, "tree_actor", actor.getUUID()); WildlifeQuestProof.data(self).put("tree_state", NbtUtils.writeBlockState(state));
        }
    }
}
