package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateSpoutProofs;
import com.justenoughmods.achievementguide.integration.create.CreateFilmProofs;
import com.justenoughmods.achievementguide.integration.create.CreateSpoutRun;
import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.justenoughmods.achievementguide.integration.create.CreateTransportProof;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.fluids.spout.SpoutBlockEntity", remap = false)
public abstract class CreateSpoutQuestMixin implements CreateSpoutRun {
    @Shadow SmartFluidTankBehaviour tank;
    @Unique private UUID jem$fillingActor;
    @Unique private boolean jem$film;
    @Unique private FluidStack jem$expected = FluidStack.EMPTY;
    @Unique private ItemStack jem$result = ItemStack.EMPTY;
    @Unique private List<TransportedItemStack> jem$outputs = List.of();

    @Unique private FluidStack jem$fluid() { return tank.getPrimaryHandler().getFluid(); }
    @Unique private void jem$invalidate() {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(CreateSpoutProofs.FLUID);
        owner.setChanged();
    }
    @Inject(method = "addBehaviours", at = @At("TAIL"), remap = false)
    private void jem$observeNativeTank(CallbackInfo callback) {
        // In6.0.8 this spout's callback is the native empty lambda. updateFluids still performs native sync/dirty work.
        tank.whenFluidUpdates(this::jem$invalidate);
    }
    @Override public CompoundTag jem$fluidProof() {
        CompoundTag proof = ((BlockEntity)(Object)this).getPersistentData().getCompound(CreateSpoutProofs.FLUID);
        return proof.hasUUID("actor") && CreateSpoutProofs.snapshot(jem$fluid()).equals(proof.getCompound("fluid")) ? proof.copy() : new CompoundTag();
    }
    @Unique private void jem$bind(UUID actor) {
        BlockEntity owner = (BlockEntity)(Object)this;
        jem$invalidate();
        if (actor != null && !jem$fluid().isEmpty()) {
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", actor);
            proof.put("fluid", CreateSpoutProofs.snapshot(jem$fluid()));
            owner.getPersistentData().put(CreateSpoutProofs.FLUID, proof);
            owner.setChanged();
        }
    }
    @Override public void jem$pumped(UUID actor, FluidStack before, FluidStack supplied, int accepted, CompoundTag prior) {
        jem$invalidate();
        if (actor == null || accepted <= 0 || !before.isEmpty() && (!prior.hasUUID("actor") || !actor.equals(prior.getUUID("actor"))
                || !CreateSpoutProofs.snapshot(before).equals(prior.getCompound("fluid")))) return;
        FluidStack expected = before.isEmpty() ? supplied.copy() : before.copy();
        expected.setAmount(before.getAmount() + accepted);
        if (CreateSpoutProofs.snapshot(expected).equals(CreateSpoutProofs.snapshot(jem$fluid()))) jem$bind(actor);
    }
    @Inject(method = "whenItemHeld", at = @At("HEAD"), remap = false)
    private void jem$beginFill(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler, CallbackInfoReturnable<ProcessingResult> callback) {
        jem$film = false;
        jem$fillingActor = null; jem$expected = FluidStack.EMPTY; jem$result = ItemStack.EMPTY; jem$outputs = List.of();
    }
    @Redirect(method = "whenItemHeld", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;fillItem(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/item/ItemStack;Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$nativeFilledItem(Level world, int required, ItemStack input, FluidStack fluid,
                                          TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        CompoundTag proof = jem$fluidProof();
        UUID supplier = ((CreateTransportProof)(Object)transported).jem$supplier();
        ItemStack before = input.copy();
        FluidStack supplied = fluid.copy();
        FluidStack expected = fluid.copy();
        expected.shrink(required);
        boolean eligible = world instanceof ServerLevel && required > 0 && supplier != null && proof.hasUUID("actor")
                && supplier.equals(proof.getUUID("actor")) && required <= fluid.getAmount();
        ItemStack result = FillingBySpout.fillItem(world, required, input, fluid);
        if (eligible && !result.isEmpty() && !ItemStack.isSameItemSameTags(before, result)) {
            jem$fillingActor = supplier; jem$expected = expected; jem$result = result.copy();
            jem$film = CreateFilmProofs.completed(before, result, supplied, required);
        }
        return result;
    }
    @Redirect(method = "whenItemHeld", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;handleProcessingOnItem(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;)V"), remap = false)
    private void jem$nativeOutput(TransportedItemStackHandlerBehaviour handler, TransportedItemStack input, TransportedResult result) {
        jem$outputs = List.copyOf(result.getOutputs());
        handler.handleProcessingOnItem(input, result);
    }
    @Inject(method = "whenItemHeld", at = @At("RETURN"), remap = false)
    private void jem$tankAndOutputCommitted(TransportedItemStack input, TransportedItemStackHandlerBehaviour handler, CallbackInfoReturnable<ProcessingResult> callback) {
        if (jem$fillingActor == null || !( ((BlockEntity)(Object)this).getLevel() instanceof ServerLevel level)
                || !CreateSpoutProofs.snapshot(jem$expected).equals(CreateSpoutProofs.snapshot(jem$fluid()))) return;
        jem$bind(jem$fillingActor);
        for (TransportedItemStack output : jem$outputs) {
            if (!output.stack.save(new CompoundTag()).equals(jem$result.save(new CompoundTag()))) continue;
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", jem$fillingActor);
            proof.put("stack", output.stack.save(new CompoundTag()));
            proof.putBoolean("spout", true);
            proof.putBoolean("film", jem$film);
            ((CreateTransportProof)(Object)output).jem$transportProof(proof);
        }
        // Depot installs during the native callback. Belt queues install later through the existing native insert hook.
        handler.handleProcessingOnAllItems(actual -> {
            if (jem$outputs.contains(actual)) CreateTransportInputs.committed(level, actual);
            return null;
        });
    }
}
