package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.FluidTransferObservation;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper", remap = false)
public abstract class BackpackFluidQuestMixin {
    @Unique private ServerPlayer jem$actor;
    @Unique private Map<FluidTransferObservation.Kind, Long> jem$external;
    @Unique private Map<FluidTransferObservation.Kind, Long> jem$tank;

    @Inject(method = {"fillContainerInHand", "fillFromHand"}, at = @At("HEAD"))
    private void jem$beforeTransfer(Player player, InteractionHand hand, IFluidHandlerItem external, IFluidHandler tank,
                                    CallbackInfoReturnable<Boolean> callback) {
        jem$actor = null;
        jem$external = jem$tank = null;
        if (player instanceof ServerPlayer actor && external != tank && ((IUpgradeWrapper) this).isEnabled()
                && ((UpgradeWrapperQuestAccess) this).jem$questStorageWrapper() instanceof IBackpackWrapper) {
            jem$external = FluidTransferObservation.snapshot(external);
            jem$tank = FluidTransferObservation.snapshot(tank);
            if (jem$external != null && jem$tank != null) jem$actor = actor;
        }
    }

    @Inject(method = {"fillContainerInHand", "fillFromHand"}, at = @At("RETURN"))
    private void jem$afterTransfer(Player player, InteractionHand hand, IFluidHandlerItem external, IFluidHandler tank,
                                   CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer actor = jem$actor;
        var beforeExternal = jem$external;
        var beforeTank = jem$tank;
        jem$actor = null;
        jem$external = jem$tank = null;
        if (actor == null || actor != player || !callback.getReturnValue()
                || !ItemStack.matches(actor.getItemInHand(hand), external.getContainer())) return;
        if (FluidTransferObservation.conserved(beforeExternal, FluidTransferObservation.snapshot(external),
                beforeTank, FluidTransferObservation.snapshot(tank))) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_store_fluids");
        }
    }
}
