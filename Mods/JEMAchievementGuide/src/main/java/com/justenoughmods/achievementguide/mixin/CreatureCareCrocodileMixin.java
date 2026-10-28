package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareOutputs;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.CrocodileEntity", remap = false)
public abstract class CreatureCareCrocodileMixin {
    @Shadow protected SimpleContainer inventory;
    @Shadow public abstract boolean hasClock();
    @Unique private List<ItemStack> jem$stomach = List.of();
    @Unique private ItemStack jem$input = ItemStack.EMPTY;
    @Unique private CreatureCareOutputs.Run jem$run;
    @Unique private boolean jem$clock;
    @Unique private List<ItemStack> jem$snapshot() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) result.add(inventory.getItem(i).copy());
        return result;
    }
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$run = CreatureCareOutputs.begin((Entity)(Object)this, CreatureCareProof.real(player));
        jem$stomach = jem$snapshot(); jem$input = player.getItemInHand(hand).copy(); jem$clock = hasClock();
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$result(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$run; CreatureCareOutputs.end(run); jem$run = null;
        if (run == null || !callback.getReturnValue().consumesAction()) return;
        if (jem$input.is(net.minecraftforge.common.Tags.Items.FEATHERS) && inventory.isEmpty()
                && jem$stomach.stream().anyMatch(stack -> !stack.isEmpty()) && !run.drops.isEmpty()
                && CreatureCareOutputs.conserved(jem$stomach, jem$snapshot(), run.drops)) {
            for (var drop : run.drops) CreatureCareOutputs.collected(run, drop, "v3_fauna_crocodile_retrieval");
        }
        if (jem$input.is(Items.CLOCK) && !jem$clock && hasClock() && player.getItemInHand(hand).isEmpty()
                && run.drops.isEmpty()) {
            List<ItemStack> before = new ArrayList<>(jem$stomach); before.add(jem$input);
            if (CreatureCareOutputs.conserved(before, jem$snapshot(), List.of())) {
                CreatureCareProof.emit(run.actor, "v3_fauna_crocodile_retrieval_clock");
            }
        }
        jem$stomach = List.of(); jem$input = ItemStack.EMPTY;
    }
}
