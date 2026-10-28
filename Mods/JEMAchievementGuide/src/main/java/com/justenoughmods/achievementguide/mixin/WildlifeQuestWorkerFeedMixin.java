package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey;
import com.github.alexthe666.alexsmobs.entity.EntityPlatypus;
import com.github.alexthe666.alexsmobs.entity.EntityToucan;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey", "com.github.alexthe666.alexsmobs.entity.EntityPlatypus", "com.github.alexthe666.alexsmobs.entity.EntityToucan"}, remap = false)
public abstract class WildlifeQuestWorkerFeedMixin {
    @Unique private ItemStack jem$food = ItemStack.EMPTY;
    @Unique private boolean jem$eligible;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$food = player.getItemInHand(hand).copy(); Object self = this;
        jem$eligible = self instanceof EntityGeladaMonkey monkey && monkey.getClearGrassTime() == 0 && jem$food.is(AMTagRegistry.GELADA_MONKEY_LAND_CLEARING_FOODS)
                || self instanceof EntityPlatypus platypus && !platypus.isSensing() && jem$food.is(AMTagRegistry.PLATYPUS_CHARGEABLES)
                || self instanceof EntityToucan toucan && toucan.canTargetItem(jem$food) && toucan.getMainHandItem().isEmpty();
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$accepted(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var actor = WildlifeQuestProof.real(player); Entity self = (Entity)(Object)this;
        boolean committed = (Object)this instanceof EntityGeladaMonkey monkey && monkey.getClearGrassTime() > 0
                || (Object)this instanceof EntityPlatypus platypus && platypus.isSensing()
                || (Object)this instanceof EntityToucan toucan && ItemStack.isSameItemSameTags(toucan.getMainHandItem(), jem$food);
        if (actor != null && jem$eligible && committed && callback.getReturnValue().consumesAction() && WildlifeQuestProof.debited(jem$food, player.getItemInHand(hand), 1)) {
            WildlifeQuestProof.mark(self, "worker_actor", actor.getUUID());
            WildlifeQuestProof.data(self).put("worker_food", jem$food.copyWithCount(1).save(new CompoundTag()));
        }
        jem$food = ItemStack.EMPTY;
    }
}
