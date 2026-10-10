package com.siirio.jemdrill.drill;

import com.siirio.jemdrill.api.DrillAction;
import com.siirio.jemdrill.api.DrillLifecycleEvent;
import net.minecraftforge.common.MinecraftForge;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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

import java.util.LinkedHashSet;
import java.util.Set;

public final class StationaryDrillService {
    private StationaryDrillService() {
    }

    public static InteractionResult service(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DrillBlockEntity selected) || !stack.is(Items.IRON_INGOT) || selected.getSpeed() != 0.0F) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        Set<KineticBlockEntity> members = new LinkedHashSet<>();
        members.add(selected);
        if (selected.hasNetwork()) {
            members.addAll(selected.getOrCreateNetwork().members.keySet());
        }
        int available = player.getAbilities().instabuild ? Integer.MAX_VALUE : stack.getCount();
        int serviced = 0;
        boolean finalService = false;
        for (KineticBlockEntity member : members) {
            if (serviced >= available) {
                break;
            }
            if (!(member instanceof DrillWearAccess wear) || member.getSpeed() != 0.0F || wear.jemcompat$getWear() <= 0 || wear.jemcompat$getRepairs() >= DrillWearPolicy.MAX_REPAIRS) {
                continue;
            }
            wear.jemcompat$setWear(0);
            wear.jemcompat$setRepairs(wear.jemcompat$getRepairs() + 1);
            finalService |= wear.jemcompat$getRepairs() == DrillWearPolicy.MAX_REPAIRS;
            member.setChanged();
            serviced++;
        }
        if (serviced == 0) {
            return InteractionResult.PASS;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(serviced);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MinecraftForge.EVENT_BUS.post(new DrillLifecycleEvent(serverPlayer, DrillAction.SERVICED, serviced, finalService, false));
        }
        return InteractionResult.CONSUME;
    }
}
