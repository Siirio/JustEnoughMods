package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityKangaroo", remap = false)
public abstract class WildlifeQuestKangarooWorkMixin {
    @Unique private float jem$targetHealth;
    @Unique private ItemStack jem$weapon;
    @Unique private float jem$health;
    @Unique private float jem$hurtHealth;
    @Unique private List<ItemStack> jem$armor;
    @Unique private List<ItemStack> jem$food;
    @Inject(method = "m_7327_", at = @At("HEAD"))
    private void jem$attack(Entity target, CallbackInfoReturnable<Boolean> callback) {
        EntityKangaroo self = (EntityKangaroo)(Object)this; jem$targetHealth = target instanceof LivingEntity living ? living.getHealth() : -1; jem$weapon = self.getMainHandItem().copy();
    }
    @Inject(method = "m_7327_", at = @At("RETURN"))
    private void jem$attacked(Entity target, CallbackInfoReturnable<Boolean> callback) {
        EntityKangaroo self = (EntityKangaroo)(Object)this; var actor = WildlifeQuestProof.owner(self);
        if (callback.getReturnValue() && target instanceof LivingEntity living && living.getHealth() < jem$targetHealth && WildlifeQuestProof.hostile(living, actor) && WildlifeQuestCargo.supplied(self, jem$weapon)) WildlifeQuestProof.mark(self, "cargo_combat", actor.getUUID());
    }
    @Inject(method = "m_6469_", at = @At("HEAD"))
    private void jem$beforeHurt(net.minecraft.world.damagesource.DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        EntityKangaroo self = (EntityKangaroo)(Object)this; jem$hurtHealth = self.getHealth(); jem$armor = List.of(self.getItemBySlot(EquipmentSlot.HEAD).copy(), self.getItemBySlot(EquipmentSlot.CHEST).copy());
    }
    @Inject(method = "m_6469_", at = @At("RETURN"))
    private void jem$armored(net.minecraft.world.damagesource.DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        EntityKangaroo self = (EntityKangaroo)(Object)this; var actor = WildlifeQuestProof.owner(self);
        if (actor != null && callback.getReturnValue() && self.getHealth() < jem$hurtHealth && source.getEntity() instanceof LivingEntity hostile && WildlifeQuestProof.hostile(hostile, actor)
                && jem$armor.stream().anyMatch(stack -> !stack.isEmpty() && WildlifeQuestCargo.supplied(self, stack))) WildlifeQuestProof.mark(self, "cargo_combat", actor.getUUID());
    }
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        EntityKangaroo self = (EntityKangaroo)(Object)this; jem$health = self.getHealth(); jem$food = new ArrayList<>();
        for (int i = 0; i < self.kangarooInventory.getContainerSize(); i++) jem$food.add(self.kangarooInventory.getItem(i).copy());
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void jem$after(CallbackInfo callback) {
        EntityKangaroo self = (EntityKangaroo)(Object)this; var actor = WildlifeQuestProof.owner(self);
        if (actor == null || self.getHealth() <= jem$health || !WildlifeQuestProof.matches(self, "cargo_combat", actor.getUUID())) return;
        for (int i = 0; i < jem$food.size(); i++) {
            ItemStack before = jem$food.get(i), after = self.kangarooInventory.getItem(i);
            if (!before.isEmpty() && before.getItem().isEdible() && before.getItem().getFoodProperties() != null && !before.getItem().getFoodProperties().isMeat()
                    && WildlifeQuestProof.debited(before, after, 1) && WildlifeQuestCargo.supplied(self, before)
                    && NativeFoodSources.get(actor.serverLevel()).consume(actor.getUUID(), NativeFoodSources.spent(before, 1), "wildlife_kangaroo_cargo", 1)) WildlifeQuestProof.emit(actor, "v3_fauna_cargo_and_equipment");
        }
    }
}
