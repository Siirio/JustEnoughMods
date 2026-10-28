package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldEagleContributors;
import com.justenoughmods.achievementguide.integration.worldmisc.WorldNativeQuestProof;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class WorldEagleCombatQuestMixin implements WorldEagleContributors {
    @Shadow protected boolean dead;
    @Unique private final Set<UUID> jem$eagleActors = new HashSet<>();
    @Unique private Set<UUID> jem$eagleDeathActors = Set.of();
    @Unique private float jem$eagleBeforeHealth;
    @Unique private boolean jem$eagleCaptainDeath;
    @Override public Set<UUID> jem$eagleContributors() { return jem$eagleActors; }
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void jem$damageBegin(DamageSource source, float amount, CallbackInfo ci) {
        jem$eagleBeforeHealth = ((LivingEntity)(Object)this).getHealth();
    }
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void jem$damageCommitted(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity target = (LivingEntity)(Object)this;
        if (!(target instanceof Raider raider) || !raider.isPatrolLeader() || !WorldNativeQuestProof.nativeMob(target)
                || !Float.isFinite(amount) || amount <= 0 || !Float.isFinite(jem$eagleBeforeHealth)
                || !Float.isFinite(target.getHealth()) || target.getHealth() >= jem$eagleBeforeHealth
                || source.is(DamageTypes.GENERIC_KILL) || !(source.getEntity() instanceof TamableAnimal eagle)
                || !WorldNativeQuestProof.entity(eagle, "primal:eagle") || !WorldNativeQuestProof.nativeMob(eagle)
                || !eagle.isTame() || !(target.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = WorldNativeQuestProof.actor(level, eagle.getOwnerUUID());
        if (owner != null && !target.isAlliedTo(owner) && jem$eagleActors.size() < 64) jem$eagleActors.add(owner.getUUID());
    }
    @Inject(method = "die", at = @At("HEAD"))
    private void jem$deathBegin(DamageSource source, CallbackInfo ci) {
        LivingEntity target = (LivingEntity)(Object)this;
        jem$eagleCaptainDeath = !dead && target instanceof Raider raider && raider.isPatrolLeader()
                && WorldNativeQuestProof.nativeMob(target) && !source.is(DamageTypes.GENERIC_KILL);
        jem$eagleDeathActors = jem$eagleCaptainDeath ? Set.copyOf(jem$eagleActors) : Set.of();
    }
    @Inject(method = "die", at = @At("RETURN"))
    private void jem$nativeDeathCommitted(DamageSource source, CallbackInfo ci) {
        LivingEntity target = (LivingEntity)(Object)this;
        Set<UUID> participants = jem$eagleDeathActors;
        boolean committed = jem$eagleCaptainDeath && dead && target.getHealth() <= 0;
        jem$eagleDeathActors = Set.of(); jem$eagleCaptainDeath = false;
        if (!committed || !(target.level() instanceof ServerLevel level)) return;
        jem$eagleActors.clear();
        for (UUID id : participants) WorldNativeQuestProof.emit(WorldNativeQuestProof.actor(level, id), "v3_fauna_eagle_captain");
    }
}
