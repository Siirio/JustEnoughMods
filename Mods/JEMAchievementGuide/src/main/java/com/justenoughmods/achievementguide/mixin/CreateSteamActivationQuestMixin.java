package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateSteamPlacement;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native setting commit only arms proof. Changed transmitted rotation completes it. */
@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour", remap = false)
public abstract class CreateSteamActivationQuestMixin {
    @Shadow public int value;
    @Unique private int jem$previousValue;

    @Inject(method = "setValueSettings", at = @At("HEAD"), remap = false)
    private void jem$beforeSetting(Player player, ValueSettingsBehaviour.ValueSettings settings, boolean control,
                                   CallbackInfo callback) {
        jem$previousValue = value;
    }

    @Inject(method = "setValueSettings", at = @At("RETURN"), remap = false)
    private void jem$settingCommitted(Player player, ValueSettingsBehaviour.ValueSettings settings, boolean control,
                                      CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)((BlockEntityBehaviour)(Object)this).blockEntity;
        if (value != jem$previousValue && player instanceof ServerPlayer actor && owner instanceof SteamEngineBlockEntity) {
            CreateSteamPlacement.activated(owner, actor);
        }
    }
}
