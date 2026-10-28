package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureActors;
import com.justenoughmods.achievementguide.integration.nature.NatureDropProof;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityTumbleweed.class, remap = false)
public abstract class NatureTumbleweedQuestMixin {
    @Shadow protected abstract void dropFromLootTable(DamageSource source);

    @Redirect(method = "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE",
            target = "Lnet/konwboy/tumbleweed/common/EntityTumbleweed;dropFromLootTable(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void jem$nativeWeedLoot(EntityTumbleweed weed, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer actor) || !NatureActors.eligible(actor) || actor.level() != weed.level()) {
            dropFromLootTable(source);
            return;
        }
        var run = new NatureDropProof.Run(actor, weed.blockPosition(), "");
        NatureDropProof.nativeCall(run, () -> { dropFromLootTable(source); return null; });
        if (run.accepted && weed.isRemoved()) NatureActors.emit(actor, "v3_world_desert_wind");
    }
}
