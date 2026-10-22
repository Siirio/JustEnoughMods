package com.siirio.jemworldbosstiers.encounter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Legacy preparation cleanup; all Raid and party strength is frozen at Start. */
public final class HostedRaidScaling {
    private static final String ROOT = "JEMPreparedRaidScaling";

    private HostedRaidScaling() {}

    public static boolean prepared(LivingEntity boss) {
        return boss.getPersistentData().contains(ROOT);
    }

    public static HostedScaling startScaling(LivingEntity boss, String profile, int participants) {
        restore(boss);
        return HostedConfig.scale(profile, participants, true);
    }

    public static void restore(LivingEntity boss) {
        if (!prepared(boss)) return;
        apply(boss, Attributes.MAX_HEALTH, 1);
        apply(boss, Attributes.ATTACK_DAMAGE, 1);
        boss.getPersistentData().remove(ROOT);
        boss.setHealth(Math.min(boss.getHealth(), boss.getMaxHealth()));
    }

    private static void apply(LivingEntity boss, Attribute attribute, double factor) {
        var instance = boss.getAttribute(attribute);
        if (instance == null) return;
        UUID id = UUID.nameUUIDFromBytes((ROOT + attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(id);
        if (factor != 1) instance.addPermanentModifier(new AttributeModifier(id, ROOT,
                factor - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
