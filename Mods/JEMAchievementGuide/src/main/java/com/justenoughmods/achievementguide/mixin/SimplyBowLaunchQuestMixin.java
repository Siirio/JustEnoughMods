package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.item.unique.SimplyBowItem", remap = false)
public abstract class SimplyBowLaunchQuestMixin {
    @Redirect(method = {"shootAll", "shootFan"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$acceptedUniqueBowLaunch(ServerLevel level, Entity result) {
        boolean accepted = level.addFreshEntity(result);
        if (accepted && result instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer actor
                && !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof net.minecraftforge.common.util.FakePlayer)) {
            String bow = BuiltInRegistries.ITEM.getKey((Item) (Object) this).toString();
            result.getPersistentData().putUUID("JemQuestNativeBowActorV4", actor.getUUID());
            result.getPersistentData().putString("JemQuestNativeBowSubjectV4", bow);
            if (bow.equals("simplybows:ice_bow/ice_bow") && result instanceof HomingArrowQuestAccess homing
                    && homing.jem$nativeHomingEnabled() && result.getDeltaMovement().lengthSqr() > 0) {
                QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_bow_ice");
            }
            if (bow.equals("simplybows:bubble_bow/bubble_bow") && actor.isUnderWater()) {
                result.getPersistentData().putBoolean("JemQuestBubbleUnderwaterV4", true);
            }
        }
        return accepted;
    }
}
