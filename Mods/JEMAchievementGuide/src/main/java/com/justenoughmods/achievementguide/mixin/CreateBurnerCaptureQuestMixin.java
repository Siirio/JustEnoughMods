package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.burner.BlazeBurnerBlockItem", remap = false)
public abstract class CreateBurnerCaptureQuestMixin {
    @Unique private int jem$before;
    @Unique private int jem$emptyBefore;
    @Unique private int jem$count(Player actor) {
        int result = 0;
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
            ItemStack item = actor.getInventory().getItem(slot);
            if (CreateRunProofs.item(item, "create:blaze_burner")) result += item.getCount();
        }
        return result;
    }
    @Inject(method = "giveBurnerItemTo", at = @At("HEAD"), remap = false)
    private void jem$before(Player actor, ItemStack empty, InteractionHand hand, CallbackInfo callback) {
        jem$before = jem$count(actor); jem$emptyBefore = empty.getCount();
    }
    @Inject(method = "giveBurnerItemTo", at = @At("RETURN"), remap = false)
    private void jem$after(Player actor, ItemStack empty, InteractionHand hand, CallbackInfo callback) {
        if (actor instanceof ServerPlayer player && empty.getCount() == jem$emptyBefore - 1 && jem$count(actor) == jem$before + 1)
            QuestActionTrigger.INSTANCE.emit(player, "create_burner_capture");
    }
}
