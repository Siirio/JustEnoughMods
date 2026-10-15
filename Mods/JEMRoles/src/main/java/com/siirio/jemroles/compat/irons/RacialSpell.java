package com.siirio.jemroles.compat.irons;

import com.siirio.jemroles.JemRoles;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class RacialSpell extends AbstractSpell {
    private final ResourceLocation id;
    private final DefaultConfig config;
    public RacialSpell(String path, ResourceLocation school, double cooldownSeconds) {
        this.id = new ResourceLocation(JemRoles.MOD_ID, path);
        this.config = new DefaultConfig().setMinRarity(SpellRarity.COMMON).setSchoolResource(school)
                .setMaxLevel(3).setCooldownSeconds(cooldownSeconds).setAllowCrafting(false).build();
        this.baseManaCost = 0; this.manaCostPerLevel = 0; this.castTime = 0;
    }
    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData data) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) com.siirio.jemroles.trait.RaceAbilities.castSignature(player);
        super.onCast(level, spellLevel, entity, source, data);
    }
}
