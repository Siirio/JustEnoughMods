package com.siirio.jemroles.compat.irons;

import com.siirio.jemroles.JemRoles;
import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceService;
import com.siirio.jemroles.trait.RaceAbilities;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class RacialSpell extends AbstractSpell {
    private final ResourceLocation id;
    private final RaceId race;
    private final DefaultConfig config;
    public RacialSpell(String path, RaceId race, ResourceLocation school, double cooldownSeconds) {
        this.id = new ResourceLocation(JemRoles.MOD_ID, path);
        this.race = race;
        this.config = new DefaultConfig().setMinRarity(SpellRarity.COMMON).setSchoolResource(school)
                .setMaxLevel(3).setCooldownSeconds(cooldownSeconds).setAllowCrafting(false).build();
        this.baseManaCost = 0; this.manaCostPerLevel = 0; this.castTime = 0;
    }
    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public int getSpellCooldown() { return RaceAbilities.signatureCooldown(race); }
    // Innate racial spells do not require the school's research unlock. Ownership stays server-validated.
    @Override public boolean requiresLearning() { return false; }

    private boolean ownsSpell(ServerPlayer player) {
        return player.server.isDedicatedServer()
                && RaceService.record(player).map(record -> record.race() == race).orElse(false);
    }

    @Override public CastResult canBeCastedBy(int spellLevel, CastSource source, MagicData data, Player entity) {
        if (!(entity instanceof ServerPlayer player)) return super.canBeCastedBy(spellLevel, source, data, entity);
        if (!ownsSpell(player))
            return new CastResult(CastResult.Type.FAILURE);
        RacialSpellBridge.syncCooldown(player);
        long remaining = RaceService.record(player).orElseThrow().remaining("signature", player.level().getGameTime());
        if (remaining > 0) return new CastResult(CastResult.Type.FAILURE,
                Component.translatable("jem.roles.cooldown", com.siirio.jemidentity.api.MinecraftDays.format(remaining)));
        return super.canBeCastedBy(spellLevel, source, data, entity);
    }

    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData data) {
        return entity instanceof ServerPlayer player && ownsSpell(player) && RaceAbilities.canCastSignature(player);
    }

    @Override public void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean triggerCooldown) {
        // AbstractSpell.castSpell adds cooldown and sends successful-cast effects even when onCast did nothing.
        // Commit gameplay first; only confirmed success enters that native completion lifecycle.
        if (!ownsSpell(player) || !RaceAbilities.castSignature(player)) return;
        super.castSpell(level, spellLevel, player, source, false);
        RacialSpellBridge.syncCooldown(player);
    }
}
