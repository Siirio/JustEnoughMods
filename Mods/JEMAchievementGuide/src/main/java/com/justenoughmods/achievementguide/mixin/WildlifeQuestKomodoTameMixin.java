package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon", remap = false)
public abstract class WildlifeQuestKomodoTameMixin {
    @Unique private ItemStack jem$offering = ItemStack.EMPTY;
    @Unique private boolean jem$wild;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { jem$offering = player.getItemInHand(hand).copy(); jem$wild = !((TamableAnimal)(Object)this).isTame(); }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$accepted(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var actor = WildlifeQuestProof.real(player); var self = (TamableAnimal)(Object)this;
        if (actor != null && jem$wild && jem$offering.is(AMTagRegistry.KOMODO_DRAGON_TAMEABLES) && self.isTame() && actor.getUUID().equals(self.getOwnerUUID())
                && callback.getReturnValue().consumesAction() && WildlifeQuestProof.debited(jem$offering, player.getItemInHand(hand), jem$offering.getCount())) WildlifeQuestProof.mark(self, "komodo_native_owner", actor.getUUID());
        jem$offering = ItemStack.EMPTY;
    }
}
