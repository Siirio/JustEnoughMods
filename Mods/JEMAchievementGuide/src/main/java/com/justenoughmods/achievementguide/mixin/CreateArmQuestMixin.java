package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateArmRun;
import com.justenoughmods.achievementguide.integration.create.CreateCargoTransfers;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity", remap = false)
public abstract class CreateArmQuestMixin implements CreateArmRun {
    @Unique private static final String JEM_ARM = "jemQuestArmConfigurationV1";
    @Shadow ListTag interactionPointTag;
    @Shadow public abstract void writeInteractionPoints(CompoundTag tag);

    @Override public void jem$configured(ServerPlayer actor, ListTag committedPoints) {
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || committedPoints == null || interactionPointTag != committedPoints) return;
        BlockEntity owner = (BlockEntity)(Object)this;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor.getUUID());
        proof.putBoolean("pending_configuration", true);
        owner.getPersistentData().put(JEM_ARM, proof);
        owner.setChanged();
    }

    @Inject(method = "initInteractionPoints", at = @At("TAIL"), remap = false)
    private void jem$configurationResolved(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        CompoundTag proof = owner.getPersistentData().getCompound(JEM_ARM);
        if (!proof.hasUUID("actor")) return;
        CompoundTag actual = new CompoundTag();
        writeInteractionPoints(actual);
        if (proof.getBoolean("pending_configuration")) {
            proof.put("configuration", actual);
            proof.putBoolean("pending_configuration", false);
        } else if (!actual.equals(proof.getCompound("configuration"))) {
            owner.getPersistentData().remove(JEM_ARM);
        }
        owner.setChanged();
    }

    @Redirect(method = "collectItem", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;extract(IIZ)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$collected(ArmInteractionPoint point, int slot, int amount, boolean simulate) {
        ItemStack extracted = point.extract(slot, amount, simulate);
        BlockEntity owner = (BlockEntity)(Object)this;
        CompoundTag proof = owner.getPersistentData().getCompound(JEM_ARM);
        if (!simulate && !extracted.isEmpty() && proof.hasUUID("actor") && !proof.getBoolean("pending_configuration")) {
            proof.putLong("source", point.getPos().asLong());
            proof.putString("held", CreateCargoTransfers.fingerprint(extracted));
            owner.setChanged();
        }
        return extracted;
    }

    @Redirect(method = "depositItem", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;insert(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$deposited(ArmInteractionPoint point, ItemStack input, boolean simulate) {
        int before = input.getCount();
        String item = CreateCargoTransfers.fingerprint(input);
        ItemStack remainder = point.insert(input, simulate);
        BlockEntity owner = (BlockEntity)(Object)this;
        CompoundTag proof = owner.getPersistentData().getCompound(JEM_ARM);
        if (!simulate && remainder.getCount() < before && proof.hasUUID("actor") && proof.contains("source")
                && proof.getLong("source") != point.getPos().asLong() && item.equals(proof.getString("held"))
                && owner.getLevel() instanceof ServerLevel level) {
            CreateRunProofs.emit(level, proof.getUUID("actor"), "create_arm_transfer");
            proof.remove("source");
            proof.remove("held");
            owner.setChanged();
        }
        return remainder;
    }
}
