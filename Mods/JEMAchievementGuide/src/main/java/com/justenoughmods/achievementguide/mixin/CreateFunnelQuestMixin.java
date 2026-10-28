package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateBeltTransfers;
import com.justenoughmods.achievementguide.integration.create.CreateFunnelRun;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.funnel.FunnelBlockEntity", remap = false)
public abstract class CreateFunnelQuestMixin implements CreateFunnelRun {
    @Unique private static final String JEM_FILTER = "jemQuestFunnelFilterV1";
    @Unique private static final String JEM_BELT = "jemQuestFunnelBeltV1";
    @Shadow private FilteringBehaviour filtering;

    @Unique private boolean jem$eligible(ServerPlayer actor) {
        return !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild;
    }
    @Override public void jem$filterConfiguration(ServerPlayer actor, CompoundTag configuration) {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(JEM_FILTER);
        if (jem$eligible(actor) && !filtering.getFilter().isEmpty()) {
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", actor.getUUID());
            proof.put("configuration", configuration.copy());
            owner.getPersistentData().put(JEM_FILTER, proof);
        }
        owner.setChanged();
    }
    @Override public void jem$beltConfiguration(ServerPlayer actor) {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(JEM_BELT);
        if (jem$eligible(actor)) {
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", actor.getUUID());
            proof.putString("configuration", owner.getBlockState().toString());
            owner.getPersistentData().put(JEM_BELT, proof);
        }
        owner.setChanged();
    }
    @Unique private void jem$filterAttempt(ItemStack item, boolean accepted) {
        BlockEntity owner = (BlockEntity)(Object)this;
        CompoundTag proof = owner.getPersistentData().getCompound(JEM_FILTER);
        if (!proof.hasUUID("actor") || item.isEmpty() || filtering.getFilter().isEmpty()) return;
        CompoundTag actual = new CompoundTag();
        filtering.write(actual, false);
        if (!actual.equals(proof.getCompound("configuration"))) {
            owner.getPersistentData().remove(JEM_FILTER);
            owner.setChanged();
            return;
        }
        proof.putString(accepted ? "accepted" : "rejected", String.valueOf(ForgeRegistries.ITEMS.getKey(item.getItem())));
        if (proof.contains("accepted") && proof.contains("rejected")
                && !proof.getString("accepted").equals(proof.getString("rejected")) && owner.getLevel() instanceof ServerLevel level) {
            CreateRunProofs.emit(level, proof.getUUID("actor"), "create_filtered_funnel");
            owner.getPersistentData().remove(JEM_FILTER);
        }
        owner.setChanged();
    }
    @Redirect(method = "handleDirectBeltInput", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/filtering/FilteringBehaviour;test(Lnet/minecraft/world/item/ItemStack;)Z"), remap = false)
    private boolean jem$nativeFilterRejected(FilteringBehaviour filter, ItemStack input, TransportedItemStack transported,
                                            Direction side, boolean simulate) {
        boolean accepted = filter.test(input);
        if (!simulate && !accepted) jem$filterAttempt(input, false);
        return accepted;
    }
    @Redirect(method = "handleDirectBeltInput", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/inventory/InvManipulationBehaviour;insert(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$nativeInsert(InvManipulationBehaviour inventory, ItemStack input, TransportedItemStack transported,
                                       Direction side, boolean simulate) {
        int before = input.getCount();
        ItemStack evidence = input.copy();
        ItemStack remainder = inventory.insert(input);
        if (!simulate && remainder.getCount() < before) {
            jem$filterAttempt(evidence, true);
            BlockEntity owner = (BlockEntity)(Object)this;
            CompoundTag proof = owner.getPersistentData().getCompound(JEM_BELT);
            String block = String.valueOf(ForgeRegistries.BLOCKS.getKey(owner.getBlockState().getBlock()));
            if (proof.hasUUID("actor") && proof.getString("configuration").equals(owner.getBlockState().toString())
                    && (block.equals("create:andesite_funnel") || block.equals("create:andesite_belt_funnel"))
                    && CreateBeltTransfers.poweredInput(transported) && owner.getLevel() instanceof ServerLevel level) {
                CreateRunProofs.emit(level, proof.getUUID("actor"), "create_belt_delivery");
                owner.getPersistentData().remove(JEM_BELT);
                owner.setChanged();
            }
        }
        return remainder;
    }
}
