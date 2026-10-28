package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import com.justenoughmods.achievementguide.integration.CaveResearchProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.inventory.SpelunkeryTableMenu$5", remap = false)
public abstract class SpelunkeryResultQuestMixin {
    @Inject(method = "m_142406_", at = @At("TAIL"))
    private void jem$codexWithdrawn(Player player, ItemStack result, CallbackInfo callback) {
        if (player instanceof ServerPlayer actor && actor.containerMenu instanceof QuestResultSnapshot snapshot
                && actor.containerMenu.getSlot(2) == (Object) this && actor.containerMenu.stillValid(actor)) {
            CaveResearchProof.withdrewCodex(actor, result.isEmpty() ? snapshot.jem$questQuickResult() : result);
        }
    }
}
