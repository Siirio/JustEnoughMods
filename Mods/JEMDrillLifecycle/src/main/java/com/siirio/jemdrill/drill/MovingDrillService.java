package com.siirio.jemdrill.drill;

import com.siirio.jemdrill.api.DrillAction;
import com.siirio.jemdrill.api.DrillLifecycleEvent;
import net.minecraftforge.common.MinecraftForge;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

public final class MovingDrillService {
    private MovingDrillService() {
    }

    public static boolean service(AbstractContraptionEntity entity, Player player, BlockPos localPos, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || !entity.getDeltaMovement().equals(Vec3.ZERO)) {
            return false;
        }
        ItemStack ingots = player.getItemInHand(hand);
        if (!ingots.is(Items.IRON_INGOT)) {
            return false;
        }
        MutablePair<StructureBlockInfo, MovementContext> selected = entity.getContraption().getActorAt(localPos);
        if (selected == null || !DrillWear.isDrill(selected.getRight())) {
            return false;
        }
        int repaired = 0;
        boolean finalRepair = false;
        for (MutablePair<StructureBlockInfo, MovementContext> actor : entity.getContraption().getActors()) {
            MovementContext context = actor.getRight();
            int wear = MovingDrillState.data(context).getInt(DrillWearPolicy.WEAR_KEY);
            int repairs = MovingDrillState.data(context).getInt(DrillWearPolicy.REPAIRS_KEY);
            if (!DrillWear.isDrill(context) || wear <= 0 || repairs >= DrillWearPolicy.MAX_REPAIRS) {
                continue;
            }
            if (!player.getAbilities().instabuild && repaired >= ingots.getCount()) {
                break;
            }
            MovingDrillState.data(context).putInt(DrillWearPolicy.WEAR_KEY, 0);
            MovingDrillState.data(context).putInt(DrillWearPolicy.REPAIRS_KEY, repairs + 1);
            repaired++;
            finalRepair |= repairs + 1 == DrillWearPolicy.MAX_REPAIRS;
        }
        if (repaired == 0) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            ingots.shrink(repaired);
        }
        MinecraftForge.EVENT_BUS.post(new DrillLifecycleEvent(serverPlayer, DrillAction.SERVICED, repaired, finalRepair, true));
        return true;
    }
}
