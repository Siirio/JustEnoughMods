package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareOutputs;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.imasillylittleguy.cnc.entity.SquonkEntity", remap = false)
public abstract class CreatureCareSquonkMixin {
    @Unique private CreatureCareOutputs.Run jem$run;
    @Unique private int jem$cookies;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$run = player.getItemInHand(hand).is(Items.COOKIE)
                ? CreatureCareOutputs.begin((Entity)(Object)this, CreatureCareProof.real(player)) : null;
        jem$cookies = player.getInventory().countItem(Items.COOKIE);
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$present(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$run; jem$run = null; CreatureCareOutputs.end(run);
        if (run == null || jem$cookies - player.getInventory().countItem(Items.COOKIE) != 1) return;
        for (var drop : run.drops) {
            String id = CreatureCareProof.item(drop.getItem());
            if (id.equals("cnc:music_disc_hills") || id.equals("cnc:music_disc_wreck_of_the_old_97")
                    || id.equals("cnc:music_disc_slough_choir")) {
                CreatureCareOutputs.collected(run, drop, "v3_fauna_pot_and_present_present");
            }
        }
    }
}
