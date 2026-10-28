package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareLineage;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareMouse.Capture;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.imasillylittleguy.cnc.procedures.MouseRightClickedOnEntityProcedure", remap = false)
public abstract class CreatureCareMouseCaptureMixin {
    @Unique private static final ThreadLocal<Capture> jem$capture = new ThreadLocal<>();
    @Inject(method = "execute", at = @At("HEAD"))
    private static void jem$before(Entity mouse, Entity player, ItemStack input, CallbackInfo callback) {
        jem$capture.remove();
        if (player instanceof ServerPlayer actor && CreatureCareProof.real(actor) != null
                && CreatureCareProof.type(mouse).equals("cnc:mouse") && mouse.level() == actor.level() && input.is(Items.FLOWER_POT)) {
            jem$capture.set(new Capture(actor, mouse, input));
        }
    }
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41777_()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jem$nativePot(ItemStack source) {
        ItemStack output = source.copy(); Capture capture = jem$capture.get();
        if (capture != null && CreatureCareProof.item(output).equals("cnc:potofmouse") && output.getCount() == 1) {
            UUID batch = NativeFoodSources.get(capture.actor.serverLevel()).provisional(capture.actor.getUUID(), "creature_mouse_pot", output, 1);
            if (batch != null) { capture.batches.add(batch); CreatureCareLineage.get(capture.actor.serverLevel()).mouse(batch, capture.state); }
        }
        return output;
    }
    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$after(Entity mouse, Entity player, ItemStack input, CallbackInfo callback) {
        Capture capture = jem$capture.get(); jem$capture.remove(); if (capture == null) return;
        boolean committed = mouse.isRemoved() && CreatureCareProof.debited(capture.before, input) && capture.batches.size() == 1;
        for (UUID batch : capture.batches) {
            NativeFoodSources.get(capture.actor.serverLevel()).activate(batch, committed);
            if (!committed) CreatureCareLineage.get(capture.actor.serverLevel()).usedMouse(batch);
        }
    }
}
