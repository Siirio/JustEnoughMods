package com.siirio.jemvisualsandbox.compat.irons;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Native instant-cast shell around the authoritative standalone ability. */
public final class SandboxRacialSpell extends AbstractSpell {
    private final ResourceLocation id;
    private final int race;
    private final DefaultConfig config;

    public SandboxRacialSpell(String path, int race, ResourceLocation school) {
        id = new ResourceLocation("jem_visual_sandbox", path);
        this.race = race;
        config = new DefaultConfig().setMinRarity(SpellRarity.COMMON).setSchoolResource(school)
                .setMaxLevel(3).setCooldownSeconds((race==0||race==2?500:race==3?400:1200) / 20.0)
                .setAllowCrafting(false).build();
        baseManaCost = 0;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public int getSpellCooldown() { return RaceExperience.signatureCooldown(race); }

    private boolean ownsSpell(ServerPlayer player) {
        return SandboxIronsBridge.owns(player) && RaceExperience.selected(player)
                && RaceExperience.race(player) == race;
    }

    @Override public CastResult canBeCastedBy(int level, CastSource source, MagicData data, Player entity) {
        if (!(entity instanceof ServerPlayer player)) return super.canBeCastedBy(level, source, data, entity);
        if (!ownsSpell(player)) return new CastResult(CastResult.Type.FAILURE);
        SandboxIronsBridge.sync(player);
        long remaining = RaceExperience.remaining(player, "signature");
        if (remaining > 0) return new CastResult(CastResult.Type.FAILURE,
                Component.translatable("jem.sandbox.cooldown", (remaining + 19) / 20));
        return super.canBeCastedBy(level, source, data, entity);
    }

    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData data) {
        return entity instanceof ServerPlayer player && ownsSpell(player) && player.isAlive() && !player.isSpectator();
    }

    @Override public void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean triggerCooldown) {
        // The native success lifecycle is entered only after the ability commits.
        // Passing false leaves cooldown authority with RaceExperience.
        if (!ownsSpell(player) || !RaceExperience.cast(player)) return;
        super.castSpell(level, spellLevel, player, source, false);
        SandboxIronsBridge.sync(player);
    }
}
