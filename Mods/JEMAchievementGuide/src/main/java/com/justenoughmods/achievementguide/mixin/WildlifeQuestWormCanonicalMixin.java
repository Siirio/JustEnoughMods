package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.siirio.jemworldbosstiers.JemWorldBossTiers", remap = false)
public abstract class WildlifeQuestWormCanonicalMixin {
    @Inject(method = "recordBossDeath", at = @At(value = "INVOKE", target = "Lcom/siirio/jemworldbosstiers/JemWorldBossTiers;recordVictory(Lnet/minecraft/server/MinecraftServer;Lcom/siirio/jemworldbosstiers/balance/BossProfile;)V", shift = At.Shift.AFTER), require = 2)
    private void jem$approved(LivingDeathEvent event, CallbackInfo callback) {
        var boss = event.getEntity();
        if (!event.isCanceled() && !event.getSource().is(DamageTypes.GENERIC_KILL) && WildlifeQuestProof.type(boss).equals("alexsmobs:void_worm") && boss instanceof WildlifeQuestDeathProof proof)
            WorldTierApi.encounter(boss).ifPresent(encounter -> proof.jem$wildlifeApproved(event, encounter.participants()));
    }
    @Inject(method = "settledHostedEncounter", at = @At("RETURN"))
    private void jem$settled(HostedEncounterClosedEvent event, CallbackInfo callback) {
        if (event.success() && WorldTierData.get(event.server()).canonicalHostedClosed(event.attemptId()) && event.frozenState().getString("EntityType").equals("alexsmobs:void_worm") && !event.frozenState().getBoolean("DeathByGenericKill"))
            WildlifeQuestWorm.get(event.server()).settled(event.bossId() + ":" + event.attemptId(), event.eligibleParticipants());
    }
}
