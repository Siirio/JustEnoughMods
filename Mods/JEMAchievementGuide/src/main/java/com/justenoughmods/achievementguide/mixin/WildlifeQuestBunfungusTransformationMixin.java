package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRun;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.event.ServerEvents", remap = false)
public abstract class WildlifeQuestBunfungusTransformationMixin {
    @Unique private final ThreadLocal<WildlifeQuestRun> jem$run = new ThreadLocal<>();
    @Inject(method = "onInteractWithEntity", at = @At("HEAD"))
    private void jem$before(EntityInteract event, CallbackInfo callback) {
        jem$run.remove();
        if (!event.isCanceled() && event.getTarget() instanceof Rabbit && WildlifeQuestProof.item(event.getItemStack()).equals("alexsmobs:mungal_spores")) {
            jem$run.set(WildlifeQuestRun.begin(event.getTarget(), WildlifeQuestProof.real(event.getEntity()), event.getItemStack()));
        }
    }
    @Inject(method = "onInteractWithEntity", at = @At("RETURN"))
    private void jem$after(EntityInteract event, CallbackInfo callback) {
        var run = jem$run.get(); jem$run.remove(); WildlifeQuestRun.end(run);
        if (run != null) run.transformed(event.getItemStack(), "alexsmobs:bunfungus", "v3_fauna_mutable_materials");
    }
}
