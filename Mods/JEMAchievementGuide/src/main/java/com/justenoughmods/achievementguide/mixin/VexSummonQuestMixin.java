package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.entity.mobs.SummonedVex;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.spells.evocation.SummonVexSpell", remap = false)
public abstract class VexSummonQuestMixin {
    @Unique private static final ThreadLocal<Set<Entity>> jem$acceptedSpawns = ThreadLocal.withInitial(
            () -> Collections.newSetFromMap(new IdentityHashMap<>()));

    @Inject(method = "onCast", at = @At("HEAD"))
    private void jem$beginSummon(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magic, CallbackInfo callback) {
        jem$acceptedSpawns.get().clear();
    }
    @Redirect(method = "onCast", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$nativeSpawn(Level world, Entity summon) {
        boolean accepted = world.addFreshEntity(summon);
        if (accepted && world instanceof ServerLevel level && summon instanceof SummonedVex
                && level.getEntity(summon.getUUID()) == summon && !summon.isRemoved()) jem$acceptedSpawns.get().add(summon);
        return accepted;
    }
    @Redirect(method = "onCast", at = @At(value = "INVOKE", target =
            "Lio/redspace/ironsspellbooks/capabilities/magic/SummonManager;initSummon(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;ILio/redspace/ironsspellbooks/capabilities/magic/SummonedEntitiesCastData;)V"))
    private void jem$nativeOwnedLifetime(Entity owner, Entity summon, int duration, SummonedEntitiesCastData data,
                                         Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magic) {
        SummonManager.initSummon(owner, summon, duration, data);
        if (owner == caster && caster instanceof ServerPlayer actor && level == actor.level() && duration > 0
                && jem$acceptedSpawns.get().remove(summon) && summon instanceof SummonedVex && !summon.isRemoved()
                && actor.serverLevel().getEntity(summon.getUUID()) == summon && MagicData.getPlayerMagicData(actor) == magic
                && SummonManager.getOwner(summon) == actor && SummonManager.getSummons(actor).contains(summon.getUUID())
                && data.getSummons().contains(summon.getUUID())
                && ((SummonExpiryQuestAccess) SummonManager.INSTANCE).jem$nativeExpirationTick(summon.getUUID()) > actor.server.getTickCount()) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_magic_summon_root");
        }
    }
    @Inject(method = "onCast", at = @At("RETURN"))
    private void jem$endSummon(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magic, CallbackInfo callback) {
        jem$acceptedSpawns.remove();
    }
}
