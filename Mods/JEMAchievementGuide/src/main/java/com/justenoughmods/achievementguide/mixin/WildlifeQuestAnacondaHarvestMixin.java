package com.justenoughmods.achievementguide.mixin;

import com.github.alexthe666.alexsmobs.entity.EntityAnaconda;
import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityAnaconda", remap = false)
public abstract class WildlifeQuestAnacondaHarvestMixin {
    @Unique private ItemStack jem$food = ItemStack.EMPTY;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$feed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EntityAnaconda self = (EntityAnaconda)(Object)this;
        jem$food = WildlifeQuestProof.real(player) != null && self.isFood(player.getItemInHand(hand)) ? player.getItemInHand(hand).copy() : ItemStack.EMPTY;
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$fed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (!jem$food.isEmpty() && callback.getReturnValue().consumesAction() && WildlifeQuestProof.debited(jem$food, player.getItemInHand(hand), 1)) WildlifeQuestProof.mark((EntityAnaconda)(Object)this, "shed_feeder", player.getUUID());
    }
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityAnaconda;spawnItemAtOffset(Lnet/minecraft/world/item/ItemStack;FF)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$shed(EntityAnaconda self, ItemStack output, float offset, float height) {
        ItemEntity drop = self.spawnItemAtOffset(output, offset, height); var actor = WildlifeQuestProof.actor(self, "shed_feeder");
        if (actor != null && drop != null && WildlifeQuestProof.item(output).equals("alexsmobs:shed_snake_skin") && self.getSheddingTime() == 0)
            FoodDroppedProof.update(drop, "v3_fauna_renewable_harvest_skin", drop.getItem().getCount(), actor.getUUID());
        return drop;
    }
}
