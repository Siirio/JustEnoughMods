package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareMusic;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareOutputs;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.primal.entity.animal.SnakeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.SnakeEntity", remap = false)
public abstract class CreatureCareSnakeMixin {
    @Unique private ItemStack jem$before = ItemStack.EMPTY;
    @Unique private ItemStack jem$potion = ItemStack.EMPTY;
    @Unique private boolean jem$wild, jem$dance;
    @Unique private int jem$received;
    @Unique private CreatureCareOutputs.Run jem$run;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        SnakeEntity self = (SnakeEntity)(Object)this;
        jem$before = player.getItemInHand(hand).copy(); jem$wild = !self.isTame();
        jem$dance = self.isDancing() && CreatureCareMusic.active(self); jem$received = 0;
        jem$potion = new ItemStack(Items.POTION); PotionUtils.setPotion(jem$potion, self.getSnakeEffect().getHarvestablePotion());
        jem$run = CreatureCareOutputs.begin(self, CreatureCareProof.real(player));
    }
    @Redirect(method = "m_6071_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;m_36054_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$received(Inventory inventory, ItemStack output) {
        int before = output.getCount(); boolean expected = ItemStack.isSameItemSameTags(jem$potion, output);
        boolean result = inventory.add(output);
        if (expected) jem$received += before - output.getCount();
        return result;
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        SnakeEntity self = (SnakeEntity)(Object)this;
        var run = jem$run; jem$run = null; CreatureCareOutputs.end(run);
        if (run != null && callback.getReturnValue().consumesAction() && CreatureCareProof.debited(jem$before, player.getItemInHand(hand))) {
            if (jem$wild && jem$dance && jem$before.is(Items.RABBIT) && CreatureCareProof.owned(self, run.actor)) {
                CreatureCareProof.mark(self, "snake_music_owner", run.actor.getUUID());
                CreatureCareProof.emit(run.actor, "v3_fauna_musical_snake");
            }
            if (jem$before.is(Items.GLASS_BOTTLE) && self.getPotionCooldown() == 600 && CreatureCareProof.owned(self, run.actor)
                    && CreatureCareProof.matches(self, "snake_music_owner", run.actor.getUUID())) {
                if (jem$received == 1) CreatureCareProof.emit(run.actor, "v3_fauna_musical_snake_bottle");
                for (var drop : run.drops) if (ItemStack.isSameItemSameTags(jem$potion, drop.getItem()) && drop.getItem().getCount() == 1) {
                    CreatureCareOutputs.collected(run, drop, "v3_fauna_musical_snake_bottle");
                }
            }
        }
        jem$before = ItemStack.EMPTY; jem$potion = ItemStack.EMPTY;
    }
}
