package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestMotion;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.event.ServerEvents", remap = false)
public abstract class WildlifeQuestNativeGearMovementMixin {
    @Shadow @Final private static AttributeModifier SAND_SPEED_BONUS;
    @Shadow private BlockPos getDownPos(BlockPos pos, LevelAccessor world) { throw new AssertionError(); }
    @Inject(method = "onLivingUpdateEvent", at = @At("RETURN"))
    private void jem$nativeMovement(LivingTickEvent event, CallbackInfo callback) {
        if (!(event.getEntity() instanceof ServerPlayer actor) || WildlifeQuestProof.real(actor) == null) return;
        var data = WildlifeQuestProof.data(actor); long time = actor.level().getGameTime();
        boolean climb = WildlifeQuestProof.item(actor.getItemBySlot(EquipmentSlot.LEGS)).equals("alexsmobs:centipede_leggings")
                && actor.horizontalCollision && !actor.isInWater() && actor.getDeltaMovement().y == 0.1;
        WildlifeQuestMotion.climb(actor, climb);
        boolean sand = WildlifeQuestProof.item(actor.getItemBySlot(EquipmentSlot.FEET)).equals("alexsmobs:roadrunner_boots")
                && actor.isSprinting() && actor.onGround() && actor.getAttribute(Attributes.MOVEMENT_SPEED) != null
                && actor.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(SAND_SPEED_BONUS)
                && actor.level().getBlockState(getDownPos(actor.blockPosition(), actor.level())).is(BlockTags.SAND)
                && data.getLong("native_walk_time") >= time - 1;
        int ticks = sand && data.getLong("sand_tick") == time - 1 ? data.getInt("sand_ticks") + 1 : sand ? 1 : 0;
        data.putInt("sand_ticks", Math.min(40, ticks)); data.putLong("sand_tick", time);
        if (ticks >= 40) WildlifeQuestProof.emit(actor, "v3_fauna_exotic_travel");
    }
}
