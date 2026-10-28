package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.item.RawDoughItem;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RawDoughItem.class, remap = false)
public abstract class CookeryDoughQuestMixin {
    @Redirect(method = "m_5551_", at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/util/ItemUtils;getItemToLivingEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void jem$nativeNoodles(LivingEntity actor, ItemStack noodles, ItemStack dough, Level level, LivingEntity nativeActor, int timeLeft) {
        ItemStack expected = noodles.copy();
        boolean transformed = actor instanceof ServerPlayer && actor == nativeActor && dough.isEmpty()
                && FoodOutputProof.item(expected, "kaleidoscope_cookery:raw_noodles");
        int held = transformed ? FoodOutputProof.held((ServerPlayer)actor, ((ServerPlayer)actor).containerMenu, expected) : 0;
        ItemUtils.getItemToLivingEntity(actor, noodles);
        if (transformed && FoodOutputProof.held((ServerPlayer)actor, ((ServerPlayer)actor).containerMenu, expected) > held)
            QuestActionTrigger.INSTANCE.emit((ServerPlayer)actor, "food_dough_noodles");
    }
}
