package com.siirio.jemcompat.mixin;

import com.siirio.jemcompat.feature.create.DrillWearAccess;
import com.siirio.jemcompat.feature.create.StationaryDrillWear;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(value = DrillBlock.class, remap = false)
public abstract class DrillBlockMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$serviceDrills(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack stack = player.getItemInHand(hand);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DrillBlockEntity selected) || !stack.is(Items.IRON_INGOT) || selected.getSpeed() != 0.0F) {
            return;
        }
        if (level.isClientSide) {
            callback.setReturnValue(InteractionResult.sidedSuccess(true));
            return;
        }
        Set<KineticBlockEntity> members = new LinkedHashSet<>();
        members.add(selected);
        if (selected.hasNetwork()) {
            members.addAll(selected.getOrCreateNetwork().members.keySet());
        }
        int available = player.getAbilities().instabuild ? Integer.MAX_VALUE : stack.getCount();
        int serviced = 0;
        for (KineticBlockEntity member : members) {
            if (serviced >= available) {
                break;
            }
            if (!(member instanceof DrillWearAccess wear) || member.getSpeed() != 0.0F || wear.jemcompat$getWear() <= 0 || wear.jemcompat$getRepairs() >= StationaryDrillWear.MAX_REPAIRS) {
                continue;
            }
            wear.jemcompat$setWear(0);
            wear.jemcompat$setRepairs(wear.jemcompat$getRepairs() + 1);
            member.setChanged();
            serviced++;
        }
        if (serviced == 0) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(serviced);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            StationaryDrillWear.awardService(serverPlayer);
        }
        callback.setReturnValue(InteractionResult.CONSUME);
    }
}
