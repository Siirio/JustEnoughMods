package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateWhistleProofs;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.schedule.Schedule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.railwayteam.railways.content.conductor.whistle.ConductorWhistleItem", remap = false)
public abstract class ConductorWhistleQuestMixin {
    @Unique private final ThreadLocal<Schedule> jem$before = new ThreadLocal<>();
    @Inject(method = {"interactLivingEntity", "m_6880_"}, at = @At("RETURN"), remap = false)
    private void jem$assigned(ItemStack whistle, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (callback.getReturnValue() == InteractionResult.SUCCESS && player instanceof ServerPlayer actor && CreateWhistleProofs.eligible(actor)
                && target instanceof ConductorEntity && target.getPersistentData().hasUUID(CreateWhistleProofs.CREATOR)
                && actor.getUUID().equals(target.getPersistentData().getUUID(CreateWhistleProofs.CREATOR))
                && target.getVehicle() instanceof CarriageContraptionEntity carriage && whistle.hasTag()
                && whistle.getTag().hasUUID("SelectedTrain") && whistle.getTag().hasUUID("SelectedConductor")
                && target.getUUID().equals(whistle.getTag().getUUID("SelectedConductor")) && carriage.getCarriage() != null
                && carriage.getCarriage().train.id.equals(whistle.getTag().getUUID("SelectedTrain")))
            target.getPersistentData().putUUID(CreateWhistleProofs.ASSIGNER, actor.getUUID());
    }
    @Inject(method = {"useOn", "m_6225_"}, at = @At("HEAD"), remap = false)
    private void jem$before(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        jem$before.remove();
        if (!(context.getPlayer() instanceof ServerPlayer)) return;
        var tag = context.getItemInHand().getTag();
        if (tag != null && tag.hasUUID("SelectedTrain")) {
            var train = Create.RAILWAYS.trains.get(tag.getUUID("SelectedTrain")); if (train != null) jem$before.set(train.runtime.schedule);
        }
    }
    @Inject(method = {"useOn", "m_6225_"}, at = @At("RETURN"), remap = false)
    private void jem$requested(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (callback.getReturnValue() == InteractionResult.SUCCESS && context.getPlayer() instanceof ServerPlayer actor)
            CreateWhistleProofs.requested(actor, context.getItemInHand(), jem$before.get());
        jem$before.remove();
    }
}
