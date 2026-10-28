package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TrainAccessClimbQuestMixin {
    @Unique private static final Set<String> JEM_ACCESS_CLIMBS = Set.of("bellsandwhistles:andesite_grab_rails", "bellsandwhistles:brass_grab_rails", "bellsandwhistles:copper_grab_rails",
            "bellsandwhistles:andesite_bogie_steps", "bellsandwhistles:brass_bogie_steps", "bellsandwhistles:copper_bogie_steps");
    @Unique private double jem$beforeY;
    @Inject(method = "travel", at = @At("HEAD"))
    private void jem$beforeClimb(Vec3 movement, CallbackInfo callback) {
        if ((Object)this instanceof ServerPlayer actor) jem$beforeY = actor.getY();
    }
    @Inject(method = "travel", at = @At("RETURN"))
    private void jem$afterClimb(Vec3 movement, CallbackInfo callback) {
        if (!((Object)this instanceof ServerPlayer actor) || actor.getY() - jem$beforeY <= .0001
                || actor.getAbilities().flying || actor.isFallFlying() || actor.isPassenger() || !actor.onClimbable()) return;
        var climb = actor.getLastClimbablePos();
        if (climb.isPresent() && actor.serverLevel().isLoaded(climb.get()) && JEM_ACCESS_CLIMBS.contains(String.valueOf(
                ForgeRegistries.BLOCKS.getKey(actor.serverLevel().getBlockState(climb.get()).getBlock()))))
            QuestActionTrigger.INSTANCE.emit(actor, "create_train_access_climb");
    }
}
