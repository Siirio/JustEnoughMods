package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.primal.entity.animal.BearEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.BearEntity", remap = false)
public abstract class CreatureCareBearHealingMixin {
    @Shadow protected SimpleContainer inventory;
    @Unique private float jem$health;
    @Unique private List<ItemStack> jem$food = List.of();
    @Inject(method = "m_8024_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        BearEntity self = (BearEntity)(Object)this;
        jem$health = self.getHealth(); jem$food = List.of();
        var actor = CreatureCareProof.actor(self, "bear_food_owner");
        if (!CreatureCareProof.owned(self, actor) || !self.hasChest()) return;
        var snapshot = new ArrayList<ItemStack>();
        for (int i = 0; i < inventory.getContainerSize(); i++) snapshot.add(inventory.getItem(i).copy());
        jem$food = snapshot;
    }
    @Inject(method = "m_8024_", at = @At("RETURN"))
    private void jem$healed(CallbackInfo callback) {
        BearEntity self = (BearEntity)(Object)this;
        var actor = CreatureCareProof.actor(self, "bear_food_owner");
        if (!jem$food.isEmpty() && CreatureCareProof.owned(self, actor) && self.hasChest()
                && self.getHealth() > jem$health && self.getHealingCooldown() == 50) {
            int changed = 0; boolean food = false;
            for (int i = 0; i < jem$food.size(); i++) {
                ItemStack before = jem$food.get(i), after = inventory.getItem(i);
                if (!ItemStack.matches(before, after)) {
                    changed++;
                    food = BearEntity.isHealFood(before) && before.getFoodProperties(self) != null
                            && CreatureCareProof.debited(before, after);
                }
            }
            if (changed == 1 && food) CreatureCareProof.emit(actor, "v3_fauna_barrel_bear");
        }
        jem$food = List.of();
    }
}
