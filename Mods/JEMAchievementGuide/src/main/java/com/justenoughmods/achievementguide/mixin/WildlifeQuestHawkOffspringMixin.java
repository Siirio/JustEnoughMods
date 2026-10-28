package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.effect.EffectDebilitatingSting", remap = false)
public abstract class WildlifeQuestHawkOffspringMixin {
    @Unique private final ThreadLocal<WildlifeQuestRun> jem$birth = new ThreadLocal<>();
    @Inject(method = "m_6742_", at = @At("HEAD"))
    private void jem$before(LivingEntity spider, int amplifier, CallbackInfo callback) {
        var actor = WildlifeQuestProof.actor(spider, "hawk_parasite_actor"); jem$birth.set(actor != null && amplifier > 0 ? WildlifeQuestRun.begin(spider, actor, ItemStack.EMPTY) : null);
    }
    @Inject(method = "m_6742_", at = @At("RETURN"))
    private void jem$after(LivingEntity spider, int amplifier, CallbackInfo callback) {
        var run = jem$birth.get(); jem$birth.remove(); WildlifeQuestRun.end(run); if (run == null) return;
        for (var entity : run.created) if (entity instanceof EntityTarantulaHawk baby && baby.isAlive() && baby.isBaby() && run.actor.serverLevel().getEntity(baby.getUUID()) == baby) {
            WildlifeQuestProof.mark(baby, "native_hawk_breeder", run.actor.getUUID());
            WildlifeQuestProof.data(baby).putUUID("native_hawk_host", spider.getUUID()); WildlifeQuestProof.data(baby).putUUID("native_hawk_mother", WildlifeQuestProof.data(spider).getUUID("hawk_mother"));
            WildlifeQuestProof.data(run.actor).putUUID("native_hawk_offspring", baby.getUUID()); WildlifeQuestProof.emit(run.actor, "v3_fauna_insect_nursery_lifecycle");
        }
    }
}
