package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBreed;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class WildlifeQuestFroststalkerBreedMixin {
    @Unique private ItemStack jem$pork = ItemStack.EMPTY;
    @Unique private WildlifeQuestBreed jem$breed;
    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void jem$feedBefore(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$pork = WildlifeQuestProof.type((Animal)(Object)this).equals("alexsmobs:froststalker") ? player.getItemInHand(hand).copy() : ItemStack.EMPTY;
    }
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void jem$fed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        Animal self = (Animal)(Object)this; var actor = WildlifeQuestProof.real(player);
        if (actor != null && jem$pork.is(Items.PORKCHOP) && self.isInLove() && self.getLoveCause() == actor
                && WildlifeQuestProof.debited(jem$pork, player.getItemInHand(hand), 1)) WildlifeQuestProof.mark(self, "frost_pork_actor", actor.getUUID());
        jem$pork = ItemStack.EMPTY;
    }
    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"))
    private void jem$before(ServerLevel level, Animal other, CallbackInfo callback) { jem$breed = null; }
    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
    private void jem$nativeCredit(ServerLevel level, Animal other, AgeableMob child, CallbackInfo callback) {
        Animal self = (Animal)(Object)this;
        var actor = WildlifeQuestProof.real(self.getLoveCause() != null ? self.getLoveCause() : other.getLoveCause());
        if (actor != null && WildlifeQuestProof.type(self).equals("alexsmobs:froststalker") && WildlifeQuestProof.type(other).equals("alexsmobs:froststalker")
                && WildlifeQuestProof.type(child).equals("alexsmobs:froststalker") && child.isBaby()
                && (WildlifeQuestProof.matches(self, "frost_pork_actor", actor.getUUID()) || WildlifeQuestProof.matches(other, "frost_pork_actor", actor.getUUID()))
                && WildlifeQuestProof.item(actor.getItemBySlot(EquipmentSlot.HEAD)).equals("alexsmobs:froststalker_helmet")) jem$breed = new WildlifeQuestBreed(actor, child);
    }
    @Inject(method = "spawnChildFromBreeding", at = @At("RETURN"))
    private void jem$born(ServerLevel level, Animal other, CallbackInfo callback) {
        var run = jem$breed; jem$breed = null;
        if (run != null && level.getEntity(run.child().getUUID()) == run.child() && run.child().isBaby()) WildlifeQuestProof.emit(run.actor(), "v3_fauna_cold_pack");
    }
}
