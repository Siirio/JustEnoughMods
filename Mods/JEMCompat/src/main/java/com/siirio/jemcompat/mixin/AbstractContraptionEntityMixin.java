package com.siirio.jemcompat.mixin;

import com.siirio.jemcompat.advancement.AdvancementAwards;
import com.siirio.jemcompat.feature.create.DrillWear;
import com.siirio.jemcompat.feature.create.MovingDrillState;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class AbstractContraptionEntityMixin {
    private static final ResourceLocation DRILL_SERVICE = advancement("drill_service");
    private static final ResourceLocation BATCH_SERVICE = advancement("batch_service");
    private static final ResourceLocation LAST_SERVICE = advancement("drill_last_service");

    @Shadow
    public abstract Contraption getContraption();

    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true)
    private void jemcompat$serviceDrills(Player player, BlockPos localPos, Direction side, InteractionHand hand, CallbackInfoReturnable<Boolean> callback) {
        AbstractContraptionEntity entity = (AbstractContraptionEntity) (Object) this;
        if (!(player instanceof ServerPlayer serverPlayer) || !entity.getDeltaMovement().equals(Vec3.ZERO)) {
            return;
        }
        ItemStack ingots = player.getItemInHand(hand);
        if (!ingots.is(Items.IRON_INGOT)) {
            return;
        }
        MutablePair<StructureBlockInfo, MovementContext> selected = getContraption().getActorAt(localPos);
        if (selected == null || !DrillWear.isDrill(selected.getRight())) {
            return;
        }
        int repaired = 0;
        boolean finalRepair = false;
        for (MutablePair<StructureBlockInfo, MovementContext> actor : getContraption().getActors()) {
            MovementContext context = actor.getRight();
            int wear = MovingDrillState.data(context).getInt(DrillWear.WEAR_KEY);
            int repairs = MovingDrillState.data(context).getInt(DrillWear.REPAIRS_KEY);
            if (!DrillWear.isDrill(context) || wear <= 0 || repairs >= DrillWear.MAX_REPAIRS) {
                continue;
            }
            if (!player.getAbilities().instabuild && repaired >= ingots.getCount()) {
                break;
            }
            MovingDrillState.data(context).putInt(DrillWear.WEAR_KEY, 0);
            MovingDrillState.data(context).putInt(DrillWear.REPAIRS_KEY, repairs + 1);
            repaired++;
            finalRepair |= repairs + 1 == DrillWear.MAX_REPAIRS;
        }
        if (repaired == 0) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            ingots.shrink(repaired);
        }
        AdvancementAwards.award(serverPlayer, DRILL_SERVICE);
        if (repaired > 1) {
            AdvancementAwards.award(serverPlayer, BATCH_SERVICE);
        }
        if (finalRepair) {
            AdvancementAwards.award(serverPlayer, LAST_SERVICE);
        }
        callback.setReturnValue(true);
    }

    private static ResourceLocation advancement(String path) {
        return new ResourceLocation("jem", "engineering/" + path);
    }
}
