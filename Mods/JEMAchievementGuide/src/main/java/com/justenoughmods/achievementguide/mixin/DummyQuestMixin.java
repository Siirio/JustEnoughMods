package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.dummmmmmy.common.TargetDummyEntity", remap = false)
public abstract class DummyQuestMixin {
    @Inject(method = "onActuallyDamagedOrTrueDamageDetected", at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/dummmmmmy/common/TargetDummyEntity;updateTargetBlock(F)V", shift = At.Shift.AFTER))
    private void jem$nativeDummyDamage(float amount, DamageSource source, CallbackInfo callback) {
        if (!Float.isFinite(amount) || amount <= 0 || source == null
                || !(source.getEntity() instanceof ServerPlayer actor) || actor.isSpectator()
                || actor.getAbilities().instabuild || actor instanceof net.minecraftforge.common.util.FakePlayer) return;
        LivingEntity dummy = (LivingEntity) (Object) this;
        ItemStack chest = dummy.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag proof = actor.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (chest.isEmpty()) {
            proof.putUUID("JemQuestBareDummyV4", dummy.getUUID());
        } else if (chest.getItem() instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.CHESTPLATE
                && proof.hasUUID("JemQuestBareDummyV4")
                && proof.getUUID("JemQuestBareDummyV4").equals(dummy.getUUID())) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_dummy_workshop");
            proof.remove("JemQuestBareDummyV4");
        }
        actor.getPersistentData().put(Player.PERSISTED_NBT_TAG, proof);
    }
}
