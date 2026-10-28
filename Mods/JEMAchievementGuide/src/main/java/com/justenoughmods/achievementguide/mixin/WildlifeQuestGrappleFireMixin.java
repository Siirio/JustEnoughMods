package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRun;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestTools;
import com.github.alexthe666.alexsmobs.entity.EntitySquidGrapple;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.item.ItemSquidGrapple", remap = false)
public abstract class WildlifeQuestGrappleFireMixin {
    @Unique private final ThreadLocal<WildlifeQuestRun> jem$fire = new ThreadLocal<>();
    @Inject(method = "m_5551_", at = @At("HEAD"))
    private void jem$before(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo callback) {
        jem$fire.remove();
        if (entity instanceof ServerPlayer actor && WildlifeQuestProof.real(actor) != null
                && actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(stack, "wildlife_grapple"))) jem$fire.set(WildlifeQuestRun.begin(actor, actor, stack));
    }
    @Inject(method = "m_5551_", at = @At("RETURN"))
    private void jem$fired(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo callback) {
        var run = jem$fire.get(); jem$fire.remove(); WildlifeQuestRun.end(run); if (run == null) return;
        var tools = WildlifeQuestTools.get(run.actor.serverLevel()); var origin = tools.origin(stack.isEmpty() ? run.input : stack); if (origin == null) return;
        for (var created : run.created) if (created instanceof EntitySquidGrapple hook && hook.getOwner() == run.actor && run.actor.serverLevel().getEntity(hook.getUUID()) == hook) {
            WildlifeQuestProof.mark(hook, "grapple_native_actor", run.actor.getUUID()); WildlifeQuestProof.data(hook).putUUID("grapple_tool", origin);
        }
    }
}
