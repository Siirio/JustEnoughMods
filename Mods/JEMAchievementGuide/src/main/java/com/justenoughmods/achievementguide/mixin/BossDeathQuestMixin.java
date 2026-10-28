package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.CanonicalBossDeathProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BossDeathQuestMixin implements CanonicalBossDeathProof {
    @Shadow protected boolean dead;
    @Unique private DamageSource jem$deathSource;
    @Unique private LivingDeathEvent jem$approvedDeath;
    @Unique private ServerPlayer jem$approvedKiller;
    @Unique private boolean jem$alreadyDead;

    @Inject(method = "die", at = @At("HEAD"))
    private void jem$beginNativeDeath(DamageSource source, CallbackInfo callback) {
        jem$deathSource = source;
        jem$approvedDeath = null;
        jem$approvedKiller = null;
        jem$alreadyDead = dead;
    }

    @Override
    public void jem$approvedNativeVictory(LivingDeathEvent event, ServerPlayer killer) {
        if (event.getEntity() == (Object) this && event.getSource() == jem$deathSource && !jem$alreadyDead) {
            jem$approvedDeath = event;
            jem$approvedKiller = killer;
        }
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void jem$completedNativeDeath(DamageSource source, CallbackInfo callback) {
        var approved = jem$approvedDeath;
        var killer = jem$approvedKiller;
        boolean committed = source == jem$deathSource && !jem$alreadyDead && dead
                && approved != null && !approved.isCanceled() && approved.getSource() == source
                && killer != null && source.getEntity() == killer;
        jem$deathSource = null;
        jem$approvedDeath = null;
        jem$approvedKiller = null;
        if (committed) QuestActionTrigger.INSTANCE.emit(killer, "legitimate_dead_king_victory");
    }
}
