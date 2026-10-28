package com.justenoughmods.achievementguide.integration.wildlife;
import com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus;
import java.util.ArrayDeque;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
public final class WildlifeQuestAbilities {
    private static final ThreadLocal<ArrayDeque<EntityMimicOctopus>> EXPLOSIONS = ThreadLocal.withInitial(ArrayDeque::new);
    private WildlifeQuestAbilities() {}
    public static void begin(EntityMimicOctopus mimic) { EXPLOSIONS.get().push(mimic); }
    public static void end(EntityMimicOctopus mimic) { var q = EXPLOSIONS.get(); if (q.peek() == mimic) q.pop(); if (q.isEmpty()) EXPLOSIONS.remove(); }
    public static void copied(EntityMimicOctopus mimic, LivingEntity target, float health, boolean accepted) {
        var actor = WildlifeQuestProof.owner(mimic);
        if (actor != null && accepted && target.getHealth() < health && mimic.isUpgraded() && WildlifeQuestProof.matches(mimic, "mimic_trainer", actor.getUUID()) && WildlifeQuestProof.hostile(target, actor)) WildlifeQuestProof.emit(actor, "v3_fauna_companion_powers");
    }
    public static void exploded(LivingEntity target, DamageSource source, float health, boolean accepted) { var mimic = EXPLOSIONS.get().peek(); if (mimic != null && source.getEntity() == mimic) copied(mimic, target, health, accepted); }
}
