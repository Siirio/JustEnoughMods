package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.CanonicalBossDeathProof;
import com.justenoughmods.achievementguide.integration.DeadKingQuestRecipients;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.siirio.jemworldbosstiers.JemWorldBossTiers", remap = false)
public abstract class CanonicalBossVictoryQuestMixin {
    @Unique private static final ResourceLocation jem$deadKing = new ResourceLocation("irons_spellbooks", "dead_king");

    @Inject(method = "recordBossDeath", at = @At(value = "INVOKE", target =
            "Lcom/siirio/jemworldbosstiers/JemWorldBossTiers;recordVictory(Lnet/minecraft/server/MinecraftServer;Lcom/siirio/jemworldbosstiers/balance/BossProfile;)V",
            shift = At.Shift.AFTER), require = 2)
    private void jem$approvedNativeBossDeath(LivingDeathEvent event, CallbackInfo callback) {
        var boss = event.getEntity();
        if (!jem$deadKing.equals(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()))
                || event.isCanceled() || event.getSource().is(DamageTypes.GENERIC_KILL)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        var encounter = WorldTierApi.encounter(boss).orElse(null);
        if (encounter != null && encounter.participants().contains(killer.getUUID())
                && boss instanceof CanonicalBossDeathProof proof) proof.jem$approvedNativeVictory(event, killer);
    }

    @Inject(method = "settledHostedEncounter", at = @At("RETURN"))
    private void jem$settledHostedVictory(HostedEncounterClosedEvent event, CallbackInfo callback) {
        if (!event.success() || !WorldTierData.get(event.server()).canonicalHostedClosed(event.attemptId())) return;
        var frozen = event.frozenState();
        if (!jem$deadKing.toString().equals(frozen.getString("EntityType")) || frozen.getBoolean("DeathByGenericKill")) return;
        DeadKingQuestRecipients.accepted(event.server(), event.eligibleParticipants());
    }
}
